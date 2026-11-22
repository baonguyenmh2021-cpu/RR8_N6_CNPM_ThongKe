
package com.example.demo.modules.discount.service;

import com.example.demo.modules.discount.dto.DiscountEligibilityResponse;
import com.example.demo.modules.discount.repository.DiscountManagementRepository;
import com.example.demo.modules.discount.repository.DiscountManagementRepository.DiscountState;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class DiscountManagementService {

    private final DiscountManagementRepository repository;

    public DiscountManagementService(
            DiscountManagementRepository repository) {

        this.repository = repository;
    }

    // 1. Kiem tra ma co the ap dung hay khong
    @Transactional(readOnly = true)
    public DiscountEligibilityResponse checkEligibility(
            int discountId,
            BigDecimal orderAmount) {

        if (orderAmount == null ||
                orderAmount.compareTo(BigDecimal.ZERO) < 0) {

            throw new IllegalArgumentException(
                    "Gia tri don hang khong hop le");
        }

        DiscountState state = getExistingState(discountId);

        LocalDate today = state.databaseToday();

        // Trang thai hien thi
        String effectiveStatus;

        if ("Disabled".equals(state.storedStatus())) {
            effectiveStatus = "Disabled";

        } else if (today.isAfter(state.validTo())) {
            effectiveStatus = "Expired";

        } else {
            effectiveStatus = "Active";
        }

        // Tinh so luot con lai
        Integer remainingUses = null;

        if (state.usageLimit() != null) {

            long remaining = Math.max(
                    0L,
                    (long) state.usageLimit() - state.uses());

            remainingUses = (int) remaining;
        }

        // Kiem tra dieu kien ap dung
        boolean canApply = true;
        String reason = "Ma co the ap dung";

        if ("Disabled".equals(state.storedStatus())) {

            canApply = false;
            reason = "Ma da bi vo hieu hoa";

        } else if (today.isBefore(state.validFrom())) {

            canApply = false;
            reason = "Ma chua den ngay hieu luc";

        } else if (today.isAfter(state.validTo())) {

            canApply = false;
            reason = "Ma da het han";

        } else if (remainingUses != null
                && remainingUses == 0) {

            canApply = false;
            reason = "Ma da het luot su dung";

        } else if (orderAmount.compareTo(
                state.minAmount()) < 0) {

            canApply = false;
            reason = "Don hang chua dat gia tri toi thieu";
        }

        return new DiscountEligibilityResponse(
                state.discountId(),
                state.code(),
                state.storedStatus(),
                effectiveStatus,
                state.validFrom(),
                state.validTo(),
                state.minAmount(),
                orderAmount,
                state.usageLimit(),
                state.uses(),
                remainingUses,
                canApply,
                reason);
    }

    // 2. Vo hieu hoa ma
    @Transactional
    public DiscountState disableDiscount(int discountId) {

        DiscountState current = getExistingState(discountId);

        if ("Disabled".equals(current.storedStatus())) {
            throw new IllegalStateException(
                    "Ma da bi vo hieu hoa truoc do");
        }

        boolean updated = repository.disableDiscount(discountId);

        if (!updated) {
            throw new IllegalStateException(
                    "Khong the vo hieu hoa do trang thai da thay doi");
        }

        return getExistingState(discountId);
    }

    // 3. Gia han ma
    @Transactional
    public DiscountState extendDiscount(
            int discountId,
            LocalDate newValidTo) {

        if (newValidTo == null) {
            throw new IllegalArgumentException(
                    "Ngay het han moi khong duoc rong");
        }

        DiscountState current = getExistingState(discountId);

        if (!newValidTo.isAfter(current.validTo())) {

            throw new IllegalArgumentException(
                    "Ngay het han moi phai sau ngay cu");
        }

        boolean updated = repository.extendDiscount(
                discountId, newValidTo);

        if (!updated) {
            throw new IllegalStateException(
                    "Khong the gia han do du lieu da thay doi");
        }

        // Doc lai thong tin sau khi cap nhat
        return getExistingState(discountId);
    }

    // 4. Tim ma va xu ly truong hop khong ton tai
    private DiscountState getExistingState(int discountId) {

        if (discountId <= 0) {
            throw new IllegalArgumentException(
                    "ID ma giam gia khong hop le");
        }

        return repository.findById(discountId)
                .orElseThrow(() -> new DiscountNotFoundException(
                        "Khong tim thay ma giam gia"));
    }

    // Ngoai le khi khong tim thay ma
    public static class DiscountNotFoundException
            extends RuntimeException {

        public DiscountNotFoundException(String message) {
            super(message);
        }
    }
}
