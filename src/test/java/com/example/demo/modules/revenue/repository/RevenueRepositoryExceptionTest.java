
package com.example.demo.modules.revenue.repository;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RevenueRepositoryExceptionTest {

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);

    private final RevenueRepository repository = new RevenueRepository(jdbcTemplate);

    // TC01: SQL Server mat ket noi khi tinh doanh thu
    @Test
    void testDatabaseConnectionFailure() {

        when(jdbcTemplate.queryForObject(
                anyString(),
                eq(BigDecimal.class),
                any(LocalDate.class),
                any(LocalDate.class)))
                .thenThrow(new DataAccessResourceFailureException(
                        "Database connection failed"));

        assertThrows(DataAccessException.class,
                () -> repository.getRevenue(
                        LocalDate.of(2026, 9, 1),
                        LocalDate.of(2026, 10, 1)));
    }

    // TC02: Truy van dem booking bi loi
    @Test
    void testBookingCountQueryFailure() {

        when(jdbcTemplate.queryForObject(
                anyString(),
                eq(Long.class),
                any(LocalDate.class),
                any(LocalDate.class)))
                .thenThrow(new DataAccessResourceFailureException(
                        "Database unavailable"));

        assertThrows(DataAccessException.class,
                () -> repository.getCancelledBookings(
                        LocalDate.of(2026, 9, 1),
                        LocalDate.of(2026, 10, 1)));
    }
}
