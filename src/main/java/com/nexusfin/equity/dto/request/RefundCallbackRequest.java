package com.nexusfin.equity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RefundCallbackRequest(
        @NotBlank String requestId,
        @NotBlank String benefitOrderNo,
        @NotBlank String refundStatus,
        @NotNull @Positive Long refundAmount,
        String refundTime,
        String refundReason
) {
}
