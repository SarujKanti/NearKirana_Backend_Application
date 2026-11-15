package com.nearkirana.backend.dto.auth;

/**
 * "data" part of a successful verify-otp response.
 *
 * isNewUser:  true only on the number's first successful verification.
 * enteredOTP: always true here (the user just entered the OTP); kept so the
 *             app can read the same field from every auth response.
 *
 * Stage 6 will add accessToken, refreshToken, tokenType and expiresIn here.
 */
public record VerifyOtpResponse(
        UserResponse user,
        boolean isNewUser,
        boolean enteredOTP
) {
}
