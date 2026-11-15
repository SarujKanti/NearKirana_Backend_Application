package com.nearkirana.backend.security;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DevFixedOtpGeneratorTest {

    private final DevFixedOtpGenerator generator = new DevFixedOtpGenerator(
            new SecureOtpGenerator(), "123456", List.of("9911111111", "+91 91919 19191"));

    @Test
    void demoNumbersGetFixedOtp() {
        assertEquals("123456", generator.generate("+919911111111"));
        assertEquals("123456", generator.generate("+919191919191"));
    }

    @Test
    void otherNumbersStillGetRandomOtp() {
        // With a random OTP, 20 tries all being "123456" is practically impossible.
        boolean sawDifferentOtp = false;
        for (int i = 0; i < 20; i++) {
            String otp = generator.generate("+919876543210");
            assertTrue(otp.matches("\\d{6}"));
            sawDifferentOtp |= !otp.equals("123456");
        }
        assertTrue(sawDifferentOtp);
    }

    @Test
    void rejectsBadConfiguration() {
        assertThrows(IllegalStateException.class,
                () -> new DevFixedOtpGenerator(new SecureOtpGenerator(), "12", List.of("9911111111")));
        assertThrows(IllegalStateException.class,
                () -> new DevFixedOtpGenerator(new SecureOtpGenerator(), "123456", List.of("12345")));
    }
}
