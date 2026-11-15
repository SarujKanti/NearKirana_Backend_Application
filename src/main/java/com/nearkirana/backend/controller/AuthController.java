package com.nearkirana.backend.controller;

import com.nearkirana.backend.dto.ApiResponse;
import com.nearkirana.backend.dto.auth.AuthTokens;
import com.nearkirana.backend.dto.auth.CancelOtpRequest;
import com.nearkirana.backend.dto.auth.CancelOtpResponse;
import com.nearkirana.backend.dto.auth.RefreshTokenRequest;
import com.nearkirana.backend.dto.auth.SendOtpRequest;
import com.nearkirana.backend.dto.auth.SendOtpResponse;
import com.nearkirana.backend.dto.auth.UserResponse;
import com.nearkirana.backend.dto.auth.VerifyOtpRequest;
import com.nearkirana.backend.dto.auth.VerifyOtpResponse;
import com.nearkirana.backend.security.TokenService;
import com.nearkirana.backend.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
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
    private final TokenService tokenService;

    public AuthController(AuthService authService, TokenService tokenService) {
        this.authService = authService;
        this.tokenService = tokenService;
    }

    // POST http://localhost:8080/api/v1/auth/send-otp            (public)
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

    // POST http://localhost:8080/api/v1/auth/verify-otp          (public)
    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<VerifyOtpResponse>> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request) {
        VerifyOtpResponse data = authService.verifyOtp(request);
        return ResponseEntity.ok(ApiResponse.ok("Mobile number verified successfully", data));
    }

    // POST http://localhost:8080/api/v1/auth/cancel-otp          (public)
    // Called by the app when the user closes / cancels the OTP screen.
    @PostMapping("/cancel-otp")
    public ResponseEntity<ApiResponse<CancelOtpResponse>> cancelOtp(
            @Valid @RequestBody CancelOtpRequest request) {
        CancelOtpResponse data = authService.cancelOtp(request);
        return ResponseEntity.ok(ApiResponse.ok("OTP request cancelled", data));
    }

    // POST http://localhost:8080/api/v1/auth/refresh-token       (public: the refresh token IS the proof)
    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<AuthTokens>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {
        AuthTokens tokens = tokenService.refresh(request.refreshToken());
        return ResponseEntity.ok(ApiResponse.ok("Token refreshed successfully", tokens));
    }

    // POST http://localhost:8080/api/v1/auth/logout              (public: works even if the access token expired)
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Valid @RequestBody RefreshTokenRequest request) {
        tokenService.logout(request.refreshToken());
        return ResponseEntity.ok(ApiResponse.ok("Logged out successfully", null));
    }

    // GET http://localhost:8080/api/v1/auth/me                   (needs: Authorization: Bearer <accessToken>)
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> me(@AuthenticationPrincipal Jwt jwt) {
        // Spring Security already verified this JWT; "sub" holds our user id.
        Long userId = Long.valueOf(jwt.getSubject());
        UserResponse user = authService.getCurrentUser(userId);
        return ResponseEntity.ok(ApiResponse.ok("User details fetched successfully", user));
    }
}
