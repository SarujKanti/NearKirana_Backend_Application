package com.nearkirana.backend.dto.auth;

/**
 * The token pair the app receives after login or refresh.
 *
 * accessToken:  short-lived JWT, sent as "Authorization: Bearer <token>" on every API call
 * refreshToken: long-lived random string, used ONLY with /auth/refresh-token and /auth/logout
 * tokenType:    always "Bearer"
 * expiresIn:    access token lifetime in seconds (e.g. 900 = 15 minutes)
 */
public record AuthTokens(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn
) {
}
