
package com.example.demo.modules.occupancy.dto;

import java.math.BigDecimal;

public record OccupancyCategoryResponse(

        int categoryId,

        String categoryName,

        long availableRooms,

        long periodNights,

        long bookedNights,

        BigDecimal occupancyRatePercent,

        BigDecimal paidRevenue,

        BigDecimal minCurrentPrice,

        BigDecimal maxCurrentPrice

) {
}
