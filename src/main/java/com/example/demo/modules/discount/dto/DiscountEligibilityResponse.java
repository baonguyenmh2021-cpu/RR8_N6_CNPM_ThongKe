
package com.example.demo.modules.discount.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DiscountEligibilityResponse(

        int discountId,

        String code,

        String storedStatus,

        String effectiveStatus,

        LocalDate validFrom,

        LocalDate validTo,

        BigDecimal minAmount,

        BigDecimal orderAmount,

        Integer usageLimit,

        long uses,

        Integer remainingUses,

        boolean canApply,

        String reason

) {
}
