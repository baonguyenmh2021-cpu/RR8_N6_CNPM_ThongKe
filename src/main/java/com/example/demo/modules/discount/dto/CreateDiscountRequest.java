
package com.example.demo.modules.discount.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.*;

public record CreateDiscountRequest(

        @NotBlank(message = "Ma giam gia khong duoc rong") @Pattern(regexp = "^[A-Za-z0-9_]{3,30}$", message = "Ma phai co 3-30 ky tu chu, so hoac _") String code,

        @Size(max = 255, message = "Mo ta toi da 255 ky tu") String description,

        @NotNull(message = "Loai giam gia khong duoc rong") @Pattern(regexp = "^(PERCENT|FIXED)$", message = "Loai phai la PERCENT hoac FIXED") String type,

        @NotNull(message = "Gia tri giam gia khong duoc rong") @DecimalMin(value = "0.01", message = "Gia tri phai lon hon 0") @Digits(integer = 10, fraction = 2) BigDecimal value,

        @DecimalMin(value = "0.00", message = "Don toi thieu khong duoc am") @Digits(integer = 10, fraction = 2) BigDecimal minAmount,

        @NotNull(message = "Ngay bat dau khong duoc rong") LocalDate validFrom,

        @NotNull(message = "Ngay ket thuc khong duoc rong") LocalDate validTo,

        @Positive(message = "Gioi han luot dung phai lon hon 0") Integer usageLimit

) {
}
