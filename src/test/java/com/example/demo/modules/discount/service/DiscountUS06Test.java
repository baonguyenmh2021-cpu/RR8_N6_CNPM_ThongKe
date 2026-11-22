
package com.example.demo.modules.discount.service;

import com.example.demo.modules.discount.dto.DiscountEffectivenessResponse;
import com.example.demo.modules.discount.repository.DiscountEffectivenessRepository;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DiscountUS06Test {

    private final DiscountEffectivenessRepository repository = mock(DiscountEffectivenessRepository.class);

    private final DiscountEffectivenessService service = new DiscountEffectivenessService(repository);

    private final LocalDate from = LocalDate.of(2026, 9, 1);

    private final LocalDate to = LocalDate.of(2026, 10, 1);

    // TC06-01: Thong ke toan bo ma giam gia
    @Test
    void testGetAllEffectiveness() {

        DiscountEffectivenessResponse thu10 = new DiscountEffectivenessResponse(
                1,
                "THU10",
                "Active",
                100,
                2L,
                98,
                new BigDecimal("315000"),
                new BigDecimal("1800000"));

        when(repository.findEffectiveness(null, null))
                .thenReturn(List.of(thu10));

        List<DiscountEffectivenessResponse> result = service.getEffectiveness(null, null);

        assertEquals(1, result.size());
        assertEquals("THU10", result.get(0).code());
        assertEquals(2L, result.get(0).uses());
        assertEquals(98, result.get(0).remainingUses());

        assertEquals(
                0,
                new BigDecimal("315000")
                        .compareTo(result.get(0).totalDiscount()));

        verify(repository).findEffectiveness(null, null);
    }

    // TC06-02: Loc theo khoang ngay hop le
    @Test
    void testFilterByDate() {

        when(repository.findEffectiveness(from, to))
                .thenReturn(List.of(
                        new DiscountEffectivenessResponse(
                                1,
                                "THU10",
                                "Active",
                                100,
                                2L,
                                98,
                                new BigDecimal("315000"),
                                new BigDecimal("1800000"))));

        List<DiscountEffectivenessResponse> result = service.getEffectiveness(from, to);

        assertEquals(1, result.size());

        verify(repository, times(1))
                .findEffectiveness(from, to);
    }

    // TC06-03: Chi nhap ngay bat dau
    @Test
    void testOnlyFromDate() {

        assertThrows(
                IllegalArgumentException.class,
                () -> service.getEffectiveness(from, null));

        verifyNoInteractions(repository);
    }

    // TC06-04: Chi nhap ngay ket thuc
    @Test
    void testOnlyToDate() {

        assertThrows(
                IllegalArgumentException.class,
                () -> service.getEffectiveness(null, to));

        verifyNoInteractions(repository);
    }

    // TC06-05: Hai ngay bang nhau
    @Test
    void testSameDates() {

        assertThrows(
                IllegalArgumentException.class,
                () -> service.getEffectiveness(from, from));

        verifyNoInteractions(repository);
    }

    // TC06-06: Ngay ket thuc truoc ngay bat dau
    @Test
    void testEndBeforeStart() {

        assertThrows(
                IllegalArgumentException.class,
                () -> service.getEffectiveness(to, from));

        verifyNoInteractions(repository);
    }

    // TC06-07: Ma chua su dung va khong gioi han luot
    @Test
    void testUnusedDiscount() {

        DiscountEffectivenessResponse unused = new DiscountEffectivenessResponse(
                3,
                "GIAM50K",
                "Active",
                null,
                0L,
                null,
                BigDecimal.ZERO,
                BigDecimal.ZERO);

        when(repository.findEffectiveness(null, null))
                .thenReturn(List.of(unused));

        List<DiscountEffectivenessResponse> result = service.getEffectiveness(null, null);

        assertEquals(1, result.size());

        DiscountEffectivenessResponse item = result.get(0);

        assertEquals(0L, item.uses());
        assertNull(item.usageLimit());
        assertNull(item.remainingUses());

        assertEquals(
                0,
                BigDecimal.ZERO.compareTo(item.totalDiscount()));

        assertEquals(
                0,
                BigDecimal.ZERO.compareTo(item.netRevenue()));

        verify(repository).findEffectiveness(null, null);
    }

    // TC06-08: Repository gap loi database
    @Test
    void testDatabaseFailure() {

        DataAccessResourceFailureException error = new DataAccessResourceFailureException(
                "Khong ket noi duoc SQL Server");

        when(repository.findEffectiveness(from, to))
                .thenThrow(error);

        DataAccessResourceFailureException actual = assertThrows(
                DataAccessResourceFailureException.class,
                () -> service.getEffectiveness(from, to));

        assertSame(error, actual);

        verify(repository).findEffectiveness(from, to);
    }
}
