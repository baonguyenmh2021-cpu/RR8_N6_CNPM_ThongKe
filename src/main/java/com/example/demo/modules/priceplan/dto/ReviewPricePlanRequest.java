
package com.example.demo.modules.priceplan.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewPricePlanRequest(

        @NotNull(message = "Phai chon duyet hoac tu choi") Boolean approve,

        @Size(max = 500, message = "Ly do tu choi toi da 500 ky tu") String rejectReason

) {
}
