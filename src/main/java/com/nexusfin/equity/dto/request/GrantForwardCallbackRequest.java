package com.nexusfin.equity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record GrantForwardCallbackRequest(
        @NotBlank String requestId,
        @NotBlank String benefitOrderNo,
        @NotBlank String grantStatus,
        @NotNull @PositiveOrZero Long actualAmount,
        String loanOrderNo,
        String failReason,
        String grantTime,
        @NotNull Long timestamp
) {
}
