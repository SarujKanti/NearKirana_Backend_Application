package com.nearkirana.backend.security;

import com.nearkirana.backend.config.OtpProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OtpHasherTest {

    static OtpProperties props(String secret) {
        return new OtpProperties(Duration.ofMinutes(5), 5, Duration.ofSeconds(60), 5, 20, secret);
    }

    private final OtpHasher hasher = new OtpHasher(props("test-secret-that-is-at-least-32-characters"));

    @Test
    void hashIsNotThePlainOtp() {
        String hash = hasher.hash(UUID.randomUUID(), "123456");
        assertNotEquals("123456", hash);
        assertEquals(64, hash.length()); // SHA-256 = 32 bytes = 64 hex characters
    }

    @Test
    void matchesOnlyTheCorrectOtp() {
        UUID id = UUID.randomUUID();
        String hash = hasher.hash(id, "123456");
        assertTrue(hasher.matches(id, "123456", hash));
        assertFalse(hasher.matches(id, "654321", hash));
    }

    @Test
    void sameOtpDiffersPerChallenge() {
        assertNotEquals(hasher.hash(UUID.randomUUID(), "123456"), hasher.hash(UUID.randomUUID(), "123456"));
    }

    @Test
    void differentSecretGivesDifferentHash() {
        UUID id = UUID.randomUUID();
        OtpHasher other = new OtpHasher(props("another-secret-that-is-at-least-32-chars"));
        assertNotEquals(hasher.hash(id, "123456"), other.hash(id, "123456"));
    }

    @Test
    void generatorAlwaysGivesSixDigits() {
        OtpGenerator generator = new OtpGenerator();
        for (int i = 0; i < 1000; i++) {
            assertTrue(generator.generate().matches("\\d{6}"));
        }
    }
}
