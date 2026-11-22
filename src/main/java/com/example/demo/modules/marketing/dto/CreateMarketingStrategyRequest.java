
package com.example.demo.modules.marketing.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateMarketingStrategyRequest(

        @NotBlank(message = "Ten chien luoc khong duoc rong") @Size(max = 150, message = "Ten chien luoc toi da 150 ky tu") String title,

        @Size(max = 150, message = "Doi tuong muc tieu toi da 150 ky tu") String targetSegment,

        String description,

        @NotNull(message = "Ngay bat dau khong duoc rong") LocalDate startDate,

        @NotNull(message = "Ngay ket thuc khong duoc rong") LocalDate endDate,

        @NotNull(message = "Ngan sach khong duoc rong") @DecimalMin(value = "0.0", message = "Ngan sach khong duoc am") @Digits(integer = 12, fraction = 2, message = "Ngan sach khong dung dinh dang") BigDecimal budget,

        @Positive(message = "ID ma giam gia phai lon hon 0") Integer discountId,

        @Min(value = 0, message = "So booking ky vong khong duoc am") Integer expectedBookings

) {
}
