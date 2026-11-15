package com.nearkirana.backend.security;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Creates 6-digit OTPs using SecureRandom (cryptographically strong).
 * Never use java.util.Random for security: its numbers can be predicted.
 */
@Component
public class OtpGenerator {

    private static final int OTP_BOUND = 1_000_000; // 000000 .. 999999

    private final SecureRandom secureRandom = new SecureRandom();

    public String generate() {
        return String.format("%06d", secureRandom.nextInt(OTP_BOUND));
    }
}
