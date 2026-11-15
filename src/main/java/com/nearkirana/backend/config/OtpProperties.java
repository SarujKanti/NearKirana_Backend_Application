package com.nearkirana.backend.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * All OTP settings, read from application.properties (prefix "nearkirana.otp").
 * @Validated: the app refuses to start if a value is missing or invalid.
 */
@Validated
@ConfigurationProperties(prefix = "nearkirana.otp")
public record OtpProperties(
        @NotNull Duration expiry,              // how long an OTP is valid
        @Min(1) int maxAttempts,               // wrong guesses allowed per OTP
        @NotNull Duration resendCooldown,      // wait time before asking again
        @Min(1) int maxSendsPerHour,           // per mobile number
        @Min(1) int maxSendsPerIpPerHour,      // per device / network
        @NotBlank @Size(min = 32) String hashSecret // server secret for hashing OTPs
) {
}
