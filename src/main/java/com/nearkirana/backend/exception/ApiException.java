package com.nearkirana.backend.exception;

import org.springframework.http.HttpStatus;

/**
 * An expected, user-facing error: "Invalid OTP", "Too many requests", ...
 * GlobalExceptionHandler turns it into a clean JSON response with the right status code.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final Long retryAfterSeconds;

    public ApiException(HttpStatus status, String message) {
        this(status, message, null);
    }

    public ApiException(HttpStatus status, String message, Long retryAfterSeconds) {
        super(message);
        this.status = status;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public HttpStatus getStatus() {
        return status;
    }

    /** For 429 responses: how many seconds the client should wait (sent as the Retry-After header). */
    public Long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
