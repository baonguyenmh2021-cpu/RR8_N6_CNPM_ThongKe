
package com.example.demo.modules.revenue.service;

import com.example.demo.modules.revenue.dto.ClosedMonthlyReportResponse;
import com.example.demo.modules.revenue.repository.RevenueRepository;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RevenueUS04Test {

    private final RevenueRepository repository = mock(RevenueRepository.class);

    private final RevenueService service = new RevenueService(repository);

    // Du lieu bao cao mau dung cho Unit Test
    private ClosedMonthlyReportResponse sampleReport() {
        return new ClosedMonthlyReportResponse(
                1,
                2026,
                9,
                new BigDecimal("11155000"),
                new BigDecimal("395000"),
                2,
                0,
                1,
                LocalDateTime.of(2026, 11, 17, 9, 21, 54));
    }

    // TC01: Xem bao cao da chot
    @Test
    void testGetClosedReportSuccess() {

        when(repository.getClosedMonthlyReport(2026, 9))
                .thenReturn(Optional.of(sampleReport()));

        Optional<ClosedMonthlyReportResponse> result = service.getClosedMonthlyReport(2026, 9);

        assertTrue(result.isPresent());

        ClosedMonthlyReportResponse report = result.get();

        assertEquals(1, report.reportId());
        assertEquals(2026, report.year());
        assertEquals(9, report.month());

        assertEquals(0,
                new BigDecimal("11155000")
                        .compareTo(report.totalRevenue()));

        assertEquals(2, report.bookingCount());

        verify(repository)
                .getClosedMonthlyReport(2026, 9);
    }

    // TC02: Thang chua co bao cao chot
    @Test
    void testGetClosedReportNotFound() {

        when(repository.getClosedMonthlyReport(2026, 8))
                .thenReturn(Optional.empty());

        Optional<ClosedMonthlyReportResponse> result = service.getClosedMonthlyReport(2026, 8);

        assertTrue(result.isEmpty());

        verify(repository)
                .getClosedMonthlyReport(2026, 8);
    }

    // TC03: Xem bao cao voi thang khong hop le
    @Test
    void testGetReportInvalidMonth() {

        assertThrows(
                IllegalArgumentException.class,
                () -> service.getClosedMonthlyReport(2026, 0));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.getClosedMonthlyReport(2026, 13));

        verifyNoInteractions(repository);
    }

    // TC04: Admin chot bao cao thanh cong
    @Test
    void testCloseReportSuccess() {

        when(repository.closeMonthlyReport(2026, 10, 1))
                .thenReturn(new ClosedMonthlyReportResponse(
                        2,
                        2026,
                        10,
                        new BigDecimal("1080000"),
                        BigDecimal.ZERO,
                        1,
                        0,
                        1,
                        LocalDateTime.of(2026, 11, 17, 10, 0)));

        ClosedMonthlyReportResponse result = service.closeMonthlyReport(2026, 10, 1);

        assertEquals(2, result.reportId());
        assertEquals(2026, result.year());
        assertEquals(10, result.month());

        assertEquals(0,
                new BigDecimal("1080000")
                        .compareTo(result.totalRevenue()));

        verify(repository)
                .closeMonthlyReport(2026, 10, 1);
    }

    // TC05: Nam, thang khong hop le khi chot
    @Test
    void testCloseReportInvalidPeriod() {

        assertThrows(
                IllegalArgumentException.class,
                () -> service.closeMonthlyReport(2026, 13, 1));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.closeMonthlyReport(1752, 9, 1));

        verifyNoInteractions(repository);
    }

    // TC06: ID nguoi thuc hien khong hop le
    @Test
    void testCloseReportInvalidAdminId() {

        assertThrows(
                IllegalArgumentException.class,
                () -> service.closeMonthlyReport(2026, 9, 0));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.closeMonthlyReport(2026, 9, -1));

        verifyNoInteractions(repository);
    }

    // TC07: Repository bao loi khi chot trung
    @Test
    void testCloseReportDuplicate() {

        when(repository.closeMonthlyReport(2026, 9, 1))
                .thenThrow(
                        new DataIntegrityViolationException(
                                "Bao cao thang nay da chot"));

        assertThrows(
                DataAccessException.class,
                () -> service.closeMonthlyReport(2026, 9, 1));

        verify(repository)
                .closeMonthlyReport(2026, 9, 1);
    }

    // TC08: Database khong kha dung khi chot
    @Test
    void testCloseReportDatabaseFailure() {

        when(repository.closeMonthlyReport(2026, 10, 1))
                .thenThrow(
                        new DataAccessResourceFailureException(
                                "Database unavailable"));

        assertThrows(
                DataAccessResourceFailureException.class,
                () -> service.closeMonthlyReport(2026, 10, 1));
    }
}
