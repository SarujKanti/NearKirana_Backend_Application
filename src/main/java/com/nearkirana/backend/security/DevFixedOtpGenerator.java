package com.nearkirana.backend.security;

import com.nearkirana.backend.util.MobileNumbers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * DEV / DEMO ONLY: demo mobile numbers always get the same OTP.
 * Every other number still gets a secure random OTP.
 *
 * @Profile({"dev", "demo"}): exists only in the dev (laptop) and demo (Render
 *                  testing) profiles. In real production neither is active, so a
 *                  fixed OTP can never work in production.
 * @Primary:        when both generators exist (dev or demo), Spring injects this one.
 */
@Component
@Primary
@Profile({"dev", "demo"})
public class DevFixedOtpGenerator implements OtpGenerator {

    private static final Logger log = LoggerFactory.getLogger(DevFixedOtpGenerator.class);

    private final SecureOtpGenerator secureOtpGenerator;
    private final String fixedOtp;
    private final Set<String> demoNumbers;

    public DevFixedOtpGenerator(SecureOtpGenerator secureOtpGenerator,
                                @Value("${nearkirana.dev-otp.fixed-otp}") String fixedOtp,
                                @Value("${nearkirana.dev-otp.demo-numbers}") List<String> demoNumbers) {
        if (fixedOtp == null || !fixedOtp.matches("\\d{6}")) {
            throw new IllegalStateException("nearkirana.dev-otp.fixed-otp must be 6 digits");
        }
        this.secureOtpGenerator = secureOtpGenerator;
        this.fixedOtp = fixedOtp;
        this.demoNumbers = demoNumbers.stream()
                .map(raw -> MobileNumbers.normalize(raw).orElseThrow(() ->
                        new IllegalStateException("Invalid demo number in nearkirana.dev-otp.demo-numbers: " + raw)))
                .collect(Collectors.toUnmodifiableSet());

        log.warn("DEV PROFILE: fixed OTP is enabled for {} demo number(s): {}",
                this.demoNumbers.size(),
                this.demoNumbers.stream().map(MobileNumbers::mask).sorted().toList());
    }

    @Override
    public String generate(String mobileNumber) {
        return demoNumbers.contains(mobileNumber) ? fixedOtp : secureOtpGenerator.generate(mobileNumber);
    }
}
