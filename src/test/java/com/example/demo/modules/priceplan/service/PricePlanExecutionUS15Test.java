
package com.example.demo.modules.priceplan.service;

import com.example.demo.modules.priceplan.repository.PricePlanExecutionRepository;
import com.example.demo.modules.priceplan.repository.PricePlanExecutionRepository.PricePlanSnapshot;
import com.example.demo.modules.priceplan.repository.PricePlanExecutionRepository.RoomPriceSnapshot;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.dao.DataAccessResourceFailureException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PricePlanExecutionUS15Test {

    @Mock
    private PricePlanExecutionRepository repository;

    @InjectMocks
    private PricePlanExecutionService service;

    private static final int PLAN_ID = 7;
    private static final int CATEGORY_ID = 2;
    private static final int ADMIN_ID = 1;

    private static final BigDecimal OLD_PRICE = new BigDecimal("400000.00");

    private static final BigDecimal NEW_PRICE = new BigDecimal("360000.00");

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 23);

    private static final LocalDate FROM = LocalDate.of(2026, 11, 1);

    private static final LocalDate TO = LocalDate.of(2026, 11, 30);

    private static final LocalDateTime APPLIED_AT = LocalDateTime.of(2026, 11, 23, 10, 30);

    // =========================================
    // DU LIEU GIA LAP
    // =========================================

    private PricePlanSnapshot plan(String status) {

        return new PricePlanSnapshot(
                PLAN_ID,
                CATEGORY_ID,
                OLD_PRICE,
                NEW_PRICE,
                FROM,
                TO,
                status,
                "Applied".equals(status)
                        || "Ended".equals(status)
                                ? APPLIED_AT
                                : null);
    }

    private RoomPriceSnapshot rooms(
            int count,
            BigDecimal min,
            BigDecimal max) {

        return new RoomPriceSnapshot(
                count, min, max);
    }

    private void mockAdmin() {

        when(repository.findUserRole(ADMIN_ID))
                .thenReturn(Optional.of("Admin"));
    }

    private void mockPlan(String status) {

        when(repository.lockPlanById(PLAN_ID))
                .thenReturn(Optional.of(plan(status)));
    }

    // Chuan bi cac dieu kien truoc khi doc gia phong
    private void prepareApply() {

        mockAdmin();
        mockPlan("Approved");

        when(repository.getDatabaseToday())
                .thenReturn(TODAY);

        when(repository.lockCategory(CATEGORY_ID))
                .thenReturn(true);

        when(repository.hasOtherAppliedPlan(
                CATEGORY_ID, PLAN_ID))
                .thenReturn(false);
    }

    private void prepareEnd() {

        mockAdmin();
        mockPlan("Applied");

        when(repository.lockCategory(CATEGORY_ID))
                .thenReturn(true);

        when(repository.hasOtherAppliedPlan(
                CATEGORY_ID, PLAN_ID))
                .thenReturn(false);
    }

    // =========================================
    // NHOM A: AP DUNG KE HOACH GIA
    // =========================================

    // TC15-01: Apply thanh cong
    @Test
    void testApplySuccessfully() {

        prepareApply();

        when(repository.lockRoomPrices(CATEGORY_ID))
                .thenReturn(rooms(
                        2, OLD_PRICE, OLD_PRICE));

        when(repository.updateRoomPrices(
                CATEGORY_ID,
                OLD_PRICE,
                NEW_PRICE)).thenReturn(2);

        when(repository.markApplied(PLAN_ID))
                .thenReturn(1);

        when(repository.findById(PLAN_ID))
                .thenReturn(Optional.of(
                        plan("Applied")));

        PricePlanSnapshot result = service.applyPlan(
                PLAN_ID, ADMIN_ID);

        assertEquals("Applied", result.status());
        assertNotNull(result.appliedAt());

        verify(repository).updateRoomPrices(
                CATEGORY_ID,
                OLD_PRICE,
                NEW_PRICE);

        verify(repository).markApplied(PLAN_ID);
    }

    // TC15-02: Chi Admin duoc thao tac
    @Test
    void testNonAdminForbidden() {

        when(repository.findUserRole(ADMIN_ID))
                .thenReturn(Optional.of("Manager"));

        assertThrows(
                PricePlanExecutionService.PricePlanExecutionForbiddenException.class,
                () -> service.applyPlan(
                        PLAN_ID, ADMIN_ID));

        assertThrows(
                PricePlanExecutionService.PricePlanExecutionForbiddenException.class,
                () -> service.endPlan(
                        PLAN_ID, ADMIN_ID));

        verify(repository, never())
                .lockPlanById(anyInt());
    }

    // TC15-03: Khong tim thay Admin
    @Test
    void testAdminNotFound() {

        when(repository.findUserRole(ADMIN_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                PricePlanExecutionService.PricePlanExecutionForbiddenException.class,
                () -> service.applyPlan(
                        PLAN_ID, ADMIN_ID));

        verify(repository, never())
                .lockPlanById(anyInt());
    }

    // TC15-04: ID khong hop le
    @Test
    void testInvalidIds() {

        assertThrows(
                IllegalArgumentException.class,
                () -> service.applyPlan(0, ADMIN_ID));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.endPlan(-1, ADMIN_ID));

        assertThrows(
                PricePlanExecutionService.PricePlanExecutionForbiddenException.class,
                () -> service.applyPlan(PLAN_ID, 0));

        verifyNoInteractions(repository);
    }

    // TC15-05: Khong tim thay ke hoach
    @Test
    void testPlanNotFound() {

        mockAdmin();

        when(repository.lockPlanById(PLAN_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                PricePlanExecutionService.PricePlanExecutionNotFoundException.class,
                () -> service.applyPlan(
                        PLAN_ID, ADMIN_ID));
    }

    // TC15-06: Apply sai trang thai
    @ParameterizedTest
    @ValueSource(strings = {
            "Draft",
            "Rejected",
            "Applied",
            "Ended"
    })
    void testCannotApplyWrongStatus(String status) {

        mockAdmin();
        mockPlan(status);

        assertThrows(
                PricePlanExecutionService.PricePlanExecutionConflictException.class,
                () -> service.applyPlan(
                        PLAN_ID, ADMIN_ID));

        verify(repository, never())
                .updateRoomPrices(
                        anyInt(), any(), any());
    }

    // TC15-07: Chua den ngay hieu luc
    @Test
    void testApplyBeforeEffectiveDate() {

        mockAdmin();
        mockPlan("Approved");

        when(repository.getDatabaseToday())
                .thenReturn(FROM.minusDays(1));

        assertThrows(
                PricePlanExecutionService.PricePlanExecutionConflictException.class,
                () -> service.applyPlan(
                        PLAN_ID, ADMIN_ID));

        verify(repository, never())
                .lockCategory(anyInt());
    }

    // TC15-08: Da qua ngay hieu luc
    @Test
    void testApplyAfterExpiry() {

        mockAdmin();
        mockPlan("Approved");

        when(repository.getDatabaseToday())
                .thenReturn(TO.plusDays(1));

        assertThrows(
                PricePlanExecutionService.PricePlanExecutionConflictException.class,
                () -> service.applyPlan(
                        PLAN_ID, ADMIN_ID));

        verify(repository, never())
                .updateRoomPrices(
                        anyInt(), any(), any());
    }

    // TC15-09: effective_to = NULL
    @Test
    void testApplyWithoutEndDate() {

        mockAdmin();

        PricePlanSnapshot openEnded = new PricePlanSnapshot(
                PLAN_ID,
                CATEGORY_ID,
                OLD_PRICE,
                NEW_PRICE,
                FROM,
                null,
                "Approved",
                null);

        when(repository.lockPlanById(PLAN_ID))
                .thenReturn(Optional.of(openEnded));

        when(repository.getDatabaseToday())
                .thenReturn(TODAY);

        when(repository.lockCategory(CATEGORY_ID))
                .thenReturn(true);

        when(repository.hasOtherAppliedPlan(
                CATEGORY_ID, PLAN_ID))
                .thenReturn(false);

        when(repository.lockRoomPrices(CATEGORY_ID))
                .thenReturn(rooms(
                        2, OLD_PRICE, OLD_PRICE));

        when(repository.updateRoomPrices(
                CATEGORY_ID, OLD_PRICE, NEW_PRICE))
                .thenReturn(2);

        when(repository.markApplied(PLAN_ID))
                .thenReturn(1);

        when(repository.findById(PLAN_ID))
                .thenReturn(Optional.of(
                        plan("Applied")));

        PricePlanSnapshot result = service.applyPlan(
                PLAN_ID, ADMIN_ID);

        assertEquals("Applied", result.status());
    }

    // TC15-10: Loai phong khong ton tai
    @Test
    void testCategoryNotFound() {

        mockAdmin();
        mockPlan("Approved");

        when(repository.getDatabaseToday())
                .thenReturn(TODAY);

        when(repository.lockCategory(CATEGORY_ID))
                .thenReturn(false);

        assertThrows(
                IllegalStateException.class,
                () -> service.applyPlan(
                        PLAN_ID, ADMIN_ID));

        verify(repository, never())
                .updateRoomPrices(
                        anyInt(), any(), any());
    }

    // TC15-11: Co ke hoach Applied khac
    @Test
    void testOtherAppliedPlanConflict() {

        mockAdmin();
        mockPlan("Approved");

        when(repository.getDatabaseToday())
                .thenReturn(TODAY);

        when(repository.lockCategory(CATEGORY_ID))
                .thenReturn(true);

        when(repository.hasOtherAppliedPlan(
                CATEGORY_ID, PLAN_ID))
                .thenReturn(true);

        assertThrows(
                PricePlanExecutionService.PricePlanExecutionConflictException.class,
                () -> service.applyPlan(
                        PLAN_ID, ADMIN_ID));

        verify(repository, never())
                .updateRoomPrices(
                        anyInt(), any(), any());
    }

    // TC15-12: Loai phong khong co phong
    @Test
    void testNoRooms() {

        prepareApply();

        when(repository.lockRoomPrices(CATEGORY_ID))
                .thenReturn(rooms(
                        0, null, null));

        assertThrows(
                PricePlanExecutionService.PricePlanExecutionConflictException.class,
                () -> service.applyPlan(
                        PLAN_ID, ADMIN_ID));

        verify(repository, never())
                .updateRoomPrices(
                        anyInt(), any(), any());
    }

    // TC15-13: Cac phong khong dong gia
    @Test
    void testDifferentRoomPrices() {

        prepareApply();

        when(repository.lockRoomPrices(CATEGORY_ID))
                .thenReturn(rooms(
                        2,
                        OLD_PRICE,
                        new BigDecimal("420000.00")));

        assertThrows(
                PricePlanExecutionService.PricePlanExecutionConflictException.class,
                () -> service.applyPlan(
                        PLAN_ID, ADMIN_ID));

        verify(repository, never())
                .updateRoomPrices(
                        anyInt(), any(), any());
    }

    // TC15-14: Gia hien tai khac old_price
    @Test
    void testCurrentPriceMismatch() {

        prepareApply();

        BigDecimal currentPrice = new BigDecimal("450000.00");

        when(repository.lockRoomPrices(CATEGORY_ID))
                .thenReturn(rooms(
                        2, currentPrice, currentPrice));

        assertThrows(
                PricePlanExecutionService.PricePlanExecutionConflictException.class,
                () -> service.applyPlan(
                        PLAN_ID, ADMIN_ID));

        verify(repository, never())
                .markApplied(anyInt());
    }

    // TC15-15: UPDATE khong du so phong
    @Test
    void testApplyPartialRoomUpdate() {

        prepareApply();

        when(repository.lockRoomPrices(CATEGORY_ID))
                .thenReturn(rooms(
                        3, OLD_PRICE, OLD_PRICE));

        when(repository.updateRoomPrices(
                CATEGORY_ID,
                OLD_PRICE,
                NEW_PRICE)).thenReturn(2);

        assertThrows(
                PricePlanExecutionService.PricePlanExecutionConflictException.class,
                () -> service.applyPlan(
                        PLAN_ID, ADMIN_ID));

        verify(repository, never())
                .markApplied(anyInt());
    }

    // TC15-16: Khong chuyen duoc sang Applied
    @Test
    void testMarkAppliedFails() {

        prepareApply();

        when(repository.lockRoomPrices(CATEGORY_ID))
                .thenReturn(rooms(
                        2, OLD_PRICE, OLD_PRICE));

        when(repository.updateRoomPrices(
                CATEGORY_ID,
                OLD_PRICE,
                NEW_PRICE)).thenReturn(2);

        when(repository.markApplied(PLAN_ID))
                .thenReturn(0);

        assertThrows(
                PricePlanExecutionService.PricePlanExecutionConflictException.class,
                () -> service.applyPlan(
                        PLAN_ID, ADMIN_ID));

        verify(repository).markApplied(PLAN_ID);

        verify(repository, never())
                .findById(anyInt());
    }

    // TC15-17: Khong doc duoc sau Apply
    @Test
    void testApplyReadBackMissing() {

        prepareApply();

        when(repository.lockRoomPrices(CATEGORY_ID))
                .thenReturn(rooms(
                        2, OLD_PRICE, OLD_PRICE));

        when(repository.updateRoomPrices(
                CATEGORY_ID,
                OLD_PRICE,
                NEW_PRICE)).thenReturn(2);

        when(repository.markApplied(PLAN_ID))
                .thenReturn(1);

        when(repository.findById(PLAN_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalStateException.class,
                () -> service.applyPlan(
                        PLAN_ID, ADMIN_ID));
    }

    // =========================================
    // NHOM B: KET THUC KE HOACH GIA
    // =========================================

    // TC15-18: End thanh cong
    @Test
    void testEndSuccessfully() {

        prepareEnd();

        when(repository.lockRoomPrices(CATEGORY_ID))
                .thenReturn(rooms(
                        2, NEW_PRICE, NEW_PRICE));

        when(repository.updateRoomPrices(
                CATEGORY_ID,
                NEW_PRICE,
                OLD_PRICE)).thenReturn(2);

        when(repository.markEnded(PLAN_ID))
                .thenReturn(1);

        when(repository.findById(PLAN_ID))
                .thenReturn(Optional.of(
                        plan("Ended")));

        PricePlanSnapshot result = service.endPlan(
                PLAN_ID, ADMIN_ID);

        assertEquals("Ended", result.status());

        verify(repository).updateRoomPrices(
                CATEGORY_ID,
                NEW_PRICE,
                OLD_PRICE);

        verify(repository).markEnded(PLAN_ID);

        // End khong can kiem tra ngay hien tai
        verify(repository, never())
                .getDatabaseToday();
    }

    // TC15-19: Chi Applied moi duoc End
    @ParameterizedTest
    @ValueSource(strings = {
            "Draft",
            "Approved",
            "Rejected",
            "Ended"
    })
    void testCannotEndWrongStatus(String status) {

        mockAdmin();
        mockPlan(status);

        assertThrows(
                PricePlanExecutionService.PricePlanExecutionConflictException.class,
                () -> service.endPlan(
                        PLAN_ID, ADMIN_ID));

        verify(repository, never())
                .updateRoomPrices(
                        anyInt(), any(), any());
    }

    // TC15-20: Applied nhung thieu applied_at
    @Test
    void testEndMissingAppliedAt() {

        mockAdmin();

        PricePlanSnapshot invalid = new PricePlanSnapshot(
                PLAN_ID,
                CATEGORY_ID,
                OLD_PRICE,
                NEW_PRICE,
                FROM,
                TO,
                "Applied",
                null);

        when(repository.lockPlanById(PLAN_ID))
                .thenReturn(Optional.of(invalid));

        assertThrows(
                IllegalStateException.class,
                () -> service.endPlan(
                        PLAN_ID, ADMIN_ID));

        verify(repository, never())
                .updateRoomPrices(
                        anyInt(), any(), any());
    }

    // TC15-21: Gia phong da thay doi
    @Test
    void testEndWhenPriceChanged() {

        prepareEnd();

        BigDecimal changedPrice = new BigDecimal("380000.00");

        when(repository.lockRoomPrices(CATEGORY_ID))
                .thenReturn(rooms(
                        2,
                        changedPrice,
                        changedPrice));

        assertThrows(
                PricePlanExecutionService.PricePlanExecutionConflictException.class,
                () -> service.endPlan(
                        PLAN_ID, ADMIN_ID));

        verify(repository, never())
                .markEnded(anyInt());
    }

    // TC15-22: Co ke hoach Applied khac
    @Test
    void testEndOtherAppliedPlanConflict() {

        mockAdmin();
        mockPlan("Applied");

        when(repository.lockCategory(CATEGORY_ID))
                .thenReturn(true);

        when(repository.hasOtherAppliedPlan(
                CATEGORY_ID, PLAN_ID))
                .thenReturn(true);

        assertThrows(
                PricePlanExecutionService.PricePlanExecutionConflictException.class,
                () -> service.endPlan(
                        PLAN_ID, ADMIN_ID));

        verify(repository, never())
                .updateRoomPrices(
                        anyInt(), any(), any());
    }

    // TC15-23: Khoi phuc gia khong du phong
    @Test
    void testEndPartialRoomUpdate() {

        prepareEnd();

        when(repository.lockRoomPrices(CATEGORY_ID))
                .thenReturn(rooms(
                        3,
                        NEW_PRICE,
                        NEW_PRICE));

        when(repository.updateRoomPrices(
                CATEGORY_ID,
                NEW_PRICE,
                OLD_PRICE)).thenReturn(2);

        assertThrows(
                PricePlanExecutionService.PricePlanExecutionConflictException.class,
                () -> service.endPlan(
                        PLAN_ID, ADMIN_ID));

        verify(repository, never())
                .markEnded(anyInt());
    }

    // TC15-24: Khong chuyen duoc sang Ended
    @Test
    void testMarkEndedFails() {

        prepareEnd();

        when(repository.lockRoomPrices(CATEGORY_ID))
                .thenReturn(rooms(
                        2,
                        NEW_PRICE,
                        NEW_PRICE));

        when(repository.updateRoomPrices(
                CATEGORY_ID,
                NEW_PRICE,
                OLD_PRICE)).thenReturn(2);

        when(repository.markEnded(PLAN_ID))
                .thenReturn(0);

        assertThrows(
                PricePlanExecutionService.PricePlanExecutionConflictException.class,
                () -> service.endPlan(
                        PLAN_ID, ADMIN_ID));

        verify(repository).markEnded(PLAN_ID);
    }

    // =========================================
    // NHOM C: LOI DATABASE
    // =========================================

    // TC15-25: Repository gap loi database
    @Test
    void testDatabaseFailure() {

        mockAdmin();

        DataAccessResourceFailureException error = new DataAccessResourceFailureException(
                "Khong ket noi duoc SQL Server");

        when(repository.lockPlanById(PLAN_ID))
                .thenThrow(error);

        DataAccessResourceFailureException actual = assertThrows(
                DataAccessResourceFailureException.class,
                () -> service.applyPlan(
                        PLAN_ID, ADMIN_ID));

        assertSame(error, actual);
    }
}
