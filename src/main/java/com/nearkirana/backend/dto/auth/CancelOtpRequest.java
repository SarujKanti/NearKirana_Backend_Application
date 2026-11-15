package com.nearkirana.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Body of POST /api/v1/auth/cancel-otp. The app sends this when the user
 * closes / cancels the OTP screen.
 * { "challengeId": "from send-otp", "mobileNumber": "9876543210" }
 */
public record CancelOtpRequest(
        @NotNull(message = "challengeId is required")
        UUID challengeId,

        @NotBlank(message = "Mobile number is required")
        String mobileNumber
) {
}
