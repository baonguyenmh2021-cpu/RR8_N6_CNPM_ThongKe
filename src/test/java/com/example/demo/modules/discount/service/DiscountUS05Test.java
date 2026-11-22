
package com.example.demo.modules.discount.service;

import com.example.demo.modules.discount.dto.CreateDiscountRequest;
import com.example.demo.modules.discount.dto.DiscountResponse;
import com.example.demo.modules.discount.repository.DiscountRepository;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DiscountUS05Test {

    private final DiscountRepository repository = mock(DiscountRepository.class);

    private final DiscountService service = new DiscountService(repository);

    private final LocalDate from = LocalDate.of(2026, 12, 1);

    private final LocalDate to = LocalDate.of(2026, 12, 31);

    // Tao du lieu test cho de su dung
    private CreateDiscountRequest request(
            String code,
            String type,
            String value,
            BigDecimal minAmount,
            LocalDate validFrom,
            LocalDate validTo,
            Integer usageLimit) {

        return new CreateDiscountRequest(
                code,
                "Uu dai test",
                type,
                new BigDecimal(value),
                minAmount,
                validFrom,
                validTo,
                usageLimit);
    }

    // TC05-01: Tao ma giam gia theo phan tram
    @Test
    void testCreatePercentSuccess() {

        CreateDiscountRequest req = request(
                "GIAM20", "PERCENT", "20",
                new BigDecimal("500000"),
                from, to, 100);

        when(repository.insertDiscount(req, 2))
                .thenReturn(3);

        when(repository.findById(3))
                .thenReturn(Optional.of(
                        new DiscountResponse(
                                3, "GIAM20", "Uu dai test",
                                "PERCENT", new BigDecimal("20"),
                                new BigDecimal("500000"),
                                from, to, 100,
                                "Active", 2,
                                LocalDateTime.of(2026, 11, 18, 9, 0))));

        DiscountResponse result = service.createDiscount(req, 2);

        assertEquals(3, result.discountId());
        assertEquals("GIAM20", result.code());
        assertEquals("PERCENT", result.type());
        assertEquals("Active", result.status());

        verify(repository).insertDiscount(req, 2);
    }

    // TC05-02: Tao ma giam gia so tien co dinh
    @Test
    void testCreateFixedSuccess() {

        CreateDiscountRequest req = request(
                "GIAM50K", "FIXED", "50000",
                BigDecimal.ZERO, from, to, null);

        when(repository.insertDiscount(req, 2))
                .thenReturn(4);

        when(repository.findById(4))
                .thenReturn(Optional.of(
                        new DiscountResponse(
                                4, "GIAM50K", "Uu dai test",
                                "FIXED", new BigDecimal("50000"),
                                BigDecimal.ZERO,
                                from, to, null,
                                "Active", 2,
                                LocalDateTime.of(2026, 11, 18, 9, 0))));

        DiscountResponse result = service.createDiscount(req, 2);

        assertEquals("FIXED", result.type());
        assertEquals(
                0,
                new BigDecimal("50000")
                        .compareTo(result.value()));

        assertNull(result.usageLimit());
    }

    // TC05-03: Ma giam gia bi trung
    @Test
    void testDuplicateCode() {

        CreateDiscountRequest req = request(
                "GIAM20", "PERCENT", "20",
                BigDecimal.ZERO, from, to, 100);

        when(repository.existsByCode("GIAM20"))
                .thenReturn(true);

        assertThrows(
                DiscountService.DuplicateDiscountCodeException.class,
                () -> service.createDiscount(req, 2));

        verify(repository, never())
                .insertDiscount(any(), anyInt());
    }

    // TC05-04: Ma, loai hoac gia tri khong hop le
    @Test
    void testInvalidBasicData() {

        assertThrows(
                IllegalArgumentException.class,
                () -> service.createDiscount(
                        request("GIAM-20", "PERCENT", "20",
                                BigDecimal.ZERO, from, to, 100),
                        2));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.createDiscount(
                        request("GIAM20", "OTHER", "20",
                                BigDecimal.ZERO, from, to, 100),
                        2));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.createDiscount(
                        request("GIAM20", "FIXED", "0",
                                BigDecimal.ZERO, from, to, 100),
                        2));

        verifyNoInteractions(repository);
    }

    // TC05-05: Phan tram giam vuot 100
    @Test
    void testPercentAbove100() {

        CreateDiscountRequest req = request(
                "GIAM120", "PERCENT", "120",
                BigDecimal.ZERO, from, to, 100);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.createDiscount(req, 2));

        verifyNoInteractions(repository);
    }

    // TC05-06: Ngay ket thuc truoc ngay bat dau
    @Test
    void testInvalidDates() {

        CreateDiscountRequest req = request(
                "GIAM20", "PERCENT", "20",
                BigDecimal.ZERO,
                LocalDate.of(2026, 12, 31),
                LocalDate.of(2026, 12, 1),
                100);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.createDiscount(req, 2));

        verifyNoInteractions(repository);
    }

    // TC05-07: Don toi thieu va gioi han luot dung sai
    @Test
    void testInvalidLimits() {

        assertThrows(
                IllegalArgumentException.class,
                () -> service.createDiscount(
                        request("GIAM20", "PERCENT", "20",
                                new BigDecimal("-1000"),
                                from, to, 100),
                        2));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.createDiscount(
                        request("GIAM20", "PERCENT", "20",
                                BigDecimal.ZERO,
                                from, to, 0),
                        2));

        verifyNoInteractions(repository);
    }

    // TC05-08: ID nguoi tao khong hop le
    @Test
    void testInvalidManagerId() {

        CreateDiscountRequest req = request(
                "GIAM20", "PERCENT", "20",
                BigDecimal.ZERO, from, to, 100);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.createDiscount(req, 0));

        verifyNoInteractions(repository);
    }

    // TC05-09: Ma bi trung trong luc INSERT
    @Test
    void testDuplicateDuringInsert() {

        CreateDiscountRequest req = request(
                "GIAM20", "PERCENT", "20",
                BigDecimal.ZERO, from, to, 100);

        // Lan dau chua trung, lan hai da ton tai
        when(repository.existsByCode("GIAM20"))
                .thenReturn(false, true);

        when(repository.insertDiscount(req, 2))
                .thenThrow(
                        new DataIntegrityViolationException(
                                "Duplicate key"));

        assertThrows(
                DiscountService.DuplicateDiscountCodeException.class,
                () -> service.createDiscount(req, 2));

        verify(repository, times(2))
                .existsByCode("GIAM20");
    }
}
