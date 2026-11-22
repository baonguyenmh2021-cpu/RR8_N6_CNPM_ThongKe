
package com.example.demo.modules.discount.service;

import com.example.demo.modules.discount.dto.DiscountEffectivenessResponse;
import com.example.demo.modules.discount.repository.DiscountEffectivenessRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class DiscountEffectivenessService {

    private final DiscountEffectivenessRepository repository;

    public DiscountEffectivenessService(
            DiscountEffectivenessRepository repository) {

        this.repository = repository;
    }

    // US-06: Thong ke hieu qua ma chiet khau
    @Transactional(readOnly = true)
    public List<DiscountEffectivenessResponse> getEffectiveness(
            LocalDate from,
            LocalDate to) {

        // 1. Kiem tra chi nhap mot ngay
        if ((from == null) != (to == null)) {
            throw new IllegalArgumentException(
                    "Phai nhap ca from va to, hoac bo trong ca hai");
        }

        // 2. Kiem tra khoang thoi gian
        if (from != null && !to.isAfter(from)) {
            throw new IllegalArgumentException(
                    "Ngay ket thuc phai sau ngay bat dau");
        }

        // 3. Goi Repository lay du lieu
        return repository.findEffectiveness(from, to);
    }
}
