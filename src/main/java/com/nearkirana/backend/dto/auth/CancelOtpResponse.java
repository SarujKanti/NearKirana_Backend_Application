package com.nearkirana.backend.dto.auth;

/**
 * "data" part of a cancel-otp response.
 *
 * enteredOTP: did the user type an OTP for THIS request before cancelling?
 *             false = closed the screen without trying; true = tried, then gave up.
 */
public record CancelOtpResponse(
        boolean enteredOTP
) {
}
