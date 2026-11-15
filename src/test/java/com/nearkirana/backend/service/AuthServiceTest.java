package com.nearkirana.backend.service;

import com.nearkirana.backend.config.OtpProperties;
import com.nearkirana.backend.dto.auth.AuthTokens;
import com.nearkirana.backend.dto.auth.CancelOtpRequest;
import com.nearkirana.backend.dto.auth.CancelOtpResponse;
import com.nearkirana.backend.dto.auth.SendOtpResponse;
import com.nearkirana.backend.dto.auth.VerifyOtpRequest;
import com.nearkirana.backend.dto.auth.VerifyOtpResponse;
import com.nearkirana.backend.entity.OtpChallenge;
import com.nearkirana.backend.entity.Role;
import com.nearkirana.backend.entity.User;
import com.nearkirana.backend.exception.ApiException;
import com.nearkirana.backend.repository.MobileLoginRecordRepository;
import com.nearkirana.backend.repository.OtpChallengeRepository;
import com.nearkirana.backend.repository.UserRepository;
import com.nearkirana.backend.security.OtpHasher;
import com.nearkirana.backend.security.SecureOtpGenerator;
import com.nearkirana.backend.security.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests the OTP rules with fake repositories and a fixed clock.
 */
class AuthServiceTest {

    private static final String MOBILE = "+919876543210";
    private static final String IP = "127.0.0.1";
    private static final Instant NOW = Instant.parse("2026-11-15T10:00:00Z");

    private final OtpProperties props = new OtpProperties(
            Duration.ofMinutes(5), 5, Duration.ofSeconds(60), 5, 20,
            "test-secret-that-is-at-least-32-characters");

    private OtpChallengeRepository challengeRepo;
    private UserRepository userRepo;
    private MobileLoginRecordRepository recordRepo;
    private TokenService tokenService;
    private OtpHasher hasher;
    private String lastSmsOtp; // what our fake SMS sender "delivered"
    private AuthService service;

