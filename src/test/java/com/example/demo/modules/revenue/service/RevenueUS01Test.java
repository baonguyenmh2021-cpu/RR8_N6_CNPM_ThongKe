
package com.example.demo.modules.revenue.service;

import com.example.demo.modules.revenue.dto.MonthlyRevenueResponse;
import com.example.demo.modules.revenue.repository.RevenueRepository;
import com.example.demo.modules.revenue.repository.RevenueRepository.BookingSummary;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RevenueUS01Test {

    private final RevenueRepository repository = mock(RevenueRepository.class);

    private final RevenueService service = new RevenueService(repository);

    // TC01: Doanh thu thang binh thuong
    @Test
    void testMonthlyRevenue() {
        LocalDate from = LocalDate.of(2026, 9, 1);
        LocalDate to = LocalDate.of(2026, 10, 1);

        when(repository.getRevenue(from, to))
                .thenReturn(new BigDecimal("11155000"));

        when(repository.getRevenue(
                LocalDate.of(2026, 8, 1), from))
                .thenReturn(new BigDecimal("1200000"));

        when(repository.getBookingSummary(from, to))
                .thenReturn(new BookingSummary(
                        2, new BigDecimal("395000")));

        when(repository.getCancelledBookings(from, to))
                .thenReturn(0L);

        MonthlyRevenueResponse result = service.getMonthlyRevenue(2026, 9);

        assertEquals(
                new BigDecimal("11155000"), result.revenue());

        assertEquals(2, result.bookingCount());

        assertEquals(
                new BigDecimal("395000"), result.totalDiscount());

        assertEquals(
                0, result.cancelledBookingsCreatedInMonth());

        assertEquals(
                new BigDecimal("829.6"),
                result.revenueChangePercent());
    }

    // TC02: Thang khong co doanh thu
    @Test
    void testEmptyMonth() {
        LocalDate from = LocalDate.of(2026, 6, 1);
        LocalDate to = LocalDate.of(2026, 7, 1);

        when(repository.getRevenue(
                any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(BigDecimal.ZERO);

        when(repository.getBookingSummary(from, to))
                .thenReturn(new BookingSummary(
                        0, BigDecimal.ZERO));

        when(repository.getCancelledBookings(from, to))
                .thenReturn(0L);

        MonthlyRevenueResponse result = service.getMonthlyRevenue(2026, 6);

        assertEquals(BigDecimal.ZERO, result.revenue());
        assertEquals(0, result.bookingCount());
        assertEquals(BigDecimal.ZERO, result.totalDiscount());
        assertNull(result.revenueChangePercent());
    }

    // TC03: Thang hoac nam khong hop le
    @Test
    void testInvalidMonth() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.getMonthlyRevenue(2026, 13));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.getMonthlyRevenue(2026, 0));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.getMonthlyRevenue(1752, 9));

        verifyNoInteractions(repository);
    }

    // TC04: Chuyen nam tu thang 12 sang thang 1
    @Test
    void testPreviousYear() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 2, 1);
        LocalDate previous = LocalDate.of(2025, 12, 1);

        when(repository.getRevenue(from, to))
                .thenReturn(new BigDecimal("2000000"));

        when(repository.getRevenue(previous, from))
                .thenReturn(new BigDecimal("1000000"));

        when(repository.getBookingSummary(from, to))
                .thenReturn(new BookingSummary(1, BigDecimal.ZERO));

        when(repository.getCancelledBookings(from, to))
                .thenReturn(0L);

        MonthlyRevenueResponse result = service.getMonthlyRevenue(2026, 1);

        assertEquals(
                new BigDecimal("100.0"),
                result.revenueChangePercent());

        verify(repository).getRevenue(previous, from);
    }
}
