
package com.example.demo.modules.marketing.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record MarketingStrategyResponse(

        int strategyId,

        String title,

        String targetSegment,

        String description,

        LocalDate startDate,

        LocalDate endDate,

        BigDecimal budget,

        BigDecimal actualSpend,

        Integer discountId,

        Integer expectedBookings,

        String status,

        int createdBy,

        LocalDateTime createdAt,

        LocalDateTime updatedAt

) {
}
