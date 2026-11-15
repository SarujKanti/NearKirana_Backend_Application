package com.nearkirana.backend.service;

import com.nearkirana.backend.config.OtpProperties;
import com.nearkirana.backend.dto.auth.CancelOtpRequest;
import com.nearkirana.backend.dto.auth.CancelOtpResponse;
import com.nearkirana.backend.dto.auth.SendOtpResponse;
import com.nearkirana.backend.dto.auth.UserResponse;
import com.nearkirana.backend.dto.auth.VerifyOtpRequest;
import com.nearkirana.backend.dto.auth.VerifyOtpResponse;
import com.nearkirana.backend.entity.OtpChallenge;
import com.nearkirana.backend.entity.User;
import com.nearkirana.backend.exception.ApiException;
import com.nearkirana.backend.repository.MobileLoginRecordRepository;
import com.nearkirana.backend.repository.OtpChallengeRepository;
import com.nearkirana.backend.repository.UserRepository;
import com.nearkirana.backend.security.OtpGenerator;
import com.nearkirana.backend.security.OtpHasher;
import com.nearkirana.backend.sms.SmsSender;
import com.nearkirana.backend.util.MobileNumbers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Mobile number + OTP login.
 *
 *  send-otp:   validate number -> rate limits -> record number -> generate OTP -> store HASH -> send SMS
 *  verify-otp: load challenge (locked) -> record "OTP entered" -> checks -> compare hash -> find or create user
 *  cancel-otp: load challenge (locked) -> invalidate it -> record the cancel
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final Duration RATE_LIMIT_WINDOW = Duration.ofHours(1);

    private final OtpChallengeRepository otpChallengeRepository;
    private final UserRepository userRepository;
    private final MobileLoginRecordRepository mobileLoginRecordRepository;
    private final OtpGenerator otpGenerator;
    private final OtpHasher otpHasher;
    private final SmsSender smsSender;
    private final OtpProperties otpProperties;
    private final Clock clock;

    public AuthService(OtpChallengeRepository otpChallengeRepository,
                       UserRepository userRepository,
                       MobileLoginRecordRepository mobileLoginRecordRepository,
                       OtpGenerator otpGenerator,
                       OtpHasher otpHasher,
                       SmsSender smsSender,
                       OtpProperties otpProperties,
                       Clock clock) {
        this.otpChallengeRepository = otpChallengeRepository;
        this.userRepository = userRepository;
        this.mobileLoginRecordRepository = mobileLoginRecordRepository;
        this.otpGenerator = otpGenerator;
        this.otpHasher = otpHasher;
        this.smsSender = smsSender;
        this.otpProperties = otpProperties;
        this.clock = clock;
    }

    // ------------------------------------------------------------------
    // POST /api/v1/auth/send-otp
    // ------------------------------------------------------------------
    @Transactional
    public SendOtpResponse sendOtp(String rawMobileNumber, String requestIp) {
        String mobileNumber = normalizeOrThrow(rawMobileNumber);
        Instant now = clock.instant();

        enforceRateLimits(mobileNumber, requestIp, now);

        // First time this number is entered -> new record; otherwise update it.
        mobileLoginRecordRepository.recordOtpRequested(mobileNumber, now);

        // Older unused OTPs for this number stop working.
        otpChallengeRepository.invalidateActiveChallenges(mobileNumber, now);

        UUID challengeId = UUID.randomUUID();
        String otp = otpGenerator.generate(mobileNumber);
        OtpChallenge challenge = new OtpChallenge(
                challengeId,
                mobileNumber,
                otpHasher.hash(challengeId, otp), // only the hash is saved
                now.plus(otpProperties.expiry()),
                requestIp,
                now);
        otpChallengeRepository.save(challenge);

        smsSender.sendOtp(mobileNumber, otp);
        log.info("OTP challenge {} created for {}", challengeId, MobileNumbers.mask(mobileNumber));

        return new SendOtpResponse(
                challengeId,
                otpProperties.expiry().toSeconds(),
                otpProperties.resendCooldown().toSeconds(),
                false,  // enteredOTP: nothing typed yet for this new OTP
                true);  // sendOTP: OTP sent
    }

    private void enforceRateLimits(String mobileNumber, String requestIp, Instant now) {
        // 1) Cooldown: wait before asking for another OTP
        Optional<OtpChallenge> latest = otpChallengeRepository.findFirstByMobileNumberOrderByCreatedAtDesc(mobileNumber);
        if (latest.isPresent()) {
            Instant allowedAt = latest.get().getCreatedAt().plus(otpProperties.resendCooldown());
            if (now.isBefore(allowedAt)) {
                long waitSeconds = Math.max(1, Duration.between(now, allowedAt).toSeconds());
                throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                        "Please wait " + waitSeconds + " seconds before requesting a new OTP.", waitSeconds);
            }
        }

        Instant windowStart = now.minus(RATE_LIMIT_WINDOW);

        // 2) Per mobile number, per hour
        if (otpChallengeRepository.countByMobileNumberAndCreatedAtAfter(mobileNumber, windowStart)
                >= otpProperties.maxSendsPerHour()) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many OTP requests for this number. Please try again later.",
                    RATE_LIMIT_WINDOW.toSeconds());
        }

        // 3) Per device / network (IP address), per hour
        if (otpChallengeRepository.countByRequestIpAndCreatedAtAfter(requestIp, windowStart)
                >= otpProperties.maxSendsPerIpPerHour()) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many OTP requests. Please try again later.",
                    RATE_LIMIT_WINDOW.toSeconds());
        }
    }

    // ------------------------------------------------------------------
    // POST /api/v1/auth/verify-otp
    // ------------------------------------------------------------------

    /**
     * noRollbackFor: when we reject a wrong OTP we throw ApiException, but the
     * increased attempt counter must still be SAVED. Otherwise an attacker
     * could guess forever.
     */
    @Transactional(noRollbackFor = ApiException.class)
    public VerifyOtpResponse verifyOtp(VerifyOtpRequest request) {
        String mobileNumber = normalizeOrThrow(request.mobileNumber());
        Instant now = clock.instant();

        OtpChallenge challenge = otpChallengeRepository.findByIdForUpdate(request.challengeId())
                .filter(c -> c.getMobileNumber().equals(mobileNumber))
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST,
                        "Invalid verification request. Please request a new OTP."));

        // The user DID enter an OTP (right or wrong). Saved even if we reject it below.
        challenge.markOtpEntered();
        mobileLoginRecordRepository.recordOtpEntered(mobileNumber, now);

        if (challenge.isConsumed()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "This OTP has already been used, replaced or cancelled. Please request a new OTP.");
        }
        if (challenge.isExpired(now)) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "OTP has expired. Please request a new OTP.");
        }
        if (challenge.getAttemptCount() >= otpProperties.maxAttempts()) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many incorrect attempts. Please request a new OTP.");
        }

        if (!otpHasher.matches(challenge.getId(), request.otp(), challenge.getOtpHash())) {
            challenge.recordFailedAttempt(); // saved on commit (see noRollbackFor)
            int remaining = otpProperties.maxAttempts() - challenge.getAttemptCount();
            log.info("Wrong OTP for challenge {} ({} attempts left)", challenge.getId(), remaining);
            if (remaining <= 0) {
                throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                        "Too many incorrect attempts. Please request a new OTP.");
            }
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Incorrect OTP. " + remaining + " attempt(s) remaining.");
        }

        // Correct OTP: it can never be used again.
        challenge.consume(now);

        Optional<User> existingUser = userRepository.findByMobileNumber(mobileNumber);
        boolean isNewUser = existingUser.isEmpty();
        User user = existingUser.orElseGet(() -> new User(mobileNumber, request.role()));

        if (!isNewUser) {
            // The role comes from OUR database, never from the client.
            if (user.getRole() != request.role()) {
                throw new ApiException(HttpStatus.CONFLICT,
                        "This mobile number is registered as a " + user.getRole()
                                + ". Please use the " + user.getRole().name().toLowerCase() + " app.");
            }
            if (!user.isActive()) {
                throw new ApiException(HttpStatus.FORBIDDEN,
                        "This account has been disabled. Please contact support.");
            }
        }

        user.markVerified();
        user = userRepository.save(user);
        log.info("Mobile {} verified (userId={}, newUser={})",
                MobileNumbers.mask(mobileNumber), user.getId(), isNewUser);

        return new VerifyOtpResponse(UserResponse.from(user), isNewUser, true);
    }

    // ------------------------------------------------------------------
    // POST /api/v1/auth/cancel-otp
    // ------------------------------------------------------------------
    @Transactional
    public CancelOtpResponse cancelOtp(CancelOtpRequest request) {
        String mobileNumber = normalizeOrThrow(request.mobileNumber());
        Instant now = clock.instant();

        OtpChallenge challenge = otpChallengeRepository.findByIdForUpdate(request.challengeId())
                .filter(c -> c.getMobileNumber().equals(mobileNumber))
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST,
                        "Invalid cancel request."));

        // Cancelling twice, or after a successful login, changes nothing.
        if (!challenge.isConsumed()) {
            challenge.consume(now); // this OTP can no longer be used
            mobileLoginRecordRepository.recordCancelled(mobileNumber, now);
            log.info("OTP challenge {} cancelled for {}", challenge.getId(), MobileNumbers.mask(mobileNumber));
        }

        return new CancelOtpResponse(challenge.isOtpEntered());
    }

    private String normalizeOrThrow(String rawMobileNumber) {
        return MobileNumbers.normalize(rawMobileNumber)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST,
                        "Please enter a valid 10-digit Indian mobile number."));
    }
}
