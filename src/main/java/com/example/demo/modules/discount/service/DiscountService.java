
package com.example.demo.modules.discount.service;

import com.example.demo.modules.discount.dto.CreateDiscountRequest;
import com.example.demo.modules.discount.dto.DiscountResponse;
import com.example.demo.modules.discount.repository.DiscountRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class DiscountService {

    private final DiscountRepository discountRepository;

    public DiscountService(DiscountRepository discountRepository) {
        this.discountRepository = discountRepository;
    }

    // US-05: Tao ma giam gia
    @Transactional
    public DiscountResponse createDiscount(
            CreateDiscountRequest request,
            int managerId) {

        // 1. Kiem tra nguoi tao
        if (managerId <= 0) {
            throw new IllegalArgumentException(
                    "Nguoi tao khong hop le");
        }

        // 2. Kiem tra ma giam gia
        if (request.code() == null ||
                !request.code().matches("[A-Za-z0-9_]{3,30}")) {

            throw new IllegalArgumentException(
                    "Ma phai co 3-30 ky tu chu, so hoac _");
        }

        // 3. Kiem tra loai chiet khau
        if (!"PERCENT".equals(request.type()) &&
                !"FIXED".equals(request.type())) {

            throw new IllegalArgumentException(
                    "Loai phai la PERCENT hoac FIXED");
        }

        // 4. Kiem tra gia tri
        if (request.value() == null ||
                request.value().compareTo(BigDecimal.ZERO) <= 0) {

            throw new IllegalArgumentException(
                    "Gia tri phai lon hon 0");
        }

        // 5. Kiem tra so chu so phu hop DECIMAL(12,2)
        BigDecimal maxValue = new BigDecimal("9999999999.99");

        if (request.value().scale() > 2 ||
                request.value().compareTo(maxValue) > 0) {

            throw new IllegalArgumentException(
                    "Gia tri khong dung dinh dang tien");
        }

        // 6. Neu la PERCENT thi khong vuot 100
        if ("PERCENT".equals(request.type()) &&
                request.value().compareTo(
                        new BigDecimal("100")) > 0) {

            throw new IllegalArgumentException(
                    "Phan tram giam khong duoc vuot 100");
        }

        // 7. Kiem tra don hang toi thieu
        if (request.minAmount() != null &&
                (request.minAmount().compareTo(BigDecimal.ZERO) < 0 ||
                        request.minAmount().scale() > 2 ||
                        request.minAmount().compareTo(maxValue) > 0)) {

            throw new IllegalArgumentException(
                    "Don hang toi thieu khong hop le");
        }

        // 8. Kiem tra ngay
        if (request.validFrom() == null ||
                request.validTo() == null ||
                request.validTo().isBefore(request.validFrom())) {

            throw new IllegalArgumentException(
                    "Ngay ket thuc phai >= ngay bat dau");
        }

        // 9. Kiem tra gioi han luot dung
        if (request.usageLimit() != null &&
                request.usageLimit() <= 0) {

            throw new IllegalArgumentException(
                    "Gioi han luot dung phai lon hon 0");
        }

        // 10. Kiem tra ma trung
        if (discountRepository.existsByCode(request.code())) {
            throw new DuplicateDiscountCodeException(
                    "Ma giam gia da ton tai");
        }

        // 11. Luu vao database
        int discountId;

        try {
            discountId = discountRepository.insertDiscount(
                    request, managerId);

        } catch (DataIntegrityViolationException ex) {

            // Xu ly truong hop hai yeu cau tao trung ma
            if (discountRepository.existsByCode(request.code())) {
                throw new DuplicateDiscountCodeException(
                        "Ma giam gia da ton tai");
            }

            // Loi rang buoc khac thi giu nguyen
            throw ex;
        }

        // 12. Doc lai ket qua vua tao
        return discountRepository.findById(discountId)
                .orElseThrow(() -> new IllegalStateException(
                        "Khong tim thay ma vua tao"));
    }

    // Ngoai le rieng cho truong hop trung ma
    public static class DuplicateDiscountCodeException
            extends RuntimeException {

        public DuplicateDiscountCodeException(String message) {
            super(message);
        }
    }
}
