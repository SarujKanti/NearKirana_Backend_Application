package com.nearkirana.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body of POST /api/v1/auth/refresh-token and POST /api/v1/auth/logout
 * { "refreshToken": "..." }
 */
public record RefreshTokenRequest(
        @NotBlank(message = "refreshToken is required")
        @Size(max = 200, message = "Invalid refresh token")
        String refreshToken
) {
}
