
package com.example.demo.modules.marketing.service;

import com.example.demo.modules.marketing.dto.MarketingEffectivenessResponse;

import com.example.demo.modules.marketing.repository.MarketingEffectivenessRepository;
import com.example.demo.modules.marketing.repository.MarketingEffectivenessRepository.MarketingMetrics;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class MarketingEffectivenessService {

    private final MarketingEffectivenessRepository repository;

    public MarketingEffectivenessService(
            MarketingEffectivenessRepository repository) {

        this.repository = repository;
    }

    // US-12: Danh gia hieu qua mot chien luoc
    @Transactional(readOnly = true)
    public MarketingEffectivenessResponse getEffectiveness(
            int strategyId) {

        // 1. Kiem tra ID chien luoc
        if (strategyId <= 0) {
            throw new IllegalArgumentException(
                    "ID chien luoc khong hop le");
        }

        // 2. Lay du lieu thong ke tu Repository
        MarketingMetrics metrics = repository.findMetricsByStrategyId(strategyId)
                .orElseThrow(
                        () -> new MarketingStrategyNotFoundException(
                                "Khong tim thay chien luoc marketing"));

        // 3. Kiem tra du lieu thong ke
        validateMetrics(metrics);

        // 4. Tinh phan tram tang/giam doanh thu
        BigDecimal revenueChangePercent = null;

        if (metrics.revenueBefore()
                .compareTo(BigDecimal.ZERO) > 0) {

            BigDecimal revenueDifference = metrics.revenueDuring()
                    .subtract(metrics.revenueBefore());

            revenueChangePercent = revenueDifference
                    .multiply(new BigDecimal("100"))
                    .divide(
                            metrics.revenueBefore(),
                            2,
                            RoundingMode.HALF_UP);
        }

        // 5. Tinh ty le dat muc tieu booking
        BigDecimal bookingAchievementPercent = null;

        Integer expectedBookings = metrics.expectedBookings();

        if (expectedBookings != null
                && expectedBookings > 0) {

            bookingAchievementPercent = BigDecimal.valueOf(
                    metrics.bookingsDuring())
                    .multiply(new BigDecimal("100"))
                    .divide(
                            BigDecimal.valueOf(expectedBookings),
                            2,
                            RoundingMode.HALF_UP);
        }

        // 6. Tao DTO tra ket qua
        return new MarketingEffectivenessResponse(
                metrics.strategyId(),
                metrics.title(),
                metrics.status(),
                metrics.startDate(),
                metrics.endDate(),
                metrics.budget(),
                metrics.actualSpend(),
                metrics.discountId(),
                metrics.expectedBookings(),
                metrics.revenueBefore(),
                metrics.revenueDuring(),
                metrics.bookingsBefore(),
                metrics.bookingsDuring(),
                revenueChangePercent,
                bookingAchievementPercent,
                metrics.bookingsWithCode());
    }

    // Kiem tra tinh nhat quan cua du lieu
    private void validateMetrics(MarketingMetrics metrics) {

        if (metrics.startDate() == null
                || metrics.endDate() == null
                || metrics.endDate().isBefore(metrics.startDate())) {

            throw new IllegalStateException(
                    "Khoang thoi gian chien luoc khong hop le");
        }

        if (metrics.revenueBefore() == null
                || metrics.revenueDuring() == null
                || metrics.revenueBefore()
                        .compareTo(BigDecimal.ZERO) < 0
                || metrics.revenueDuring()
                        .compareTo(BigDecimal.ZERO) < 0) {

            throw new IllegalStateException(
                    "Du lieu doanh thu khong hop le");
        }

        if (metrics.budget() == null
                || metrics.actualSpend() == null
                || metrics.budget().compareTo(BigDecimal.ZERO) < 0
                || metrics.actualSpend()
                        .compareTo(BigDecimal.ZERO) < 0) {

            throw new IllegalStateException(
                    "Du lieu chi phi chien luoc khong hop le");
        }

        if (metrics.bookingsBefore() < 0
                || metrics.bookingsDuring() < 0
                || metrics.bookingsWithCode() < 0
                || metrics.bookingsWithCode() > metrics.bookingsDuring()) {

            throw new IllegalStateException(
                    "Du lieu so booking khong hop le");
        }

        if (metrics.expectedBookings() != null
                && metrics.expectedBookings() < 0) {

            throw new IllegalStateException(
                    "So booking ky vong khong hop le");
        }

        if (metrics.discountId() == null
                && metrics.bookingsWithCode() != 0) {

            throw new IllegalStateException(
                    "Chien luoc khong co ma giam gia "
                            + "nhung lai co booking dung ma");
        }
    }

    // 404: Khong tim thay chien luoc
    public static class MarketingStrategyNotFoundException
            extends RuntimeException {

        public MarketingStrategyNotFoundException(
                String message) {

            super(message);
        }
    }
}
