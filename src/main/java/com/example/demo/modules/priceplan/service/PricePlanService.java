
package com.example.demo.modules.priceplan.service;

import com.example.demo.modules.priceplan.dto.CreatePricePlanRequest;
import com.example.demo.modules.priceplan.dto.PricePlanResponse;

import com.example.demo.modules.priceplan.repository.PricePlanRepository;
import com.example.demo.modules.priceplan.repository.PricePlanRepository.CategoryPriceInfo;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class PricePlanService {

    private final PricePlanRepository repository;

    public PricePlanService(PricePlanRepository repository) {
        this.repository = repository;
    }

    // US-09: Manager tao ke hoach dieu chinh gia
    @Transactional
    public PricePlanResponse createPricePlan(
            CreatePricePlanRequest request,
            int managerId) {

        // 1. Kiem tra du lieu dau vao
        validateRequest(request);

        // 2. Kiem tra ID nguoi lap
        if (managerId <= 0) {
            throw new PricePlanForbiddenException(
                    "Nguoi lap ke hoach khong hop le");
        }

        // 3. Kiem tra vai tro trong database
        String role = repository.findUserRole(managerId)
                .orElseThrow(
                        () -> new PricePlanForbiddenException(
                                "Khong tim thay nguoi lap ke hoach"));

        if (!"Manager".equals(role)) {
            throw new PricePlanForbiddenException(
                    "Chi Manager duoc lap ke hoach gia");
        }

        // 4. Lay thong tin gia cua loai phong
        CategoryPriceInfo priceInfo = repository.findCategoryPriceInfo(
                request.categoryId()).orElseThrow(
                        () -> new PricePlanNotFoundException(
                                "Khong tim thay loai phong"));

        // 5. Loai phong phai co it nhat mot phong
        if (priceInfo.roomCount() <= 0) {
            throw new IllegalStateException(
                    "Loai phong chua co phong de dieu chinh gia");
        }

        BigDecimal minPrice = priceInfo.minPrice();
        BigDecimal maxPrice = priceInfo.maxPrice();

        if (minPrice == null || maxPrice == null) {
            throw new IllegalStateException(
                    "Khong lay duoc gia hien tai cua loai phong");
        }

        // 6. Tat ca phong cung loai phai dong gia
        if (minPrice.compareTo(maxPrice) != 0) {
            throw new IllegalStateException(
                    "Gia phong trong cung loai khong dong nhat. "
                            + "Can chuan hoa gia truoc khi lap ke hoach");
        }

        // 7. Lay gia cu tu database
        BigDecimal oldPrice = minPrice;

        // 8. Luu ke hoach voi trang thai Draft
        int planId = repository.insertDraft(
                request,
                oldPrice,
                managerId);

        // 9. Doc lai ke hoach vua tao
        PricePlanResponse result = repository.findById(planId)
                .orElseThrow(
                        () -> new IllegalStateException(
                                "Khong tim thay ke hoach vua tao"));

        // 10. Dam bao trang thai ban dau la Draft
        if (!"Draft".equals(result.status())) {
            throw new IllegalStateException(
                    "Trang thai ke hoach moi khong hop le");
        }

        return result;
    }

    // Kiem tra du lieu yeu cau
    private void validateRequest(
            CreatePricePlanRequest request) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Thong tin ke hoach khong duoc rong");
        }

        if (request.title() == null
                || request.title().isBlank()
                || request.title().trim().length() > 150) {

            throw new IllegalArgumentException(
                    "Ten ke hoach khong hop le");
        }

        if (request.categoryId() == null
                || request.categoryId() <= 0) {

            throw new IllegalArgumentException(
                    "ID loai phong khong hop le");
        }

        BigDecimal newPrice = request.newPrice();

        if (newPrice == null
                || newPrice.compareTo(BigDecimal.ZERO) <= 0
                || newPrice.scale() > 2
                || newPrice.compareTo(
                        new BigDecimal("9999999999.99")) > 0) {

            throw new IllegalArgumentException(
                    "Gia moi phai lon hon 0 va dung dinh dang DECIMAL(12,2)");
        }

        if (request.effectiveFrom() == null) {
            throw new IllegalArgumentException(
                    "Ngay bat dau khong duoc rong");
        }

        if (request.effectiveTo() != null
                && request.effectiveTo()
                        .isBefore(request.effectiveFrom())) {

            throw new IllegalArgumentException(
                    "Ngay ket thuc phai bang hoac sau ngay bat dau");
        }

        if (request.reason() == null
                || request.reason().isBlank()
                || request.reason().trim().length() > 500) {

            throw new IllegalArgumentException(
                    "Ly do dieu chinh gia khong hop le");
        }
    }

    // Loi khong co quyen
    public static class PricePlanForbiddenException
            extends RuntimeException {

        public PricePlanForbiddenException(String message) {
            super(message);
        }
    }

    // Loi khong tim thay loai phong
    public static class PricePlanNotFoundException
            extends RuntimeException {

        public PricePlanNotFoundException(String message) {
            super(message);
        }
    }
}
