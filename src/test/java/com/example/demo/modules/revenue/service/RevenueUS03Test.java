
package com.example.demo.modules.revenue.service;

import com.example.demo.modules.revenue.dto.CategoryRevenueResponse;
import com.example.demo.modules.revenue.dto.CategoryRevenueResponse.CategoryRevenueItem;
import com.example.demo.modules.revenue.repository.RevenueRepository;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RevenueUS03Test {

    private final RevenueRepository repository = mock(RevenueRepository.class);

    private final RevenueService service = new RevenueService(repository);

    // TC01: Thong ke doanh thu theo thang
    @Test
    void testMonthlyCategoryRevenue() {

        LocalDate from = LocalDate.of(2026, 9, 1);
        LocalDate to = LocalDate.of(2026, 10, 1);

        when(repository.getCategoryRevenues(from, to))
                .thenReturn(List.of(
                        new CategoryRevenueItem(
                                1, "Phong don",
                                new BigDecimal("5000000"), 1),

                        new CategoryRevenueItem(
                                2, "Phong doi",
                                new BigDecimal("6155000"), 1),

                        new CategoryRevenueItem(
                                3, "Phong VIP",
                                BigDecimal.ZERO, 0)));

        CategoryRevenueResponse result = service.getCategoryRevenue(2026, 9);

        assertEquals(2026, result.year());
        assertEquals(9, result.month());

        assertEquals(3, result.categories().size());

        assertEquals(0,
                new BigDecimal("11155000")
                        .compareTo(result.totalRevenue()));

        assertEquals(1,
                result.categories().get(0).bookingCount());

        assertEquals(0,
                result.categories().get(2).bookingCount());

        verify(repository).getCategoryRevenues(from, to);
    }

    // TC02: Thong ke doanh thu ca nam
    @Test
    void testYearlyCategoryRevenue() {

        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2027, 1, 1);

        when(repository.getCategoryRevenues(from, to))
                .thenReturn(List.of(
                        new CategoryRevenueItem(
                                1, "Phong don",
                                new BigDecimal("8000000"), 3),

                        new CategoryRevenueItem(
                                2, "Phong doi",
                                new BigDecimal("5435000"), 2)));

        CategoryRevenueResponse result = service.getCategoryRevenue(2026, null);

        assertNull(result.month());

        assertEquals(0,
                new BigDecimal("13435000")
                        .compareTo(result.totalRevenue()));

        assertEquals(2, result.categories().size());

        verify(repository).getCategoryRevenues(from, to);
    }

    // TC03: Loai phong khong co doanh thu
    @Test
    void testCategoryWithoutRevenue() {

        LocalDate from = LocalDate.of(2026, 6, 1);
        LocalDate to = LocalDate.of(2026, 7, 1);

        when(repository.getCategoryRevenues(from, to))
                .thenReturn(List.of(
                        new CategoryRevenueItem(
                                1, "Phong don",
                                BigDecimal.ZERO, 0),

                        new CategoryRevenueItem(
                                2, "Phong VIP",
                                BigDecimal.ZERO, 0)));

        CategoryRevenueResponse result = service.getCategoryRevenue(2026, 6);

        assertEquals(2, result.categories().size());

        assertEquals(0,
                BigDecimal.ZERO.compareTo(
                        result.totalRevenue()));

        for (CategoryRevenueItem item : result.categories()) {
            assertEquals(0,
                    BigDecimal.ZERO.compareTo(item.revenue()));

            assertEquals(0, item.bookingCount());
        }
    }

    // TC04: Thang khong hop le
    @Test
    void testInvalidMonth() {

        assertThrows(
                IllegalArgumentException.class,
                () -> service.getCategoryRevenue(2026, 0));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.getCategoryRevenue(2026, 13));

        verifyNoInteractions(repository);
    }

    // TC05: Nam khong hop le
    @Test
    void testInvalidYear() {

        assertThrows(
                IllegalArgumentException.class,
                () -> service.getCategoryRevenue(1752, 9));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.getCategoryRevenue(9999, null));

        verifyNoInteractions(repository);
    }

    // TC06: Kiem tra thang 2 cua nam nhuan
    @Test
    void testLeapYear() {

        LocalDate from = LocalDate.of(2024, 2, 1);
        LocalDate to = LocalDate.of(2024, 3, 1);

        when(repository.getCategoryRevenues(from, to))
                .thenReturn(List.of());

        CategoryRevenueResponse result = service.getCategoryRevenue(2024, 2);

        assertEquals(0,
                BigDecimal.ZERO.compareTo(
                        result.totalRevenue()));

        verify(repository).getCategoryRevenues(from, to);
    }

    // TC07: Database gap loi
    @Test
    void testDatabaseFailure() {

        LocalDate from = LocalDate.of(2026, 9, 1);
        LocalDate to = LocalDate.of(2026, 10, 1);

        when(repository.getCategoryRevenues(from, to))
                .thenThrow(
                        new DataAccessResourceFailureException(
                                "Database unavailable"));

        assertThrows(
                DataAccessResourceFailureException.class,
                () -> service.getCategoryRevenue(2026, 9));
    }
}
