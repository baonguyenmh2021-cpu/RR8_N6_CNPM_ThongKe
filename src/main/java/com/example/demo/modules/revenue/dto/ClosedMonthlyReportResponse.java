
package com.example.demo.modules.revenue.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ClosedMonthlyReportResponse(
        int reportId,
        int year,
        int month,
        BigDecimal totalRevenue,
        BigDecimal totalDiscount,
        long bookingCount,
        long cancelledBookings,
        int generatedBy,
        LocalDateTime generatedAt) {
}
