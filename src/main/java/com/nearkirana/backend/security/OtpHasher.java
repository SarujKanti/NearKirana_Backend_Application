package com.nearkirana.backend.security;

import com.nearkirana.backend.config.OtpProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Hashes OTPs with HMAC-SHA256 and a server-side secret.
 *
 * Why HMAC and not plain SHA-256? A 6-digit OTP has only 1,000,000 values.
 * Anyone who stole a plain SHA-256 hash could try all of them in a second.
 * With HMAC they would ALSO need our secret, which is never in the database.
 *
 * The challengeId is mixed in, so the same OTP gives a different hash
 * in every challenge.
 */
@Component
public class OtpHasher {

    private static final String ALGORITHM = "HmacSHA256";

    private final SecretKeySpec key;

    public OtpHasher(OtpProperties otpProperties) {
        this.key = new SecretKeySpec(
                otpProperties.hashSecret().getBytes(StandardCharsets.UTF_8), ALGORITHM);
    }

    public String hash(UUID challengeId, String otp) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM); // Mac is not thread-safe: one per call
            mac.init(key);
            byte[] digest = mac.doFinal((challengeId + ":" + otp).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC-SHA256 is not available", e);
        }
    }

    /** Constant-time comparison: does not leak how many characters matched. */
    public boolean matches(UUID challengeId, String otp, String expectedHash) {
        byte[] actual = hash(challengeId, otp).getBytes(StandardCharsets.UTF_8);
        byte[] expected = expectedHash.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(actual, expected);
    }
}
