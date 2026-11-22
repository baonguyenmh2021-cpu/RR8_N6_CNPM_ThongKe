
package com.example.demo.modules.priceplan.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ReviewPricePlanResponse(

        int planId,

        String title,

        int categoryId,

        LocalDate effectiveFrom,

        LocalDate effectiveTo,

        String status,

        int createdBy,

        Integer approvedBy,

        LocalDateTime approvedAt,

        String rejectReason

) {
}