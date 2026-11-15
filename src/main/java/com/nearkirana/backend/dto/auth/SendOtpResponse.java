package com.nearkirana.backend.dto.auth;

import java.util.UUID;

/**
 * "data" part of a successful send-otp response. The OTP itself is never included.
 *
 * enteredOTP: has this number EVER entered an OTP before (in any earlier attempt)?
 *             false for a first-time number or one that always cancelled.
 */
public record SendOtpResponse(
        UUID challengeId,
        long expiresInSeconds,
        long resendAfterSeconds,
        boolean enteredOTP
) {
}
