package com.nearkirana.backend.dto.auth;

import java.util.UUID;

/**
 * "data" part of a successful send-otp response. The OTP itself is never included.
 *
 * enteredOTP: has the user typed an OTP for THIS request? Always false here,
 *             because the OTP was only just sent.
 * sendOTP:    the OTP was generated and handed to the SMS service.
 */
public record SendOtpResponse(
        UUID challengeId,
        long expiresInSeconds,
        long resendAfterSeconds,
        boolean enteredOTP,
        boolean sendOTP
) {
}
