
package com.example.demo.modules.discount.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

public record ExtendDiscountRequest(

        @NotNull(message = "Ngay het han moi khong duoc de trong") LocalDate newValidTo

) {
}
