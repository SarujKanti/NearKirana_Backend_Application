package com.nearkirana.backend.service;

import com.nearkirana.backend.dto.HealthResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Service layer: holds the business logic.
 *
 * The health check now also asks: "can we reach the PostgreSQL database?"
 * If the database is down, the backend cannot serve shops, products or orders,
 * so the overall status becomes DOWN.
 */
@Service
public class HealthService {

    private static final Logger log = LoggerFactory.getLogger(HealthService.class);

    private static final String UP = "UP";
    private static final String DOWN = "DOWN";
    private static final String APPLICATION_NAME = "NearKirana";
    private static final String SUCCESS_MESSAGE = "NearKirana Backend is running successfully!";
    private static final String DATABASE_DOWN_MESSAGE = "NearKirana Backend cannot reach the database.";
    private static final int DB_CHECK_TIMEOUT_SECONDS = 2;

    // DataSource = Spring's connection pool (HikariCP), configured from application.properties.
    private final DataSource dataSource;

    public HealthService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public HealthResponse getHealthStatus() {
        if (isDatabaseUp()) {
            return new HealthResponse(UP, APPLICATION_NAME, UP, SUCCESS_MESSAGE);
        }
        return new HealthResponse(DOWN, APPLICATION_NAME, DOWN, DATABASE_DOWN_MESSAGE);
    }

    private boolean isDatabaseUp() {
        // try-with-resources: the connection is always returned to the pool afterwards.
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(DB_CHECK_TIMEOUT_SECONDS);
        } catch (SQLException e) {
            // Log the reason for developers; never send internal error details to the client.
            log.warn("Database health check failed: {}", e.getMessage());
            return false;
        }
    }
}
