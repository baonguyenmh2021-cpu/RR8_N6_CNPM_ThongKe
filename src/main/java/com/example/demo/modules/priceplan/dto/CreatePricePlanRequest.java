
package com.example.demo.modules.priceplan.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.*;

public record CreatePricePlanRequest(

        @NotBlank(message = "Ten ke hoach khong duoc rong") @Size(max = 150, message = "Ten ke hoach toi da 150 ky tu") String title,

        @NotNull(message = "Loai phong khong duoc rong") @Positive(message = "ID loai phong phai lon hon 0") Integer categoryId,

        @NotNull(message = "Gia moi khong duoc rong") @DecimalMin(value = "0.01", message = "Gia moi phai lon hon 0") @Digits(integer = 10, fraction = 2, message = "Gia moi khong dung dinh dang") BigDecimal newPrice,

        @NotNull(message = "Ngay bat dau khong duoc rong") LocalDate effectiveFrom,

        LocalDate effectiveTo,

        @NotBlank(message = "Ly do khong duoc rong") @Size(max = 500, message = "Ly do toi da 500 ky tu") String reason

) {
}
