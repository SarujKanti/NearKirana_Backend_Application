package com.nearkirana.backend.dto.auth;

/**
 * "data" part of a cancel-otp response.
 *
 * enteredOTP: has this number ever entered an OTP (in this or any earlier attempt)?
 */
public record CancelOtpResponse(
        boolean enteredOTP
) {
}
