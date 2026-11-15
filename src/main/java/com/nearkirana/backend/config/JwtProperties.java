package com.nearkirana.backend.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * JWT settings (prefix "nearkirana.jwt").
 * secret: at least 32 characters, because HS256 needs a 256-bit key.
 */
@Validated
@ConfigurationProperties(prefix = "nearkirana.jwt")
public record JwtProperties(
        @NotBlank String issuer,
        @NotNull Duration accessTokenTtl,    // e.g. 15m
        @NotNull Duration refreshTokenTtl,   // e.g. 30d
        @NotBlank @Size(min = 32) String secret
) {
}
