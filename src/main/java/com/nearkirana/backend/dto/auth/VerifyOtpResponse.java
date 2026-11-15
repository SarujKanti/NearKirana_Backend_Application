package com.nearkirana.backend.dto.auth;

/**
 * "data" part of a successful verify-otp response:
 * {
 *   "accessToken": "eyJ...", "refreshToken": "...", "tokenType": "Bearer", "expiresIn": 900,
 *   "user": { ... }, "isNewUser": true, "enteredOTP": true
 * }
 *
 * isNewUser:  true only on the number's first successful verification.
 * enteredOTP: has the user typed an OTP for THIS request? Always true here
 *             (they just did); kept so every auth response has the same field.
 */
public record VerifyOtpResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UserResponse user,
        boolean isNewUser,
        boolean enteredOTP
) {
    public static VerifyOtpResponse of(AuthTokens tokens, UserResponse user, boolean isNewUser) {
        return new VerifyOtpResponse(tokens.accessToken(), tokens.refreshToken(), tokens.tokenType(),
                tokens.expiresIn(), user, isNewUser, true);
    }
}
