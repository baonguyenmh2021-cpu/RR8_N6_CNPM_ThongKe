
package com.example.demo.modules.marketing.service;

import com.example.demo.modules.marketing.dto.MarketingEffectivenessResponse;
import com.example.demo.modules.marketing.repository.MarketingEffectivenessRepository;
import com.example.demo.modules.marketing.repository.MarketingEffectivenessRepository.MarketingMetrics;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MarketingUS12Test {

    private final MarketingEffectivenessRepository repository = mock(MarketingEffectivenessRepository.class);

    private final MarketingEffectivenessService service = new MarketingEffectivenessService(repository);

    private final LocalDate start = LocalDate.of(2026, 9, 1);

    private final LocalDate end = LocalDate.of(2026, 11, 30);

    // Ham tao du lieu gia lap
    private MarketingMetrics metrics(
            String revenueBefore,
            String revenueDuring,
            long bookingsBefore,
            long bookingsDuring,
            Integer expectedBookings,
            Integer discountId,
            long bookingsWithCode) {

        return new MarketingMetrics(
                1,
                "Uu dai mua thu",
                "Running",
                start,
                end,
                new BigDecimal("5000000"),
                new BigDecimal("1000000"),
                discountId,
                expectedBookings,
                new BigDecimal(revenueBefore),
                new BigDecimal(revenueDuring),
                bookingsBefore,
                bookingsDuring,
                bookingsWithCode);
    }

    // Gia lap Repository tra ve thong ke
    private void mockMetrics(MarketingMetrics data) {

        when(repository.findMetricsByStrategyId(1))
                .thenReturn(Optional.of(data));
    }

    // TC12-01: Doanh thu tang 25%, dat 60% booking
    @Test
    void testRevenueIncrease25Percent() {

        mockMetrics(metrics(
                "8000000",
                "10000000",
                9,
                12,
                20,
                1,
                5));

        MarketingEffectivenessResponse result = service.getEffectiveness(1);

        assertEquals(1, result.strategyId());
        assertEquals("Uu dai mua thu", result.title());

        assertEquals(0,
                new BigDecimal("25.00").compareTo(
                        result.revenueChangePercent()));

        assertEquals(0,
                new BigDecimal("60.00").compareTo(
                        result.bookingAchievementPercent()));

        assertEquals(9, result.bookingsBefore());
        assertEquals(12, result.bookingsDuring());
        assertEquals(5, result.bookingsWithCode());

        verify(repository).findMetricsByStrategyId(1);
    }

    // TC12-02: Doanh thu giam 25%
    @Test
    void testRevenueDecrease25Percent() {

        mockMetrics(metrics(
                "8000000",
                "6000000",
                10,
                8,
                20,
                1,
                3));

        MarketingEffectivenessResponse result = service.getEffectiveness(1);

        assertEquals(0,
                new BigDecimal("-25.00").compareTo(
                        result.revenueChangePercent()));
    }

    // TC12-03: Doanh thu khong thay doi
    @Test
    void testRevenueUnchanged() {

        mockMetrics(metrics(
                "5000000",
                "5000000",
                8,
                8,
                20,
                1,
                2));

        MarketingEffectivenessResponse result = service.getEffectiveness(1);

        assertEquals(0,
                BigDecimal.ZERO.compareTo(
                        result.revenueChangePercent()));
    }

    // TC12-04: Doanh thu truoc bang 0
    @Test
    void testRevenueBeforeZero() {

        mockMetrics(metrics(
                "0",
                "5000000",
                0,
                7,
                20,
                1,
                2));

        MarketingEffectivenessResponse result = service.getEffectiveness(1);

        assertNull(result.revenueChangePercent());

        assertEquals(0,
                BigDecimal.ZERO.compareTo(
                        result.revenueBefore()));

        assertEquals(0,
                new BigDecimal("5000000").compareTo(
                        result.revenueDuring()));
    }

    // TC12-05: Khong khai bao booking ky vong
    @Test
    void testExpectedBookingsNull() {

        mockMetrics(metrics(
                "1000000",
                "2000000",
                2,
                5,
                null,
                1,
                2));

        MarketingEffectivenessResponse result = service.getEffectiveness(1);

        assertNull(result.expectedBookings());
        assertNull(result.bookingAchievementPercent());
    }

    // TC12-06: Booking ky vong bang 0
    @Test
    void testExpectedBookingsZero() {

        mockMetrics(metrics(
                "1000000",
                "2000000",
                2,
                5,
                0,
                1,
                2));

        MarketingEffectivenessResponse result = service.getEffectiveness(1);

        assertEquals(0, result.expectedBookings());
        assertNull(result.bookingAchievementPercent());
    }

    // TC12-07: Chien luoc khong co discount
    @Test
    void testStrategyWithoutDiscount() {

        mockMetrics(metrics(
                "2000000",
                "3000000",
                4,
                6,
                10,
                null,
                0));

        MarketingEffectivenessResponse result = service.getEffectiveness(1);

        assertNull(result.discountId());
        assertEquals(0, result.bookingsWithCode());

        assertEquals(0,
                new BigDecimal("60.00").compareTo(
                        result.bookingAchievementPercent()));
    }

    // TC12-08: Lam tron phan tram
    @Test
    void testPercentageRounding() {

        mockMetrics(metrics(
                "3",
                "4",
                1,
                1,
                3,
                null,
                0));

        MarketingEffectivenessResponse result = service.getEffectiveness(1);

        // (4 - 3) / 3 * 100 = 33.333...
        assertEquals(0,
                new BigDecimal("33.33").compareTo(
                        result.revenueChangePercent()));

        // 1 / 3 * 100 = 33.333...
        assertEquals(0,
                new BigDecimal("33.33").compareTo(
                        result.bookingAchievementPercent()));
    }

    // TC12-09: ID chien luoc khong hop le
    @Test
    void testInvalidStrategyId() {

        assertThrows(
                IllegalArgumentException.class,
                () -> service.getEffectiveness(0));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.getEffectiveness(-1));

        verifyNoInteractions(repository);
    }

    // TC12-10: Khong tim thay chien luoc
    @Test
    void testStrategyNotFound() {

        when(repository.findMetricsByStrategyId(999))
                .thenReturn(Optional.empty());

        assertThrows(
                MarketingEffectivenessService.MarketingStrategyNotFoundException.class,
                () -> service.getEffectiveness(999));

        verify(repository).findMetricsByStrategyId(999);
    }

    // TC12-11: Booking dung ma vuot tong booking
    @Test
    void testCodeBookingsExceedTotal() {

        // Tong booking = 5, booking dung ma = 7
        mockMetrics(metrics(
                "1000000",
                "2000000",
                2,
                5,
                10,
                1,
                7));

        assertThrows(
                IllegalStateException.class,
                () -> service.getEffectiveness(1));
    }

    // TC12-12: Khong co discount nhung co booking dung ma
    @Test
    void testBookingsWithCodeWithoutDiscount() {

        mockMetrics(metrics(
                "1000000",
                "2000000",
                2,
                5,
                10,
                null,
                2));

        assertThrows(
                IllegalStateException.class,
                () -> service.getEffectiveness(1));
    }

    // TC12-13: Khoang thoi gian khong hop le
    @Test
    void testInvalidStrategyDates() {

        MarketingMetrics invalid = new MarketingMetrics(
                1,
                "Chien luoc sai ngay",
                "Running",
                LocalDate.of(2026, 12, 31),
                LocalDate.of(2026, 12, 1),
                new BigDecimal("5000000"),
                BigDecimal.ZERO,
                null,
                10,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                0,
                0,
                0);

        mockMetrics(invalid);

        assertThrows(
                IllegalStateException.class,
                () -> service.getEffectiveness(1));
    }

    // TC12-14: Doanh thu hoac booking am
    @Test
    void testNegativeRevenueOrBookings() {

        MarketingMetrics negativeRevenue = metrics(
                "-100",
                "2000000",
                2,
                5,
                10,
                1,
                2);

        MarketingMetrics negativeBookings = metrics(
                "1000000",
                "2000000",
                -1,
                5,
                10,
                1,
                2);

        MarketingMetrics negativeCodeBookings = metrics(
                "1000000",
                "2000000",
                2,
                5,
                10,
                1,
                -1);

        when(repository.findMetricsByStrategyId(1))
                .thenReturn(
                        Optional.of(negativeRevenue),
                        Optional.of(negativeBookings),
                        Optional.of(negativeCodeBookings));

        for (int i = 0; i < 3; i++) {
            assertThrows(
                    IllegalStateException.class,
                    () -> service.getEffectiveness(1));
        }
    }

    // TC12-15: Ngan sach hoac chi phi am
    @Test
    void testInvalidBudgetAndActualSpend() {

        MarketingMetrics negativeBudget = new MarketingMetrics(
                1,
                "Chien luoc A",
                "Running",
                start,
                end,
                new BigDecimal("-1000"),
                BigDecimal.ZERO,
                null,
                10,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                0,
                0,
                0);

        MarketingMetrics negativeSpend = new MarketingMetrics(
                1,
                "Chien luoc B",
                "Running",
                start,
                end,
                new BigDecimal("5000000"),
                new BigDecimal("-1000"),
                null,
                10,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                0,
                0,
                0);

        when(repository.findMetricsByStrategyId(1))
                .thenReturn(
                        Optional.of(negativeBudget),
                        Optional.of(negativeSpend));

        assertThrows(
                IllegalStateException.class,
                () -> service.getEffectiveness(1));

        assertThrows(
                IllegalStateException.class,
                () -> service.getEffectiveness(1));
    }

    // TC12-16: Repository gap loi database
    @Test
    void testDatabaseFailure() {

        DataAccessResourceFailureException error = new DataAccessResourceFailureException(
                "Khong ket noi duoc SQL Server");

        when(repository.findMetricsByStrategyId(1))
                .thenThrow(error);

        DataAccessResourceFailureException actual = assertThrows(
                DataAccessResourceFailureException.class,
                () -> service.getEffectiveness(1));

        assertSame(error, actual);

        verify(repository).findMetricsByStrategyId(1);
    }
}
