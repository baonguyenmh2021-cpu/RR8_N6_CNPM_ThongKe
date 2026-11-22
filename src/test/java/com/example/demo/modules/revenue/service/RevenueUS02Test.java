
package com.example.demo.modules.revenue.service;

import com.example.demo.modules.revenue.dto.YearlyRevenueResponse;
import com.example.demo.modules.revenue.repository.RevenueRepository;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RevenueUS02Test {

    private final RevenueRepository repository = mock(RevenueRepository.class);

    private final RevenueService service = new RevenueService(repository);

    // TC01: Kiem tra du 12 thang va tong doanh thu
    @Test
    void testYearlyRevenue() {

        when(repository.getMonthlyRevenuesByYear(2026))
                .thenReturn(Map.of(
                        8, new BigDecimal("1200000"),
                        9, new BigDecimal("11155000"),
                        10, new BigDecimal("1080000")));

        YearlyRevenueResponse result = service.getYearlyRevenue(2026);

        // Du 12 thang
        assertEquals(12, result.months().size());

        // Thu tu thang phai tu 1 den 12
        for (int i = 1; i <= 12; i++) {
            assertEquals(i, result.months().get(i - 1).month());
        }

        // Tong doanh thu
        assertEquals(0, new BigDecimal("13435000")
                .compareTo(result.totalRevenue()));

        // Thang 1 khong co doanh thu
        assertEquals(0, BigDecimal.ZERO
                .compareTo(result.months().get(0).revenue()));

        // Doanh thu thang 9
        assertEquals(0, new BigDecimal("11155000")
                .compareTo(result.months().get(8).revenue()));

        // Thang 9 cao nhat
        assertEquals(List.of(9), result.highestRevenueMonths());
    }

    // TC02: Nam khong co doanh thu
    @Test
    void testEmptyYear() {

        when(repository.getMonthlyRevenuesByYear(2026))
                .thenReturn(Map.of());

        YearlyRevenueResponse result = service.getYearlyRevenue(2026);

        assertEquals(12, result.months().size());

        assertEquals(0, BigDecimal.ZERO
                .compareTo(result.totalRevenue()));

        for (var item : result.months()) {
            assertEquals(0, BigDecimal.ZERO
                    .compareTo(item.revenue()));
        }

        // Logic hien tai: 12 thang dong hang 0
        assertEquals(12, result.highestRevenueMonths().size());
    }

    // TC03: Nhieu thang dong hang cao nhat
    @Test
    void testSameHighestRevenue() {

        when(repository.getMonthlyRevenuesByYear(2026))
                .thenReturn(Map.of(
                        3, new BigDecimal("5000000"),
                        9, new BigDecimal("5000000"),
                        12, new BigDecimal("2000000")));

        YearlyRevenueResponse result = service.getYearlyRevenue(2026);

        assertEquals(List.of(3, 9),
                result.highestRevenueMonths());

        assertEquals(0, new BigDecimal("12000000")
                .compareTo(result.totalRevenue()));
    }

    // TC04: Nam khong hop le
    @Test
    void testInvalidYear() {

        assertThrows(
                IllegalArgumentException.class,
                () -> service.getYearlyRevenue(1752));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.getYearlyRevenue(9999));

        // Nam sai thi khong truy van database
        verifyNoInteractions(repository);
    }
}
