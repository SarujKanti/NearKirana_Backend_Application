package com.nearkirana.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;

/** Body of POST /api/v1/auth/send-otp  ->  { "mobileNumber": "9876543210" } */
public record SendOtpRequest(
        @NotBlank(message = "Mobile number is required")
        String mobileNumber
) {
}
