
package com.example.demo.modules.priceplan.service;

import com.example.demo.modules.priceplan.dto.ReviewPricePlanRequest;
import com.example.demo.modules.priceplan.dto.ReviewPricePlanResponse;

import com.example.demo.modules.priceplan.repository.PricePlanReviewRepository;
import com.example.demo.modules.priceplan.repository.PricePlanReviewRepository.PlanForReview;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PricePlanReviewService {

    private final PricePlanReviewRepository repository;

    public PricePlanReviewService(
            PricePlanReviewRepository repository) {

        this.repository = repository;
    }

    // US-10: Admin duyet hoac tu choi ke hoach
    @Transactional
    public ReviewPricePlanResponse reviewPricePlan(
            int planId,
            ReviewPricePlanRequest request,
            int adminId) {

        // 1. Kiem tra du lieu dau vao
        validateRequest(planId, request);

        // 2. Kiem tra ID nguoi xet duyet
        if (adminId <= 0) {
            throw new PricePlanReviewForbiddenException(
                    "Nguoi xet duyet khong hop le");
        }

        // 3. Kiem tra vai tro Admin
        String role = repository.findUserRole(adminId)
                .orElseThrow(
                        () -> new PricePlanReviewForbiddenException(
                                "Khong tim thay nguoi xet duyet"));

        if (!"Admin".equals(role)) {
            throw new PricePlanReviewForbiddenException(
                    "Chi Admin duoc xet duyet ke hoach gia");
        }

        // 4. Tim loai phong cua ke hoach
        int categoryId = repository.findPlanCategoryId(planId)
                .orElseThrow(
                        () -> new PricePlanReviewNotFoundException(
                                "Khong tim thay ke hoach gia"));

        // 5. Khoa theo loai phong
        // Khoa duoc giu den khi transaction ket thuc
        repository.lockCategoryForReview(categoryId);

        // 6. Doc lai va khoa ke hoach
        PlanForReview plan = repository.findPlanForReview(planId)
                .orElseThrow(
                        () -> new PricePlanReviewNotFoundException(
                                "Khong tim thay ke hoach gia"));

        // 7. Dam bao loai phong khong bi thay doi
        if (plan.categoryId() != categoryId) {
            throw new IllegalStateException(
                    "Loai phong cua ke hoach da thay doi");
        }

        // 8. Chi cho phep xet duyet Draft
        if (!"Draft".equals(plan.status())) {
            throw new IllegalStateException(
                    "Chi ke hoach Draft moi duoc xet duyet");
        }

        // 9. Nguoi duyet phai khac nguoi tao
        if (plan.createdBy() == adminId) {
            throw new PricePlanReviewForbiddenException(
                    "Nguoi duyet phai khac nguoi lap ke hoach");
        }

        boolean approve = request.approve();

        String rejectReason = null;

        // 10. Neu tu choi, bat buoc co ly do
        if (!approve) {
            rejectReason = request.rejectReason().trim();
        }

        // 11. Neu duyet, phai kiem tra trung thoi gian
        if (approve) {

            boolean overlapping = repository.hasOverlappingPlan(
                    plan.categoryId(),
                    plan.planId(),
                    plan.effectiveFrom(),
                    plan.effectiveTo());

            if (overlapping) {
                throw new IllegalStateException(
                        "Ke hoach trung thoi gian voi "
                                + "ke hoach Approved hoac Applied "
                                + "khac cua cung loai phong");
            }
        }

        // 12. Cap nhat trang thai
        boolean updated = repository.updateReview(
                planId,
                adminId,
                approve,
                rejectReason);

        if (!updated) {
            throw new IllegalStateException(
                    "Khong the xet duyet vi "
                            + "trang thai ke hoach da thay doi");
        }

        // 13. Doc ket qua sau khi cap nhat
        ReviewPricePlanResponse result = repository.findReviewResult(planId)
                .orElseThrow(
                        () -> new IllegalStateException(
                                "Khong tim thay ket qua xet duyet"));

        // 14. Kiem tra trang thai cuoi cung
        String expectedStatus = approve ? "Approved" : "Rejected";

        if (!expectedStatus.equals(result.status())) {
            throw new IllegalStateException(
                    "Trang thai sau xet duyet khong hop le");
        }

        return result;
    }

    // Kiem tra Request truoc khi xu ly
    private void validateRequest(
            int planId,
            ReviewPricePlanRequest request) {

        if (planId <= 0) {
            throw new IllegalArgumentException(
                    "ID ke hoach khong hop le");
        }

        if (request == null || request.approve() == null) {
            throw new IllegalArgumentException(
                    "Phai chon duyet hoac tu choi");
        }

        String reason = request.rejectReason();

        if (reason != null && reason.trim().length() > 500) {
            throw new IllegalArgumentException(
                    "Ly do tu choi toi da 500 ky tu");
        }

        if (!request.approve()) {
            if (reason == null || reason.isBlank()) {
                throw new IllegalArgumentException(
                        "Phai nhap ly do khi tu choi ke hoach");
            }
        } else {
            // Khi duyet, khong cho gui ly do tu choi
            if (reason != null && !reason.isBlank()) {
                throw new IllegalArgumentException(
                        "Khong nhap ly do tu choi khi duyet");
            }
        }
    }

    // Loi nguoi dung khong co quyen
    public static class PricePlanReviewForbiddenException
            extends RuntimeException {

        public PricePlanReviewForbiddenException(
                String message) {
            super(message);
        }
    }

    // Loi khong tim thay ke hoach
    public static class PricePlanReviewNotFoundException
            extends RuntimeException {

        public PricePlanReviewNotFoundException(
                String message) {
            super(message);
        }
    }
}
