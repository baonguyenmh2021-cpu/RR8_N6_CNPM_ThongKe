
package com.example.demo.modules.marketing.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MarketingStrategySummaryResponse(

        int strategyId,

        String title,

        String targetSegment,

        LocalDate startDate,

        LocalDate endDate,

        BigDecimal budget,

        String status

) {
}
