
package com.example.demo.modules.priceplan.service;

import com.example.demo.modules.priceplan.repository.PricePlanExecutionRepository;
import com.example.demo.modules.priceplan.repository.PricePlanExecutionRepository.PricePlanSnapshot;
import com.example.demo.modules.priceplan.repository.PricePlanExecutionRepository.RoomPriceSnapshot;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class PricePlanExecutionService {

        private final PricePlanExecutionRepository repository;

        public PricePlanExecutionService(
                        PricePlanExecutionRepository repository) {

                this.repository = repository;
        }

        // ==========================================
        // US-15A: AP DUNG KE HOACH GIA
        // Approved -> Applied
        // ==========================================

        @Transactional
        public PricePlanSnapshot applyPlan(
                        int planId,
                        int adminId) {

                // 1. Kiem tra ID va quyen Admin
                validatePlanId(planId);
                checkAdmin(adminId);

                // 2. Doc va khoa ke hoach
                PricePlanSnapshot plan = lockExistingPlan(planId);

                // 3. Chi Approved moi duoc ap dung
                if (!"Approved".equals(plan.status())) {

                        throw new PricePlanExecutionConflictException(
                                        "Chi ke hoach Approved moi duoc ap dung");
                }

                // 4. Kiem tra du lieu ke hoach
                validatePlanData(plan);

                // 5. Kiem tra thoi gian hieu luc
                LocalDate today = repository.getDatabaseToday();

                if (today.isBefore(plan.effectiveFrom())) {

                        throw new PricePlanExecutionConflictException(
                                        "Chua den ngay ap dung ke hoach");
                }

                if (plan.effectiveTo() != null
                                && today.isAfter(plan.effectiveTo())) {

                        throw new PricePlanExecutionConflictException(
                                        "Ke hoach da qua thoi gian hieu luc");
                }

                // 6. Khoa loai phong
                lockExistingCategory(plan.categoryId());

                // 7. Kiem tra xung dot voi ke hoach khac
                checkOtherAppliedPlans(plan);

                // 8. Khoa va doc gia tat ca phong
                RoomPriceSnapshot rooms = repository.lockRoomPrices(
                                plan.categoryId());

                // Gia hien tai phai bang old_price
                validateRoomPrices(
                                rooms,
                                plan.oldPrice());

                // 9. Cap nhat gia phong
                int updatedRooms = repository.updateRoomPrices(
                                plan.categoryId(),
                                plan.oldPrice(),
                                plan.newPrice());

                // Tat ca phong phai duoc cap nhat
                if (updatedRooms != rooms.roomCount()) {

                        throw new PricePlanExecutionConflictException(
                                        "So phong duoc cap nhat khong khop. "
                                                        + "Giao dich phai rollback");
                }

                // 10. Chuyen Approved -> Applied
                int updatedPlans = repository.markApplied(planId);

                if (updatedPlans != 1) {

                        throw new PricePlanExecutionConflictException(
                                        "Khong the chuyen ke hoach "
                                                        + "sang Applied");
                }

                // 11. Doc va kiem tra ket qua
                return loadResult(planId, "Applied");
        }

        // ==========================================
        // US-15B: KET THUC KE HOACH GIA
        // Applied -> Ended
        // ==========================================

        @Transactional
        public PricePlanSnapshot endPlan(
                        int planId,
                        int adminId) {

                // 1. Kiem tra ID va quyen Admin
                validatePlanId(planId);
                checkAdmin(adminId);

                // 2. Doc va khoa ke hoach
                PricePlanSnapshot plan = lockExistingPlan(planId);

                // 3. Chi Applied moi duoc ket thuc
                if (!"Applied".equals(plan.status())) {

                        throw new PricePlanExecutionConflictException(
                                        "Chi ke hoach Applied moi duoc ket thuc");
                }

                // 4. Kiem tra du lieu ke hoach
                validatePlanData(plan);

                if (plan.appliedAt() == null) {

                        throw new IllegalStateException(
                                        "Ke hoach Applied nhung thieu applied_at");
                }

                // 5. Khoa loai phong
                lockExistingCategory(plan.categoryId());

                // 6. Kiem tra ke hoach Applied khac
                checkOtherAppliedPlans(plan);

                // 7. Khoa va kiem tra gia hien tai
                RoomPriceSnapshot rooms = repository.lockRoomPrices(
                                plan.categoryId());

                // Gia hien tai phai bang new_price
                validateRoomPrices(
                                rooms,
                                plan.newPrice());

                // 8. Khoi phuc gia cu
                int updatedRooms = repository.updateRoomPrices(
                                plan.categoryId(),
                                plan.newPrice(),
                                plan.oldPrice());

                if (updatedRooms != rooms.roomCount()) {

                        throw new PricePlanExecutionConflictException(
                                        "Khong the khoi phuc gia tat ca phong. "
                                                        + "Giao dich phai rollback");
                }

                // 9. Chuyen Applied -> Ended
                int updatedPlans = repository.markEnded(planId);

                if (updatedPlans != 1) {

                        throw new PricePlanExecutionConflictException(
                                        "Khong the chuyen ke hoach "
                                                        + "sang Ended");
                }

                // 10. Doc va kiem tra ket qua
                return loadResult(planId, "Ended");
        }

        // ==========================================
        // CAC HAM KIEM TRA NGHIEP VU
        // ==========================================

        private void validatePlanId(int planId) {

                if (planId <= 0) {

                        throw new IllegalArgumentException(
                                        "ID ke hoach gia khong hop le");
                }
        }

        // Chi Admin duoc thuc hien US-15
        private void checkAdmin(int adminId) {

                if (adminId <= 0) {

                        throw new PricePlanExecutionForbiddenException(
                                        "Nguoi thuc hien khong hop le");
                }

                String role = repository.findUserRole(adminId)
                                .orElseThrow(
                                                () -> new PricePlanExecutionForbiddenException(
                                                                "Khong tim thay nguoi thuc hien"));

                if (!"Admin".equals(role)) {

                        throw new PricePlanExecutionForbiddenException(
                                        "Chi Admin duoc ap dung "
                                                        + "hoac ket thuc ke hoach gia");
                }
        }

        // Doc ke hoach va giu khoa trong transaction
        private PricePlanSnapshot lockExistingPlan(
                        int planId) {

                return repository.lockPlanById(planId)
                                .orElseThrow(
                                                () -> new PricePlanExecutionNotFoundException(
                                                                "Khong tim thay ke hoach gia"));
        }

        // Kiem tra du lieu co ban cua ke hoach
        private void validatePlanData(
                        PricePlanSnapshot plan) {

                if (plan.categoryId() <= 0
                                || plan.oldPrice() == null
                                || plan.newPrice() == null
                                || plan.effectiveFrom() == null) {

                        throw new IllegalStateException(
                                        "Du lieu ke hoach gia khong day du");
                }

                if (plan.oldPrice().compareTo(BigDecimal.ZERO) < 0
                                || plan.newPrice().compareTo(BigDecimal.ZERO) <= 0) {

                        throw new IllegalStateException(
                                        "Gia cu hoac gia moi khong hop le");
                }

                if (plan.effectiveTo() != null
                                && plan.effectiveTo()
                                                .isBefore(plan.effectiveFrom())) {

                        throw new IllegalStateException(
                                        "Khoang thoi gian ke hoach khong hop le");
                }
        }

        // Khoa category truoc khi xu ly gia
        private void lockExistingCategory(int categoryId) {

                boolean exists = repository.lockCategory(categoryId);

                if (!exists) {

                        throw new IllegalStateException(
                                        "Loai phong cua ke hoach khong ton tai");
                }
        }

        // Khong cho hai ke hoach Applied cung loai
        private void checkOtherAppliedPlans(
                        PricePlanSnapshot plan) {

                boolean hasConflict = repository.hasOtherAppliedPlan(
                                plan.categoryId(),
                                plan.planId());

                if (hasConflict) {

                        throw new PricePlanExecutionConflictException(
                                        "Loai phong dang co "
                                                        + "ke hoach gia Applied khac");
                }
        }

        // Kiem tra tat ca phong dang dong gia
        // va gia hien tai bang gia mong doi
        private void validateRoomPrices(
                        RoomPriceSnapshot rooms,
                        BigDecimal expectedPrice) {

                if (rooms == null
                                || rooms.roomCount() <= 0) {

                        throw new PricePlanExecutionConflictException(
                                        "Loai phong khong co phong de cap nhat");
                }

                if (rooms.minPrice() == null
                                || rooms.maxPrice() == null) {

                        throw new IllegalStateException(
                                        "Khong doc duoc gia phong");
                }

                // Cac phong phai co cung mot gia
                if (rooms.minPrice()
                                .compareTo(rooms.maxPrice()) != 0) {

                        throw new PricePlanExecutionConflictException(
                                        "Cac phong trong cung loai "
                                                        + "khong dong gia");
                }

                // Gia hien tai phai dung voi ke hoach
                if (rooms.minPrice()
                                .compareTo(expectedPrice) != 0) {

                        throw new PricePlanExecutionConflictException(
                                        "Gia phong hien tai khong khop "
                                                        + "voi gia du kien trong ke hoach");
                }
        }

        // Doc va kiem tra ban ghi sau UPDATE
        private PricePlanSnapshot loadResult(
                        int planId,
                        String expectedStatus) {

                PricePlanSnapshot result = repository.findById(planId)
                                .orElseThrow(
                                                () -> new IllegalStateException(
                                                                "Khong doc duoc ke hoach sau cap nhat"));

                if (!expectedStatus.equals(result.status())) {

                        throw new IllegalStateException(
                                        "Trang thai ke hoach sau cap nhat khong hop le");
                }

                if ("Applied".equals(expectedStatus)
                                && result.appliedAt() == null) {

                        throw new IllegalStateException(
                                        "Ke hoach da Applied nhung chua luu applied_at");
                }

                return result;
        }

        // ==========================================
        // NGOAI LE NGHIEP VU
        // ==========================================

        // 403 Forbidden
        public static class PricePlanExecutionForbiddenException
                        extends RuntimeException {

                public PricePlanExecutionForbiddenException(
                                String message) {
                        super(message);
                }
        }

        // 404 Not Found
        public static class PricePlanExecutionNotFoundException
                        extends RuntimeException {

                public PricePlanExecutionNotFoundException(
                                String message) {
                        super(message);
                }
        }

        // 409 Conflict
        public static class PricePlanExecutionConflictException
                        extends RuntimeException {

                public PricePlanExecutionConflictException(
                                String message) {
                        super(message);
                }
        }
}
