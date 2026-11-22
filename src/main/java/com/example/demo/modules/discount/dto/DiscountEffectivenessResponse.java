
package com.example.demo.modules.discount.dto;

import java.math.BigDecimal;

public record DiscountEffectivenessResponse(

        int discountId,

        String code,

        String effectiveStatus,

        Integer usageLimit,

        long uses,

        Integer remainingUses,

        BigDecimal totalDiscount,

        BigDecimal netRevenue

) {
}
