
package com.example.demo.modules.marketing.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MarketingEffectivenessResponse(

        int strategyId,

        String title,

        String status,

        LocalDate startDate,

        LocalDate endDate,

        BigDecimal budget,

        BigDecimal actualSpend,

        Integer discountId,

        Integer expectedBookings,

        BigDecimal revenueBefore,

        BigDecimal revenueDuring,

        long bookingsBefore,

        long bookingsDuring,

        BigDecimal revenueChangePercent,

        BigDecimal bookingAchievementPercent,

        long bookingsWithCode

) {
}