    @BeforeEach
    void setUp() {
        challengeRepo = mock(OtpChallengeRepository.class);
        userRepo = mock(UserRepository.class);
        recordRepo = mock(MobileLoginRecordRepository.class);
        hasher = new OtpHasher(props);
        when(userRepo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        tokenService = mock(TokenService.class);
        when(tokenService.issueTokens(any(User.class)))
                .thenReturn(new AuthTokens("test-access", "test-refresh", "Bearer", 900));
        service = new AuthService(challengeRepo, userRepo, recordRepo, new SecureOtpGenerator(), hasher,
                (mobile, otp) -> lastSmsOtp = otp, tokenService, props, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    // ---------------- send-otp ----------------

    @Test
    void sendOtpStoresHashNotPlainOtp() {
        when(challengeRepo.findFirstByMobileNumberOrderByCreatedAtDesc(MOBILE)).thenReturn(Optional.empty());

        SendOtpResponse response = service.sendOtp("98765 43210", IP);

        ArgumentCaptor<OtpChallenge> saved = ArgumentCaptor.forClass(OtpChallenge.class);
        verify(challengeRepo).save(saved.capture());
        OtpChallenge challenge = saved.getValue();
        assertEquals(MOBILE, challenge.getMobileNumber());
        assertNotEquals(lastSmsOtp, challenge.getOtpHash());
        assertTrue(hasher.matches(challenge.getId(), lastSmsOtp, challenge.getOtpHash()));
        assertEquals(NOW.plus(Duration.ofMinutes(5)), challenge.getExpiresAt());
        assertEquals(300, response.expiresInSeconds());
        verify(challengeRepo).invalidateActiveChallenges(MOBILE, NOW);
        verify(recordRepo).recordOtpRequested(MOBILE, NOW); // number is recorded
        assertFalse(response.enteredOTP());                  // nothing typed yet for this OTP
        assertTrue(response.sendOTP());                      // OTP was sent
        assertFalse(challenge.isOtpEntered());
    }

    @Test
    void rateLimitedRequestIsNotRecorded() {
        OtpChallenge recent = challenge("x", NOW.minusSeconds(20));
        when(challengeRepo.findFirstByMobileNumberOrderByCreatedAtDesc(MOBILE)).thenReturn(Optional.of(recent));

        assertThrows(ApiException.class, () -> service.sendOtp(MOBILE, IP));
        verify(recordRepo, never()).recordOtpRequested(anyString(), any());
    }

    @Test
    void sendOtpRejectsInvalidNumber() {
        ApiException ex = assertThrows(ApiException.class, () -> service.sendOtp("12345", IP));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void sendOtpEnforcesResendCooldown() {
        OtpChallenge recent = challenge("x", NOW.minusSeconds(20));
        when(challengeRepo.findFirstByMobileNumberOrderByCreatedAtDesc(MOBILE)).thenReturn(Optional.of(recent));

        ApiException ex = assertThrows(ApiException.class, () -> service.sendOtp(MOBILE, IP));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatus());
        assertEquals(40L, ex.getRetryAfterSeconds());
        verify(challengeRepo, never()).save(any());
    }

    @Test
    void sendOtpEnforcesHourlyLimitPerNumber() {
        when(challengeRepo.findFirstByMobileNumberOrderByCreatedAtDesc(MOBILE)).thenReturn(Optional.empty());
        when(challengeRepo.countByMobileNumberAndCreatedAtAfter(anyString(), any())).thenReturn(5L);

        ApiException ex = assertThrows(ApiException.class, () -> service.sendOtp(MOBILE, IP));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatus());
    }

    // ---------------- verify-otp ----------------

    @Test
    void verifyCorrectOtpCreatesNewVerifiedUser() {
        OtpChallenge challenge = challenge("123456", NOW.minusSeconds(30));
        when(challengeRepo.findByIdForUpdate(challenge.getId())).thenReturn(Optional.of(challenge));
        when(userRepo.findByMobileNumber(MOBILE)).thenReturn(Optional.empty());

        VerifyOtpResponse response = service.verifyOtp(
                new VerifyOtpRequest(challenge.getId(), "9876543210", "123456", Role.CUSTOMER));

        assertTrue(response.isNewUser());
        assertTrue(response.enteredOTP());
        assertEquals("test-access", response.accessToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(900, response.expiresIn());
        assertTrue(response.user().isVerified());
        assertEquals(Role.CUSTOMER, response.user().role());
        assertTrue(challenge.isConsumed());
        verify(recordRepo).recordOtpEntered(MOBILE, NOW);
    }

    @Test
    void returningUserIsNotNew() {
        OtpChallenge challenge = challenge("123456", NOW.minusSeconds(30));
        when(challengeRepo.findByIdForUpdate(challenge.getId())).thenReturn(Optional.of(challenge));
        when(userRepo.findByMobileNumber(MOBILE)).thenReturn(Optional.of(new User(MOBILE, Role.CUSTOMER)));

        VerifyOtpResponse response = service.verifyOtp(
                new VerifyOtpRequest(challenge.getId(), MOBILE, "123456", Role.CUSTOMER));

        assertFalse(response.isNewUser());
    }

    @Test
    void wrongOtpCountsAttempt() {
        OtpChallenge challenge = challenge("123456", NOW.minusSeconds(30));
        when(challengeRepo.findByIdForUpdate(challenge.getId())).thenReturn(Optional.of(challenge));

        ApiException ex = assertThrows(ApiException.class, () -> service.verifyOtp(
                new VerifyOtpRequest(challenge.getId(), MOBILE, "000000", Role.CUSTOMER)));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("Incorrect OTP. 4 attempt(s) remaining.", ex.getMessage());
        assertEquals(1, challenge.getAttemptCount());
        assertFalse(challenge.isConsumed());
        verify(tokenService, never()).issueTokens(any());   // NO tokens without a correct OTP
        assertTrue(challenge.isOtpEntered());              // a wrong OTP still counts as "entered"
        verify(recordRepo).recordOtpEntered(MOBILE, NOW);
    }

    @Test
    void cancelAfterTypingWrongOtpReportsEntered() {
        OtpChallenge challenge = challenge("123456", NOW.minusSeconds(30));
        when(challengeRepo.findByIdForUpdate(challenge.getId())).thenReturn(Optional.of(challenge));
        assertThrows(ApiException.class, () -> service.verifyOtp(
                new VerifyOtpRequest(challenge.getId(), MOBILE, "000000", Role.CUSTOMER)));

        CancelOtpResponse response = service.cancelOtp(new CancelOtpRequest(challenge.getId(), MOBILE));

        assertTrue(response.enteredOTP());
    }

    // ---------------- cancel-otp ----------------

    @Test
    void cancelInvalidatesOtpAndRecordsCancel() {
        OtpChallenge challenge = challenge("123456", NOW.minusSeconds(30));
        when(challengeRepo.findByIdForUpdate(challenge.getId())).thenReturn(Optional.of(challenge));

        CancelOtpResponse response = service.cancelOtp(new CancelOtpRequest(challenge.getId(), "9876543210"));

        assertTrue(challenge.isConsumed());
        assertFalse(response.enteredOTP());
        verify(recordRepo).recordCancelled(MOBILE, NOW);

        // The cancelled OTP can no longer log in
        assertThrows(ApiException.class, () -> service.verifyOtp(
                new VerifyOtpRequest(challenge.getId(), MOBILE, "123456", Role.CUSTOMER)));
    }

    @Test
    void cancellingTwiceCountsOnce() {
        OtpChallenge challenge = challenge("123456", NOW.minusSeconds(30));
        challenge.consume(NOW.minusSeconds(5));
        when(challengeRepo.findByIdForUpdate(challenge.getId())).thenReturn(Optional.of(challenge));

        service.cancelOtp(new CancelOtpRequest(challenge.getId(), MOBILE));

        verify(recordRepo, never()).recordCancelled(anyString(), any());
    }

    @Test
    void cancelForAnotherNumberIsRejected() {
        OtpChallenge challenge = challenge("123456", NOW.minusSeconds(30));
        when(challengeRepo.findByIdForUpdate(challenge.getId())).thenReturn(Optional.of(challenge));

        assertThrows(ApiException.class,
                () -> service.cancelOtp(new CancelOtpRequest(challenge.getId(), "9123456789")));
        assertFalse(challenge.isConsumed());
    }

    @Test
    void lockedAfterMaxAttemptsEvenWithCorrectOtp() {
        OtpChallenge challenge = challenge("123456", NOW.minusSeconds(30));
        for (int i = 0; i < 5; i++) {
            challenge.recordFailedAttempt();
        }
        when(challengeRepo.findByIdForUpdate(challenge.getId())).thenReturn(Optional.of(challenge));

        ApiException ex = assertThrows(ApiException.class, () -> service.verifyOtp(
                new VerifyOtpRequest(challenge.getId(), MOBILE, "123456", Role.CUSTOMER)));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatus());
    }

    @Test
    void expiredOtpIsRejected() {
        OtpChallenge challenge = challenge("123456", NOW.minus(Duration.ofMinutes(6)));
        when(challengeRepo.findByIdForUpdate(challenge.getId())).thenReturn(Optional.of(challenge));

        ApiException ex = assertThrows(ApiException.class, () -> service.verifyOtp(
                new VerifyOtpRequest(challenge.getId(), MOBILE, "123456", Role.CUSTOMER)));
        assertEquals("OTP has expired. Please request a new OTP.", ex.getMessage());
    }

    @Test
    void usedOtpCannotBeReused() {
        OtpChallenge challenge = challenge("123456", NOW.minusSeconds(30));
        challenge.consume(NOW.minusSeconds(10));
        when(challengeRepo.findByIdForUpdate(challenge.getId())).thenReturn(Optional.of(challenge));

        assertThrows(ApiException.class, () -> service.verifyOtp(
                new VerifyOtpRequest(challenge.getId(), MOBILE, "123456", Role.CUSTOMER)));
    }

    @Test
    void challengeForAnotherNumberIsRejected() {
        OtpChallenge challenge = challenge("123456", NOW.minusSeconds(30));
        when(challengeRepo.findByIdForUpdate(challenge.getId())).thenReturn(Optional.of(challenge));

        ApiException ex = assertThrows(ApiException.class, () -> service.verifyOtp(
                new VerifyOtpRequest(challenge.getId(), "9123456789", "123456", Role.CUSTOMER)));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void existingUserRoleComesFromDatabaseNotClient() {
        OtpChallenge challenge = challenge("123456", NOW.minusSeconds(30));
        when(challengeRepo.findByIdForUpdate(challenge.getId())).thenReturn(Optional.of(challenge));
        when(userRepo.findByMobileNumber(MOBILE)).thenReturn(Optional.of(new User(MOBILE, Role.SHOPKEEPER)));

        ApiException ex = assertThrows(ApiException.class, () -> service.verifyOtp(
                new VerifyOtpRequest(challenge.getId(), MOBILE, "123456", Role.CUSTOMER)));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    /** Builds a challenge whose real OTP is {@code otp}, created at {@code createdAt}. */
    private OtpChallenge challenge(String otp, Instant createdAt) {
        java.util.UUID id = java.util.UUID.randomUUID();
        return new OtpChallenge(id, MOBILE, hasher.hash(id, otp),
                createdAt.plus(props.expiry()), IP, createdAt);
    }
}
