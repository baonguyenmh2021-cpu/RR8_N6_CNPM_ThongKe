
package com.example.demo.modules.discount.service;

import com.example.demo.modules.discount.dto.DiscountEligibilityResponse;
import com.example.demo.modules.discount.repository.DiscountManagementRepository;
import com.example.demo.modules.discount.repository.DiscountManagementRepository.DiscountState;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DiscountUS07Test {

    private final DiscountManagementRepository repository = mock(DiscountManagementRepository.class);

    private final DiscountManagementService service = new DiscountManagementService(repository);

    // Co dinh ngay test de ket qua khong thay doi
    private final LocalDate today = LocalDate.of(2026, 11, 22);

    private final LocalDate from = LocalDate.of(2026, 9, 1);

    private final LocalDate to = LocalDate.of(2026, 11, 30);

    // Ham tao du lieu gia lap
    private DiscountState state(
            String status,
            LocalDate validFrom,
            LocalDate validTo,
            Integer usageLimit,
            long uses) {

        return new DiscountState(
                1,
                "THU10",
                status,
                validFrom,
                validTo,
                new BigDecimal("500000"),
                usageLimit,
                uses,
                today);
    }

    // TC07-01: Ma hop le, con han
    @Test
    void testEligibleDiscount() {

        when(repository.findById(1))
                .thenReturn(Optional.of(
                        state("Active", from, to, 100, 2)));

        DiscountEligibilityResponse result = service.checkEligibility(
                1, new BigDecimal("1000000"));

        assertTrue(result.canApply());
        assertEquals("Active", result.effectiveStatus());
        assertEquals(98, result.remainingUses());
        assertEquals("Ma co the ap dung", result.reason());
    }

    // TC07-02: Ma chua den ngay hieu luc
    @Test
    void testNotStartedDiscount() {

        when(repository.findById(1))
                .thenReturn(Optional.of(
                        state(
                                "Active",
                                LocalDate.of(2026, 12, 1),
                                LocalDate.of(2026, 12, 31),
                                100, 0)));

        DiscountEligibilityResponse result = service.checkEligibility(
                1, new BigDecimal("1000000"));

        assertFalse(result.canApply());
        assertEquals(
                "Ma chua den ngay hieu luc",
                result.reason());
    }

    // TC07-03: Ma da het han
    @Test
    void testExpiredDiscount() {

        when(repository.findById(1))
                .thenReturn(Optional.of(
                        state(
                                "Active",
                                from,
                                LocalDate.of(2026, 11, 21),
                                100, 2)));

        DiscountEligibilityResponse result = service.checkEligibility(
                1, new BigDecimal("1000000"));

        assertFalse(result.canApply());
        assertEquals("Expired", result.effectiveStatus());
        assertEquals("Active", result.storedStatus());
        assertEquals("Ma da het han", result.reason());
    }

    // TC07-04: Ma da bi vo hieu hoa
    @Test
    void testDisabledDiscount() {

        when(repository.findById(1))
                .thenReturn(Optional.of(
                        state("Disabled", from, to, 100, 2)));

        DiscountEligibilityResponse result = service.checkEligibility(
                1, new BigDecimal("1000000"));

        assertFalse(result.canApply());
        assertEquals("Disabled", result.effectiveStatus());
        assertEquals(
                "Ma da bi vo hieu hoa",
                result.reason());
    }

    // TC07-05: Ma het luot su dung
    @Test
    void testUsageLimitReached() {

        when(repository.findById(1))
                .thenReturn(Optional.of(
                        state("Active", from, to, 100, 100)));

        DiscountEligibilityResponse result = service.checkEligibility(
                1, new BigDecimal("1000000"));

        assertFalse(result.canApply());
        assertEquals(100L, result.uses());
        assertEquals(0, result.remainingUses());
        assertEquals(
                "Ma da het luot su dung",
                result.reason());
    }

    // TC07-06: Don hang chua dat toi thieu
    @Test
    void testBelowMinimumAmount() {

        when(repository.findById(1))
                .thenReturn(Optional.of(
                        state("Active", from, to, 100, 2)));

        DiscountEligibilityResponse result = service.checkEligibility(
                1, new BigDecimal("300000"));

        assertFalse(result.canApply());

        assertEquals(
                "Don hang chua dat gia tri toi thieu",
                result.reason());
    }

    // TC07-07: Ma khong gioi han luot
    @Test
    void testUnlimitedUsage() {

        when(repository.findById(1))
                .thenReturn(Optional.of(
                        state("Active", from, to, null, 200)));

        DiscountEligibilityResponse result = service.checkEligibility(
                1, new BigDecimal("1000000"));

        assertTrue(result.canApply());
        assertNull(result.usageLimit());
        assertNull(result.remainingUses());
    }

    // TC07-08: Gia tri don hang khong hop le
    @Test
    void testInvalidOrderAmount() {

        assertThrows(
                IllegalArgumentException.class,
                () -> service.checkEligibility(1, null));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.checkEligibility(
                        1, new BigDecimal("-1000")));

        verifyNoInteractions(repository);
    }

    // TC07-09: ID khong hop le
    @Test
    void testInvalidDiscountId() {

        assertThrows(
                IllegalArgumentException.class,
                () -> service.checkEligibility(
                        0, new BigDecimal("1000000")));

        verifyNoInteractions(repository);
    }

    // TC07-10: Khong tim thay ma
    @Test
    void testDiscountNotFound() {

        when(repository.findById(99))
                .thenReturn(Optional.empty());

        assertThrows(
                DiscountManagementService.DiscountNotFoundException.class,
                () -> service.checkEligibility(
                        99, new BigDecimal("1000000")));

        verify(repository).findById(99);
    }

    // TC07-11: Vo hieu hoa thanh cong
    @Test
    void testDisableSuccess() {

        DiscountState before = state("Active", from, to, 100, 2);

        DiscountState after = state("Disabled", from, to, 100, 2);

        when(repository.findById(1))
                .thenReturn(
                        Optional.of(before),
                        Optional.of(after));

        when(repository.disableDiscount(1))
                .thenReturn(true);

        DiscountState result = service.disableDiscount(1);

        assertEquals("Disabled", result.storedStatus());

        verify(repository).disableDiscount(1);
        verify(repository, times(2)).findById(1);
    }

    // TC07-12: Vo hieu hoa ma da Disabled
    @Test
    void testDisableAlreadyDisabled() {

        when(repository.findById(1))
                .thenReturn(Optional.of(
                        state("Disabled", from, to, 100, 2)));

        assertThrows(
                IllegalStateException.class,
                () -> service.disableDiscount(1));

        verify(repository, never()).disableDiscount(1);
    }

    // TC07-13: Trang thai thay doi khi UPDATE
    @Test
    void testDisableUpdateFailed() {

        when(repository.findById(1))
                .thenReturn(Optional.of(
                        state("Active", from, to, 100, 2)));

        when(repository.disableDiscount(1))
                .thenReturn(false);

        assertThrows(
                IllegalStateException.class,
                () -> service.disableDiscount(1));

        verify(repository).disableDiscount(1);
    }

    // TC07-14: Gia han thanh cong
    @Test
    void testExtendSuccess() {

        LocalDate newDate = LocalDate.of(2026, 12, 31);

        // Thu ca ma Disabled: gia han khong tu bat lai
        DiscountState before = state("Disabled", from, to, 100, 2);

        DiscountState after = state("Disabled", from, newDate, 100, 2);

        when(repository.findById(1))
                .thenReturn(
                        Optional.of(before),
                        Optional.of(after));

        when(repository.extendDiscount(1, newDate))
                .thenReturn(true);

        DiscountState result = service.extendDiscount(1, newDate);

        assertEquals(newDate, result.validTo());
        assertEquals("Disabled", result.storedStatus());

        verify(repository).extendDiscount(1, newDate);
        verify(repository, times(2)).findById(1);
    }

    // TC07-15: Ngay gia han khong hop le
    @Test
    void testInvalidExtensionDate() {

        when(repository.findById(1))
                .thenReturn(Optional.of(
                        state("Active", from, to, 100, 2)));

        // Ngay moi bang ngay cu
        assertThrows(
                IllegalArgumentException.class,
                () -> service.extendDiscount(1, to));

        // Ngay moi truoc ngay cu
        assertThrows(
                IllegalArgumentException.class,
                () -> service.extendDiscount(
                        1, LocalDate.of(2026, 11, 15)));

        // Khong nhap ngay
        assertThrows(
                IllegalArgumentException.class,
                () -> service.extendDiscount(1, null));

        verify(repository, never())
                .extendDiscount(anyInt(), any(LocalDate.class));
    }
}
