package com.nearkirana.backend.service;

import com.nearkirana.backend.dto.HealthResponse;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for HealthService.
 * We use a fake ("mock") DataSource, so these tests do not need a real database.
 */
class HealthServiceTest {

    @Test
    void returnsUpWhenDatabaseIsReachable() throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(2)).thenReturn(true);

        HealthResponse response = new HealthService(dataSource).getHealthStatus();

        assertEquals("UP", response.status());
        assertEquals("UP", response.database());
        assertEquals("NearKirana", response.application());
        verify(connection).close(); // connection must be returned to the pool
    }

    @Test
    void returnsDownWhenDatabaseIsUnreachable() throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenThrow(new SQLException("Connection refused"));

        HealthResponse response = new HealthService(dataSource).getHealthStatus();

        assertEquals("DOWN", response.status());
        assertEquals("DOWN", response.database());
    }
}
