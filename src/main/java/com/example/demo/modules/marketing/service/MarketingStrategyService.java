
package com.example.demo.modules.marketing.service;

import com.example.demo.modules.marketing.dto.CreateMarketingStrategyRequest;
import com.example.demo.modules.marketing.dto.MarketingStrategyResponse;

import com.example.demo.modules.marketing.repository.MarketingStrategyRepository;
import com.example.demo.modules.marketing.repository.MarketingStrategyRepository.DiscountInfo;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class MarketingStrategyService {

    private final MarketingStrategyRepository repository;

    private static final BigDecimal MAX_BUDGET =
            new BigDecimal("999999999999.99");

    public MarketingStrategyService(
            MarketingStrategyRepository repository) {

        this.repository = repository;
    }

    // US-11: Manager tao chien luoc marketing
    @Transactional
    public MarketingStrategyResponse createStrategy(
            CreateMarketingStrategyRequest request,
            int managerId) {

        // 1. Kiem tra du lieu dau vao
        validateRequest(request);

        // 2. Kiem tra ID nguoi tao
        if (managerId <= 0) {
            throw new MarketingForbiddenException(
                    "Nguoi tao chien luoc khong hop le"
            );
        }

        // 3. Kiem tra vai tro Manager
        String role = repository.findUserRole(managerId)
                .orElseThrow(
                        () -> new MarketingForbiddenException(
                                "Khong tim thay nguoi tao chien luoc"
                        )
                );

        if (!"Manager".equals(role)) {
            throw new MarketingForbiddenException(
                    "Chi Manager duoc tao chien luoc marketing"
            );
        }

        // 4. Kiem tra ma giam gia neu co lien ket
        if (request.discountId() != null) {

            DiscountInfo discount =
                    repository.findDiscountById(
                            request.discountId()
                    ).orElseThrow(
                            () -> new MarketingDiscountNotFoundException(
                                    "Khong tim thay ma giam gia"
                            )
                    );

            // Ma phai dang Active
            if (!"Active".equals(discount.status())) {
                throw new MarketingDiscountConflictException(
                        "Ma giam gia da bi vo hieu hoa "
                        + "hoac khong o trang thai Active"
                );
            }

            // Ma phai bat dau truoc hoac dung ngay
            // bat dau chien luoc
            if (discount.validFrom()
                    .isAfter(request.startDate())) {

                throw new MarketingDiscountConflictException(
                        "Ma giam gia bat dau sau "
                        + "ngay bat dau chien luoc"
                );
            }

            // Ma phai ket thuc sau hoac dung ngay
            // ket thuc chien luoc
            if (discount.validTo()
                    .isBefore(request.endDate())) {

                throw new MarketingDiscountConflictException(
                        "Ma giam gia khong bao phu "
                        + "toan bo thoi gian chien luoc"
                );
            }
        }

        // 5. Luu vao database voi trang thai Planned
        int strategyId = repository.insertStrategy(
                request,
                managerId
        );

        // 6. Doc lai chien luoc vua tao
        MarketingStrategyResponse result =
                repository.findById(strategyId)
                        .orElseThrow(
                                () -> new IllegalStateException(
                                        "Khong tim thay chien luoc vua tao"
                                )
                        );

        // 7. Kiem tra trang thai ban dau
        if (!"Planned".equals(result.status())) {
            throw new IllegalStateException(
                    "Trang thai chien luoc moi khong hop le"
            );
        }

        return result;
    }

    // Kiem tra du lieu Request
    private void validateRequest(
            CreateMarketingStrategyRequest request) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Thong tin chien luoc khong duoc rong"
            );
        }

        // Kiem tra ten chien luoc
        if (request.title() == null
                || request.title().isBlank()
                || request.title().trim().length() > 150) {

            throw new IllegalArgumentException(
                    "Ten chien luoc khong hop le"
            );
        }

        // Doi tuong muc tieu la tuy chon
        if (request.targetSegment() != null
                && request.targetSegment().trim().length() > 150) {

            throw new IllegalArgumentException(
                    "Doi tuong muc tieu toi da 150 ky tu"
            );
        }

        // Kiem tra ngay
        if (request.startDate() == null
                || request.endDate() == null) {

            throw new IllegalArgumentException(
                    "Phai nhap ngay bat dau va ket thuc"
            );
        }

        if (request.endDate()
                .isBefore(request.startDate())) {

            throw new IllegalArgumentException(
                    "Ngay ket thuc phai bang "
                    + "hoac sau ngay bat dau"
            );
        }

        // Kiem tra ngan sach
        BigDecimal budget = request.budget();

        if (budget == null
                || budget.compareTo(BigDecimal.ZERO) < 0
                || budget.compareTo(MAX_BUDGET) > 0
                || budget.scale() > 2) {

            throw new IllegalArgumentException(
                    "Ngan sach phai >= 0 va "
                    + "dung dinh dang DECIMAL(14,2)"
            );
        }

        // Kiem tra ID ma giam gia neu co
        if (request.discountId() != null
                && request.discountId() <= 0) {

            throw new IllegalArgumentException(
                    "ID ma giam gia khong hop le"
            );
        }

        // Kiem tra booking ky vong
        if (request.expectedBookings() != null
                && request.expectedBookings() < 0) {

            throw new IllegalArgumentException(
                    "So booking ky vong khong duoc am"
            );
        }
    }

    // 403: Khong co quyen
    public static class MarketingForbiddenException
            extends RuntimeException {

        public MarketingForbiddenException(String message) {
            super(message);
        }
    }

    // 404: Ma giam gia khong ton tai
    public static class MarketingDiscountNotFoundException
            extends RuntimeException {

        public MarketingDiscountNotFoundException(
                String message) {
            super(message);
        }
    }

    // 409: Ma giam gia khong du dieu kien
    public static class MarketingDiscountConflictException
            extends RuntimeException {

        public MarketingDiscountConflictException(
                String message) {
            super(message);
        }
    }
}
