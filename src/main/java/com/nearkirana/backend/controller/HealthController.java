package com.nearkirana.backend.controller;

import com.nearkirana.backend.dto.HealthResponse;
import com.nearkirana.backend.service.HealthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller layer: the "front door" of the backend.
 *
 * It receives HTTP requests, calls the service, and returns the result.
 * It should NOT contain business logic.
 */
@RestController
@RequestMapping("/api/v1")
public class HealthController {

    private final HealthService healthService;

    // Constructor injection: Spring creates HealthService and passes it in here.
    public HealthController(HealthService healthService) {
        this.healthService = healthService;
    }

    // Handles: GET http://localhost:8080/api/v1/health
    // 200 OK                  -> everything is working
    // 503 Service Unavailable -> backend is running but the database is not reachable
    @GetMapping("/health")
    public ResponseEntity<HealthResponse> getHealth() {
        HealthResponse response = healthService.getHealthStatus();
        HttpStatus httpStatus = "UP".equals(response.status())
                ? HttpStatus.OK
                : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(httpStatus).body(response);
    }
}
