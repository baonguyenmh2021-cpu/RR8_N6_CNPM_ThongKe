
package com.example.demo.modules.occupancy.service;

import com.example.demo.modules.occupancy.dto.OccupancyCategoryResponse;
import com.example.demo.modules.occupancy.repository.OccupancyRepository;
import com.example.demo.modules.occupancy.repository.OccupancyRepository.OccupancyMetrics;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class OccupancyService {

    private final OccupancyRepository repository;

    public OccupancyService(OccupancyRepository repository) {
        this.repository = repository;
    }

    // US-08: Thong ke ty le lap day theo loai phong
    @Transactional(readOnly = true)
    public List<OccupancyCategoryResponse> getOccupancy(
            LocalDate from,
            LocalDate to) {

        // 1. Kiem tra ngay khong duoc rong
        if (from == null || to == null) {
            throw new IllegalArgumentException(
                    "Phai nhap day du ngay bat dau va ket thuc");
        }

        // 2. Ngay ket thuc phai sau ngay bat dau
        if (!to.isAfter(from)) {
            throw new IllegalArgumentException(
                    "Ngay ket thuc phai sau ngay bat dau");
        }

        // 3. Lay du lieu tu Repository
        List<OccupancyMetrics> metrics = repository.findMetrics(from, to);

        List<OccupancyCategoryResponse> results = new ArrayList<>();

        // 4. Tinh ty le lap day cho tung loai phong
        for (OccupancyMetrics item : metrics) {

            long availableRooms = item.availableRooms();
            long periodNights = item.periodNights();
            long bookedNights = item.bookedNights();

            // Tong so dem phong co the cung cap
            long capacity = Math.multiplyExact(
                    availableRooms,
                    periodNights);

            // 5. Kiem tra du lieu bat thuong
            if (availableRooms < 0 ||
                    periodNights <= 0 ||
                    bookedNights < 0 ||
                    bookedNights > capacity) {

                throw new IllegalStateException(
                        "Du lieu so dem dat phong khong hop le: "
                                + item.categoryName());
            }

            BigDecimal occupancyRate = null;

            // 6. Chi tinh ty le neu co phong kha dung
            if (capacity > 0) {

                occupancyRate = BigDecimal
                        .valueOf(bookedNights)
                        .multiply(new BigDecimal("100"))
                        .divide(
                                BigDecimal.valueOf(capacity),
                                2,
                                RoundingMode.HALF_UP);
            }

            // 7. Tao DTO tra ve cho API
            OccupancyCategoryResponse response = new OccupancyCategoryResponse(
                    item.categoryId(),
                    item.categoryName(),
                    availableRooms,
                    periodNights,
                    bookedNights,
                    occupancyRate,
                    item.paidRevenue(),
                    item.minCurrentPrice(),
                    item.maxCurrentPrice());

            results.add(response);
        }

        return results;
    }
}
