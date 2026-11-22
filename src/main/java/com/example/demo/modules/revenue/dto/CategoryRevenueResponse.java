package com.example.demo.modules.revenue.dto;

import java.math.BigDecimal;
import java.util.List;

public record CategoryRevenueResponse(
        int year,
        Integer month,
        BigDecimal totalRevenue,
        List<CategoryRevenueItem> categories) {

    public record CategoryRevenueItem(
            int categoryId,
            String categoryName,
            BigDecimal revenue,
            long bookingCount) {
    }
}
