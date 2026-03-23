package com.nexusfin.equity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record RepaymentForwardCallbackRequest(
        @NotBlank String requestId,
        @NotBlank String benefitOrderNo,
        @NotBlank String loanOrderNo,
        @NotNull @Positive Integer termNo,
        @NotBlank String repaymentStatus,
        @PositiveOrZero Long paidAmount,
        String paidTime,
        @PositiveOrZero Integer overdueDays,
        @NotNull Long timestamp
) {
}
