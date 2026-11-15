package com.nearkirana.backend.dto;

/**
 * DTO (Data Transfer Object) returned by GET /api/v1/health.
 *
 * A Java "record" is a compact, immutable class: Java automatically creates
 * the constructor, the getters (status(), application(), database(), message()),
 * equals(), hashCode() and toString() for us.
 *
 * Spring converts this object into JSON automatically:
 * {
 *   "status": "UP",
 *   "application": "NearKirana",
 *   "database": "UP",
 *   "message": "NearKirana Backend is running successfully!"
 * }
 */
public record HealthResponse(
        String status,
        String application,
        String database,
        String message
) {
}
