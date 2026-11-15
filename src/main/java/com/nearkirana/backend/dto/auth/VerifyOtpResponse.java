package com.nearkirana.backend.dto.auth;

/**
 * "data" part of a successful verify-otp response.
 * Stage 6 will add accessToken, refreshToken, tokenType and expiresIn here.
 */
public record VerifyOtpResponse(
        UserResponse user,
        boolean isNewUser
) {
}
