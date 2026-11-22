
package com.example.demo.modules.marketing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ChangeMarketingStrategyStatusRequest(

        @NotBlank(message = "Trang thai moi khong duoc rong")

        @Pattern(regexp = "Planned|Running|Finished|Cancelled", message = "Trang thai phai la Planned, "
                + "Running, Finished hoac Cancelled")

        String status

) {
}
