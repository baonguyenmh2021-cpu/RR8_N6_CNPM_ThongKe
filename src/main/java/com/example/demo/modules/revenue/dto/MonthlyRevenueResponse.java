
package com.example.demo.modules.revenue.dto;

import java.math.BigDecimal;

public record MonthlyRevenueResponse(
        int year,
        int month,
        BigDecimal revenue,
        long bookingCount,
        BigDecimal totalDiscount,
        long cancelledBookingsCreatedInMonth,
        BigDecimal previousMonthRevenue,
        BigDecimal revenueChangePercent) {
}
