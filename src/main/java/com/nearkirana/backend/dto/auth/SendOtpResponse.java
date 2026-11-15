package com.nearkirana.backend.dto.auth;

import java.util.UUID;

/** "data" part of a successful send-otp response. The OTP itself is never included. */
public record SendOtpResponse(
        UUID challengeId,
        long expiresInSeconds,
        long resendAfterSeconds
) {
}
