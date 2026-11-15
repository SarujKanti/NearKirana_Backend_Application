package com.nearkirana.backend.security;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Creates 6-digit OTPs using SecureRandom (cryptographically strong).
 * Never use java.util.Random for security: its numbers can be predicted.
 */
@Component
public class SecureOtpGenerator implements OtpGenerator {

    private static final int OTP_BOUND = 1_000_000; // 000000 .. 999999

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String generate(String mobileNumber) {
        return String.format("%06d", secureRandom.nextInt(OTP_BOUND));
    }
}
