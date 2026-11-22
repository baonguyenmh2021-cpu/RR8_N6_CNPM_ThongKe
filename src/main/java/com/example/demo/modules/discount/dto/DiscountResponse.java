
package com.example.demo.modules.discount.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record DiscountResponse(
        int discountId,
        String code,
        String description,
        String type,
        BigDecimal value,
        BigDecimal minAmount,
        LocalDate validFrom,
        LocalDate validTo,
        Integer usageLimit,
        String status,
        int createdBy,
        LocalDateTime createdAt) {
}
