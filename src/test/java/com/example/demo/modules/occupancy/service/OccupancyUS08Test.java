
package com.example.demo.modules.occupancy.service;

import com.example.demo.modules.occupancy.dto.OccupancyCategoryResponse;
import com.example.demo.modules.occupancy.repository.OccupancyRepository;
import com.example.demo.modules.occupancy.repository.OccupancyRepository.OccupancyMetrics;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OccupancyUS08Test {

    private final OccupancyRepository repository = mock(OccupancyRepository.class);

    private final OccupancyService service = new OccupancyService(repository);

    private final LocalDate from = LocalDate.of(2026, 9, 1);

    private final LocalDate to = LocalDate.of(2026, 10, 1);

    // Ham tao du lieu gia lap de test
    private OccupancyMetrics metric(
            int categoryId,
            String categoryName,
            long availableRooms,
            long periodNights,
            long bookedNights,
            String revenue,
            String minPrice,
            String maxPrice) {

        return new OccupancyMetrics(
                categoryId,
                categoryName,
                availableRooms,
                periodNights,
                bookedNights,
                new BigDecimal(revenue),
                minPrice == null
                        ? null
                        : new BigDecimal(minPrice),
                maxPrice == null
                        ? null
                        : new BigDecimal(maxPrice));
    }

    // TC08-01: Ty le lap day 75%
    @Test
    void testOccupancy75Percent() {

        when(repository.findMetrics(from, to))
                .thenReturn(List.of(
                        metric(
                                1, "Standard",
                                4, 10, 30,
                                "5000000",
                                "400000", "400000")));

        List<OccupancyCategoryResponse> result = service.getOccupancy(from, to);

        assertEquals(1, result.size());

        OccupancyCategoryResponse item = result.get(0);

        assertEquals("Standard", item.categoryName());
        assertEquals(4, item.availableRooms());
        assertEquals(10, item.periodNights());
        assertEquals(30, item.bookedNights());

        assertEquals(
                0,
                new BigDecimal("75.00")
                        .compareTo(item.occupancyRatePercent()));

        assertEquals(
                0,
                new BigDecimal("5000000")
                        .compareTo(item.paidRevenue()));

        verify(repository).findMetrics(from, to);
    }

    // TC08-02: Khong co booking
    @Test
    void testNoBookings() {

        when(repository.findMetrics(from, to))
                .thenReturn(List.of(
                        metric(
                                2, "Deluxe",
                                2, 30, 0,
                                "0",
                                "650000", "650000")));

        OccupancyCategoryResponse result = service.getOccupancy(from, to).get(0);

        assertEquals(0, result.bookedNights());

        assertEquals(
                0,
                BigDecimal.ZERO.compareTo(
                        result.occupancyRatePercent()));
    }

    // TC08-03: Lap day 100%
    @Test
    void testFullOccupancy() {

        when(repository.findMetrics(from, to))
                .thenReturn(List.of(
                        metric(
                                3, "Suite",
                                2, 10, 20,
                                "10000000",
                                "1200000", "1200000")));

        OccupancyCategoryResponse result = service.getOccupancy(from, to).get(0);

        assertEquals(
                0,
                new BigDecimal("100.00")
                        .compareTo(result.occupancyRatePercent()));
    }

    // TC08-04: Khong co phong kha dung
    @Test
    void testNoAvailableRooms() {

        when(repository.findMetrics(from, to))
                .thenReturn(List.of(
                        metric(
                                3, "Suite",
                                0, 30, 0,
                                "0",
                                null, null)));

        OccupancyCategoryResponse result = service.getOccupancy(from, to).get(0);

        assertEquals(0, result.availableRooms());
        assertNull(result.occupancyRatePercent());
        assertNull(result.minCurrentPrice());
        assertNull(result.maxCurrentPrice());
    }

    // TC08-05: Thieu ngay bat dau hoac ket thuc
    @Test
    void testMissingDates() {

        assertThrows(
                IllegalArgumentException.class,
                () -> service.getOccupancy(null, to));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.getOccupancy(from, null));

        verifyNoInteractions(repository);
    }

    // TC08-06: Hai ngay bang nhau
    @Test
    void testSameDates() {

        assertThrows(
                IllegalArgumentException.class,
                () -> service.getOccupancy(from, from));

        verifyNoInteractions(repository);
    }

    // TC08-07: Ngay ket thuc truoc ngay bat dau
    @Test
    void testEndBeforeStart() {

        assertThrows(
                IllegalArgumentException.class,
                () -> service.getOccupancy(to, from));

        verifyNoInteractions(repository);
    }

    // TC08-08: So dem dat vuot cong suat
    @Test
    void testBookedNightsExceedCapacity() {

        // 2 phong x 10 dem = 20 dem toi da
        // Du lieu gia lap co 25 dem -> khong hop le
        when(repository.findMetrics(from, to))
                .thenReturn(List.of(
                        metric(
                                2, "Deluxe",
                                2, 10, 25,
                                "5000000",
                                "650000", "650000")));

        assertThrows(
                IllegalStateException.class,
                () -> service.getOccupancy(from, to));

        verify(repository).findMetrics(from, to);
    }

    // TC08-09: So dem dat am
    @Test
    void testNegativeBookedNights() {

        when(repository.findMetrics(from, to))
                .thenReturn(List.of(
                        metric(
                                1, "Standard",
                                2, 10, -1,
                                "0",
                                "400000", "400000")));

        assertThrows(
                IllegalStateException.class,
                () -> service.getOccupancy(from, to));
    }

    // TC08-10: Thong ke nhieu loai phong
    @Test
    void testMultipleCategories() {

        when(repository.findMetrics(from, to))
                .thenReturn(List.of(
                        metric(
                                1, "Standard",
                                2, 10, 10,
                                "2000000",
                                "400000", "400000"),
                        metric(
                                2, "Deluxe",
                                4, 10, 30,
                                "8000000",
                                "650000", "700000")));

        List<OccupancyCategoryResponse> result = service.getOccupancy(from, to);

        assertEquals(2, result.size());

        assertEquals("Standard",
                result.get(0).categoryName());

        assertEquals("Deluxe",
                result.get(1).categoryName());

        // Standard: 10 / (2 * 10) = 50%
        assertEquals(
                0,
                new BigDecimal("50.00").compareTo(
                        result.get(0).occupancyRatePercent()));

        // Deluxe: 30 / (4 * 10) = 75%
        assertEquals(
                0,
                new BigDecimal("75.00").compareTo(
                        result.get(1).occupancyRatePercent()));

        assertEquals(
                0,
                new BigDecimal("700000").compareTo(
                        result.get(1).maxCurrentPrice()));
    }

    // TC08-11: Repository gap loi database
    @Test
    void testDatabaseFailure() {

        DataAccessResourceFailureException error = new DataAccessResourceFailureException(
                "Khong ket noi duoc SQL Server");

        when(repository.findMetrics(from, to))
                .thenThrow(error);

        DataAccessResourceFailureException actual = assertThrows(
                DataAccessResourceFailureException.class,
                () -> service.getOccupancy(from, to));

        assertSame(error, actual);

        verify(repository).findMetrics(from, to);
    }
}
