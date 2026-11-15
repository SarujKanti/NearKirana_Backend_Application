package com.nearkirana.backend.dto.auth;

/**
 * "data" part of a successful verify-otp response.
 *
 * isNewUser:  true only on the number's first successful verification.
 * enteredOTP: has the user typed an OTP for THIS request? Always true here
 *             (they just did); kept so every auth response has the same field.
 *
 * Stage 6 will add accessToken, refreshToken, tokenType and expiresIn here.
 */
public record VerifyOtpResponse(
        UserResponse user,
        boolean isNewUser,
        boolean enteredOTP
) {
}
