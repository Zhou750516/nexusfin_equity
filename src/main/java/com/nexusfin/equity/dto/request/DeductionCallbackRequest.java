package com.nexusfin.equity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record DeductionCallbackRequest(
        @NotBlank String requestId,
        @NotBlank String benefitOrderNo,
        @NotBlank String qwTradeNo,
        @NotBlank String deductStatus,
        @NotNull @Positive Long deductAmount,
        String failReason,
        String deductTime
) {
}
