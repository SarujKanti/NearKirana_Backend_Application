package com.nearkirana.backend.controller;

import com.nearkirana.backend.dto.ApiResponse;
import com.nearkirana.backend.dto.auth.SendOtpRequest;
import com.nearkirana.backend.dto.auth.SendOtpResponse;
import com.nearkirana.backend.dto.auth.VerifyOtpRequest;
import com.nearkirana.backend.dto.auth.VerifyOtpResponse;
import com.nearkirana.backend.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication endpoints.
 * POST is used (not GET) because we send data in the body and the server changes state.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // POST http://localhost:8080/api/v1/auth/send-otp
    @PostMapping("/send-otp")
    public ResponseEntity<ApiResponse<SendOtpResponse>> sendOtp(
            @Valid @RequestBody SendOtpRequest request,   // JSON body -> Java record, then validated
            HttpServletRequest httpRequest) {
        // Note: behind a proxy/load balancer in production we will configure
        // forwarded headers so this is the real client IP.
        String clientIp = httpRequest.getRemoteAddr();
        SendOtpResponse data = authService.sendOtp(request.mobileNumber(), clientIp);
        return ResponseEntity.ok(ApiResponse.ok("OTP sent successfully", data));
    }

    // POST http://localhost:8080/api/v1/auth/verify-otp
    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<VerifyOtpResponse>> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request) {
        VerifyOtpResponse data = authService.verifyOtp(request);
        return ResponseEntity.ok(ApiResponse.ok("Mobile number verified successfully", data));
    }
}
