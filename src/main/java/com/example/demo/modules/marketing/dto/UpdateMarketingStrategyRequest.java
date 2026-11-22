
package com.example.demo.modules.marketing.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateMarketingStrategyRequest(

        // Ten chien luoc - bat buoc
        @NotBlank(message = "Ten chien luoc khong duoc rong") @Size(max = 150, message = "Ten toi da 150 ky tu") String title,

        // Doi tuong muc tieu - tuy chon
        @Size(max = 150, message = "Doi tuong muc tieu toi da 150 ky tu") String targetSegment,

        // Mo ta chien luoc - tuy chon
        String description,

        // Ngay bat dau - bat buoc
        @NotNull(message = "Phai nhap ngay bat dau") LocalDate startDate,

        // Ngay ket thuc - bat buoc
        @NotNull(message = "Phai nhap ngay ket thuc") LocalDate endDate,

        // Ngan sach du kien
        @NotNull(message = "Phai nhap ngan sach") @DecimalMin(value = "0.0", message = "Ngan sach khong duoc am") @Digits(integer = 12, fraction = 2, message = "Ngan sach phai dung DECIMAL(14,2)") BigDecimal budget,

        // Chi phi thuc te
        @NotNull(message = "Phai nhap chi phi thuc te") @DecimalMin(value = "0.0", message = "Chi phi thuc te khong duoc am") @Digits(integer = 12, fraction = 2, message = "Chi phi thuc te phai dung DECIMAL(14,2)") BigDecimal actualSpend,

        // Ma giam gia lien ket - tuy chon
        @Positive(message = "ID ma giam gia phai lon hon 0") Integer discountId,

        // Booking ky vong - tuy chon
        @Min(value = 0, message = "Booking ky vong khong duoc am") Integer expectedBookings

) {
}
