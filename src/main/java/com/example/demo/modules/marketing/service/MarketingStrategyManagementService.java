
package com.example.demo.modules.marketing.service;

import com.example.demo.modules.marketing.dto.ChangeMarketingStrategyStatusRequest;
import com.example.demo.modules.marketing.dto.MarketingStrategyResponse;
import com.example.demo.modules.marketing.dto.UpdateMarketingStrategyRequest;

import com.example.demo.modules.marketing.repository.MarketingStrategyManagementRepository;
import com.example.demo.modules.marketing.repository.MarketingStrategyManagementRepository.LockedStrategy;
import com.example.demo.modules.marketing.repository.MarketingStrategyManagementRepository.DiscountSnapshot;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class MarketingStrategyManagementService {

    private final MarketingStrategyManagementRepository repository;

    private static final BigDecimal MAX_MONEY = new BigDecimal("999999999999.99");

    public MarketingStrategyManagementService(
            MarketingStrategyManagementRepository repository) {

        this.repository = repository;
    }

    // ============================================
    // US-14A: CAP NHAT NOI DUNG CHIEN LUOC
    // ============================================

    @Transactional
    public MarketingStrategyResponse updateDetails(
            int strategyId,
            UpdateMarketingStrategyRequest request,
            int managerId) {

        // 1. Kiem tra dau vao
        validateStrategyId(strategyId);
        validateUpdateRequest(request);

        // 2. Kiem tra quyen Manager
        checkManager(managerId);

        // 3. Doc va khoa chien luoc
        LockedStrategy current = repository.lockStrategyById(strategyId)
                .orElseThrow(
                        () -> new ManagementStrategyNotFoundException(
                                "Khong tim thay chien luoc marketing"));

        // 4. Chi Planned/Running duoc sua
        if (!"Planned".equals(current.status())
                && !"Running".equals(current.status())) {

            throw new ManagementConflictException(
                    "Chi chien luoc Planned hoac Running moi duoc sua");
        }

        // 5. Kiem tra lai discount neu co
        if (request.discountId() != null) {
            validateDiscount(request);
        }

        // 6. Cap nhat co dieu kien trong SQL
        int affectedRows = repository.updateStrategyDetails(
                strategyId,
                request);

        if (affectedRows != 1) {
            throw new ManagementConflictException(
                    "Khong the cap nhat. "
                            + "Chien luoc co the da thay doi trang thai");
        }

        // 7. Doc lai ban ghi vua cap nhat
        return loadUpdatedStrategy(strategyId);
    }

    // ============================================
    // US-14B: CHUYEN TRANG THAI CHIEN LUOC
    // ============================================

    @Transactional
    public MarketingStrategyResponse changeStrategyStatus(
            int strategyId,
            ChangeMarketingStrategyStatusRequest request,
            int managerId) {

        // 1. Kiem tra dau vao
        validateStrategyId(strategyId);

        if (request == null || request.status() == null
                || request.status().isBlank()) {

            throw new IllegalArgumentException(
                    "Trang thai moi khong duoc rong");
        }

        String newStatus = request.status();

        // 2. Kiem tra ten trang thai
        if (!isValidStatus(newStatus)) {
            throw new IllegalArgumentException(
                    "Trang thai khong hop le");
        }

        // 3. Kiem tra quyen Manager
        checkManager(managerId);

        // 4. Doc va khoa chien luoc
        LockedStrategy current = repository.lockStrategyById(strategyId)
                .orElseThrow(
                        () -> new ManagementStrategyNotFoundException(
                                "Khong tim thay chien luoc marketing"));

        String currentStatus = current.status();

        // 5. Kiem tra quy tac chuyen trang thai
        validateStatusTransition(
                currentStatus,
                newStatus);

        // 6. UPDATE co dieu kien
        int affectedRows = repository.changeStatus(
                strategyId,
                currentStatus,
                newStatus);

        if (affectedRows != 1) {
            throw new ManagementConflictException(
                    "Chuyen trang thai that bai. "
                            + "Trang thai chien luoc co the da thay doi");
        }

        // 7. Doc lai chien luoc
        return loadUpdatedStrategy(strategyId);
    }

    // ============================================
    // CAC HAM KIEM TRA NGHIEP VU
    // ============================================

    private void validateStrategyId(int strategyId) {

        if (strategyId <= 0) {
            throw new IllegalArgumentException(
                    "ID chien luoc khong hop le");
        }
    }

    // Kiem tra quyen Manager
    private void checkManager(int managerId) {

        if (managerId <= 0) {
            throw new ManagementForbiddenException(
                    "Nguoi thuc hien khong hop le");
        }

        String role = repository.findUserRole(managerId)
                .orElseThrow(
                        () -> new ManagementForbiddenException(
                                "Khong tim thay nguoi thuc hien"));

        if (!"Manager".equals(role)) {
            throw new ManagementForbiddenException(
                    "Chi Manager duoc quan ly chien luoc marketing");
        }
    }

    // Kiem tra du lieu cap nhat
    private void validateUpdateRequest(
            UpdateMarketingStrategyRequest request) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Du lieu cap nhat khong duoc rong");
        }

        // Ten chien luoc
        if (request.title() == null
                || request.title().isBlank()
                || request.title().length() > 150) {

            throw new IllegalArgumentException(
                    "Ten chien luoc khong hop le");
        }

        // Doi tuong muc tieu
        if (request.targetSegment() != null
                && request.targetSegment().length() > 150) {

            throw new IllegalArgumentException(
                    "Doi tuong muc tieu toi da 150 ky tu");
        }

        // Thoi gian chien luoc
        if (request.startDate() == null
                || request.endDate() == null) {

            throw new IllegalArgumentException(
                    "Phai nhap ngay bat dau va ngay ket thuc");
        }

        if (request.endDate()
                .isBefore(request.startDate())) {

            throw new IllegalArgumentException(
                    "Ngay ket thuc phai bang hoac sau ngay bat dau");
        }

        // Ngan sach va chi phi
        validateMoney(request.budget(), "Ngan sach");

        validateMoney(
                request.actualSpend(),
                "Chi phi thuc te");

        // ID ma giam gia
        if (request.discountId() != null
                && request.discountId() <= 0) {

            throw new IllegalArgumentException(
                    "ID ma giam gia khong hop le");
        }

        // Booking ky vong
        if (request.expectedBookings() != null
                && request.expectedBookings() < 0) {

            throw new IllegalArgumentException(
                    "So booking ky vong khong duoc am");
        }
    }

    // Kiem tra DECIMAL(14,2)
    private void validateMoney(
            BigDecimal value,
            String fieldName) {

        if (value == null
                || value.compareTo(BigDecimal.ZERO) < 0
                || value.compareTo(MAX_MONEY) > 0
                || value.scale() > 2) {

            throw new IllegalArgumentException(
                    fieldName
                            + " phai >= 0 va dung dinh dang DECIMAL(14,2)");
        }
    }

    // Kiem tra ma giam gia khi cap nhat
    private void validateDiscount(
            UpdateMarketingStrategyRequest request) {

        DiscountSnapshot discount = repository.lockDiscountById(
                request.discountId()).orElseThrow(
                        () -> new ManagementDiscountNotFoundException(
                                "Khong tim thay ma giam gia"));

        // Ma phai Active
        if (!"Active".equals(discount.status())) {

            throw new ManagementConflictException(
                    "Ma giam gia khong o trang thai Active");
        }

        // Ngay bat dau ma phai <= ngay bat dau chien luoc
        if (discount.validFrom()
                .isAfter(request.startDate())) {

            throw new ManagementConflictException(
                    "Ma giam gia bat dau sau ngay bat dau chien luoc");
        }

        // Ngay ket thuc ma phai >= ngay ket thuc chien luoc
        if (discount.validTo()
                .isBefore(request.endDate())) {

            throw new ManagementConflictException(
                    "Ma giam gia khong bao phu toan bo chien luoc");
        }
    }

    // Kiem tra ten trang thai
    private boolean isValidStatus(String status) {

        return switch (status) {
            case "Planned", "Running",
                    "Finished", "Cancelled" ->
                true;

            default -> false;
        };
    }

    // Kiem tra quy tac chuyen trang thai
    private void validateStatusTransition(
            String currentStatus,
            String newStatus) {

        boolean allowed = switch (currentStatus) {

            case "Planned" ->
                "Running".equals(newStatus)
                        || "Cancelled".equals(newStatus);

            case "Running" ->
                "Finished".equals(newStatus)
                        || "Cancelled".equals(newStatus);

            default -> false;
        };

        if (!allowed) {
            throw new ManagementConflictException(
                    "Khong cho phep chuyen tu "
                            + currentStatus
                            + " sang "
                            + newStatus);
        }
    }

    // Doc lai ban ghi sau khi UPDATE
    private MarketingStrategyResponse loadUpdatedStrategy(
            int strategyId) {

        return repository.findById(strategyId)
                .orElseThrow(
                        () -> new IllegalStateException(
                                "Khong doc duoc chien luoc sau cap nhat"));
    }

    // ============================================
    // CAC NGOAI LE NGHIEP VU
    // ============================================

    // 403 Forbidden
    public static class ManagementForbiddenException
            extends RuntimeException {

        public ManagementForbiddenException(String message) {
            super(message);
        }
    }

    // 404 Not Found - chien luoc
    public static class ManagementStrategyNotFoundException
            extends RuntimeException {

        public ManagementStrategyNotFoundException(String message) {
            super(message);
        }
    }

    // 404 Not Found - discount
    public static class ManagementDiscountNotFoundException
            extends RuntimeException {

        public ManagementDiscountNotFoundException(String message) {
            super(message);
        }
    }

    // 409 Conflict
    public static class ManagementConflictException
            extends RuntimeException {

        public ManagementConflictException(String message) {
            super(message);
        }
    }
}
