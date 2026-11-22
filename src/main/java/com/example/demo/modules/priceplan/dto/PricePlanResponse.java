
package com.example.demo.modules.priceplan.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record PricePlanResponse(

        int planId,

        String title,

        int categoryId,

        BigDecimal oldPrice,

        BigDecimal newPrice,

        LocalDate effectiveFrom,

        LocalDate effectiveTo,

        String reason,

        String status,

        int createdBy,

        LocalDateTime createdAt

) {
}
