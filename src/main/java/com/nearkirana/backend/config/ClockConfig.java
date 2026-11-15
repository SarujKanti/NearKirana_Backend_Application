package com.nearkirana.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Provides "the current time" as a bean. Code asks the Clock instead of
 * calling Instant.now() directly, so tests can use a fixed, fake time
 * (e.g. to test "OTP expired after 5 minutes" without waiting 5 minutes).
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
