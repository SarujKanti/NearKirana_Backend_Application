package com.nearkirana.backend.dto.auth;

import com.nearkirana.backend.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

/**
 * Body of POST /api/v1/auth/verify-otp
 * {
 *   "challengeId": "from send-otp",
 *   "mobileNumber": "9876543210",
 *   "otp": "123456",
 *   "role": "CUSTOMER"        <- which app the user is signing in to
 * }
 */
public record VerifyOtpRequest(
        @NotNull(message = "challengeId is required")
        UUID challengeId,

        @NotBlank(message = "Mobile number is required")
        String mobileNumber,

        @NotBlank(message = "OTP is required")
        @Pattern(regexp = "\\d{6}", message = "OTP must be 6 digits")
        String otp,

        @NotNull(message = "role is required (CUSTOMER or SHOPKEEPER)")
        Role role
) {
}
