
package com.example.demo.modules.revenue.dto;

import java.math.BigDecimal;
import java.util.List;

public record YearlyRevenueResponse(
        int year,
        BigDecimal totalRevenue,
        List<MonthlyRevenueItem> months,
        List<Integer> highestRevenueMonths) {
    public record MonthlyRevenueItem(
            int month,
            BigDecimal revenue) {
    }
}
