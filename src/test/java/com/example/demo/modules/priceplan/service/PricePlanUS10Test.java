
package com.example.demo.modules.priceplan.service;

import com.example.demo.modules.priceplan.dto.ReviewPricePlanRequest;
import com.example.demo.modules.priceplan.dto.ReviewPricePlanResponse;

import com.example.demo.modules.priceplan.repository.PricePlanReviewRepository;
import com.example.demo.modules.priceplan.repository.PricePlanReviewRepository.PlanForReview;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PricePlanUS10Test {

    private final PricePlanReviewRepository repository = mock(PricePlanReviewRepository.class);

    private final PricePlanReviewService service = new PricePlanReviewService(repository);

    private final LocalDate from = LocalDate.of(2027, 2, 1);

    private final LocalDate to = LocalDate.of(2027, 2, 28);

    // Request duyet
    private ReviewPricePlanRequest approveRequest() {
        return new ReviewPricePlanRequest(true, null);
    }

    // Request tu choi
    private ReviewPricePlanRequest rejectRequest() {
        return new ReviewPricePlanRequest(
                false,
                "Gia de xuat qua thap");
    }

    // Ke hoach dang Draft
    private PlanForReview draftPlan() {
        return new PlanForReview(
                3,
                1,
                2,
                "Draft",
                from,
                to);
    }

    // Response gia lap
    private ReviewPricePlanResponse response(
            String status,
            String rejectReason) {

        return new ReviewPricePlanResponse(
                3,
                "US10 TEST - Duyet Standard",
                1,
                from,
                to,
                status,
                2,
                1,
                LocalDateTime.of(2026, 11, 22, 21, 0),
                rejectReason);
    }

    // Gia lap Admin hop le va ke hoach Draft
    private void mockValidReviewContext() {

        when(repository.findUserRole(1))
                .thenReturn(Optional.of("Admin"));

        when(repository.findPlanCategoryId(3))
                .thenReturn(Optional.of(1));

        when(repository.findPlanForReview(3))
                .thenReturn(Optional.of(draftPlan()));
    }

    // TC10-01: Duyet thanh cong
    @Test
    void testApproveSuccess() {

        mockValidReviewContext();

        when(repository.hasOverlappingPlan(
                1, 3, from, to))
                .thenReturn(false);

        when(repository.updateReview(
                3, 1, true, null))
                .thenReturn(true);

        when(repository.findReviewResult(3))
                .thenReturn(Optional.of(
                        response("Approved", null)));

        ReviewPricePlanResponse result = service.reviewPricePlan(
                3, approveRequest(), 1);

        assertEquals("Approved", result.status());
        assertEquals(1, result.approvedBy());
        assertNull(result.rejectReason());
        assertNotNull(result.approvedAt());

        // Kiem tra thu tu thao tac
        InOrder order = inOrder(repository);

        order.verify(repository).findUserRole(1);
        order.verify(repository).findPlanCategoryId(3);
        order.verify(repository).lockCategoryForReview(1);
        order.verify(repository).findPlanForReview(3);

        order.verify(repository).hasOverlappingPlan(
                1, 3, from, to);

        order.verify(repository).updateReview(
                3, 1, true, null);

        order.verify(repository).findReviewResult(3);
    }

    // TC10-02: Tu choi thanh cong
    @Test
    void testRejectSuccess() {

        mockValidReviewContext();

        when(repository.updateReview(
                3, 1, false, "Gia de xuat qua thap"))
                .thenReturn(true);

        when(repository.findReviewResult(3))
                .thenReturn(Optional.of(
                        response(
                                "Rejected",
                                "Gia de xuat qua thap")));

        ReviewPricePlanResponse result = service.reviewPricePlan(
                3, rejectRequest(), 1);

        assertEquals("Rejected", result.status());

        assertEquals(
                "Gia de xuat qua thap",
                result.rejectReason());

        // Tu choi khong can kiem tra trung ngay
        verify(repository, never())
                .hasOverlappingPlan(
                        anyInt(),
                        anyInt(),
                        any(),
                        any());

        verify(repository).updateReview(
                3, 1, false, "Gia de xuat qua thap");
    }

    // TC10-03: Ke hoach trung thoi gian
    @Test
    void testOverlappingPlan() {

        mockValidReviewContext();

        when(repository.hasOverlappingPlan(
                1, 3, from, to))
                .thenReturn(true);

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> service.reviewPricePlan(
                        3, approveRequest(), 1));

        assertTrue(
                error.getMessage().contains(
                        "trung thoi gian"));

        verify(repository, never())
                .updateReview(
                        anyInt(),
                        anyInt(),
                        anyBoolean(),
                        any());
    }

    // TC10-04: Khong phai Admin hoac user khong ton tai
    @Test
    void testNotAdmin() {

        when(repository.findUserRole(1))
                .thenReturn(Optional.of("Manager"));

        when(repository.findUserRole(99))
                .thenReturn(Optional.empty());

        assertThrows(
                PricePlanReviewService.PricePlanReviewForbiddenException.class,
                () -> service.reviewPricePlan(
                        3, approveRequest(), 1));

        assertThrows(
                PricePlanReviewService.PricePlanReviewForbiddenException.class,
                () -> service.reviewPricePlan(
                        3, approveRequest(), 99));

        verify(repository, never())
                .findPlanCategoryId(anyInt());
    }

    // TC10-05: Admin ID khong hop le
    @Test
    void testInvalidAdminId() {

        assertThrows(
                PricePlanReviewService.PricePlanReviewForbiddenException.class,
                () -> service.reviewPricePlan(
                        3, approveRequest(), 0));

        verifyNoInteractions(repository);
    }

    // TC10-06: Ke hoach khong ton tai
    @Test
    void testPlanNotFound() {

        when(repository.findUserRole(1))
                .thenReturn(Optional.of("Admin"));

        when(repository.findPlanCategoryId(99))
                .thenReturn(Optional.empty());

        assertThrows(
                PricePlanReviewService.PricePlanReviewNotFoundException.class,
                () -> service.reviewPricePlan(
                        99, approveRequest(), 1));

        verify(repository, never())
                .lockCategoryForReview(anyInt());
    }

    // TC10-07: Ke hoach khong con Draft
    @Test
    void testPlanNotDraft() {

        mockValidReviewContext();

        when(repository.findPlanForReview(3))
                .thenReturn(Optional.of(
                        new PlanForReview(
                                3, 1, 2,
                                "Approved",
                                from, to)));

        assertThrows(
                IllegalStateException.class,
                () -> service.reviewPricePlan(
                        3, approveRequest(), 1));

        verify(repository, never())
                .updateReview(
                        anyInt(),
                        anyInt(),
                        anyBoolean(),
                        any());
    }

    // TC10-08: Admin tu duyet ke hoach cua minh
    @Test
    void testReviewerIsCreator() {

        mockValidReviewContext();

        when(repository.findPlanForReview(3))
                .thenReturn(Optional.of(
                        new PlanForReview(
                                3, 1, 1,
                                "Draft",
                                from, to)));

        assertThrows(
                PricePlanReviewService.PricePlanReviewForbiddenException.class,
                () -> service.reviewPricePlan(
                        3, approveRequest(), 1));

        verify(repository, never())
                .updateReview(
                        anyInt(),
                        anyInt(),
                        anyBoolean(),
                        any());
    }

    // TC10-09: Tu choi nhung thieu ly do
    @Test
    void testRejectWithoutReason() {

        assertThrows(
                IllegalArgumentException.class,
                () -> service.reviewPricePlan(
                        3,
                        new ReviewPricePlanRequest(false, null),
                        1));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.reviewPricePlan(
                        3,
                        new ReviewPricePlanRequest(false, "   "),
                        1));

        verifyNoInteractions(repository);
    }

    // TC10-10: Duyet nhung gui ly do tu choi
    @Test
    void testApproveWithRejectReason() {

        ReviewPricePlanRequest request = new ReviewPricePlanRequest(
                true,
                "Khong dong y");

        assertThrows(
                IllegalArgumentException.class,
                () -> service.reviewPricePlan(
                        3, request, 1));

        verifyNoInteractions(repository);
    }

    // TC10-11: Request khong hop le
    @Test
    void testInvalidRequest() {

        assertThrows(
                IllegalArgumentException.class,
                () -> service.reviewPricePlan(
                        0, approveRequest(), 1));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.reviewPricePlan(
                        3, null, 1));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.reviewPricePlan(
                        3,
                        new ReviewPricePlanRequest(null, null),
                        1));

        verifyNoInteractions(repository);
    }

    // TC10-12: Loai phong thay doi khi xet duyet
    @Test
    void testCategoryChanged() {

        mockValidReviewContext();

        // Lan doc dau la category 1
        // Lan doc sau lai thanh category 2
        when(repository.findPlanForReview(3))
                .thenReturn(Optional.of(
                        new PlanForReview(
                                3, 2, 2,
                                "Draft",
                                from, to)));

        assertThrows(
                IllegalStateException.class,
                () -> service.reviewPricePlan(
                        3, approveRequest(), 1));

        verify(repository, never())
                .updateReview(
                        anyInt(),
                        anyInt(),
                        anyBoolean(),
                        any());
    }

    // TC10-13: UPDATE khong thanh cong
    @Test
    void testUpdateFailed() {

        mockValidReviewContext();

        when(repository.hasOverlappingPlan(
                1, 3, from, to))
                .thenReturn(false);

        when(repository.updateReview(
                3, 1, true, null))
                .thenReturn(false);

        assertThrows(
                IllegalStateException.class,
                () -> service.reviewPricePlan(
                        3, approveRequest(), 1));

        verify(repository, never())
                .findReviewResult(anyInt());
    }

    // TC10-14: Khong doc duoc ket qua sau UPDATE
    @Test
    void testResultNotFound() {

        mockValidReviewContext();

        when(repository.updateReview(
                3, 1, true, null))
                .thenReturn(true);

        when(repository.findReviewResult(3))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalStateException.class,
                () -> service.reviewPricePlan(
                        3, approveRequest(), 1));
    }

    // TC10-15: Trang thai cuoi khong dung
    @Test
    void testUnexpectedFinalStatus() {

        mockValidReviewContext();

        when(repository.updateReview(
                3, 1, true, null))
                .thenReturn(true);

        // Du kien Approved, nhung DB tra Rejected
        when(repository.findReviewResult(3))
                .thenReturn(Optional.of(
                        response(
                                "Rejected",
                                "Ly do khac")));

        assertThrows(
                IllegalStateException.class,
                () -> service.reviewPricePlan(
                        3, approveRequest(), 1));
    }

    // TC10-16: Khong lay duoc khoa xet duyet
    @Test
    void testCategoryLockFailed() {

        mockValidReviewContext();

        doThrow(
                new IllegalStateException(
                        "Khong the khoa xet duyet loai phong"))
                .when(repository).lockCategoryForReview(1);

        assertThrows(
                IllegalStateException.class,
                () -> service.reviewPricePlan(
                        3, approveRequest(), 1));

        verify(repository, never())
                .findPlanForReview(anyInt());

        verify(repository, never())
                .updateReview(
                        anyInt(),
                        anyInt(),
                        anyBoolean(),
                        any());
    }
}
