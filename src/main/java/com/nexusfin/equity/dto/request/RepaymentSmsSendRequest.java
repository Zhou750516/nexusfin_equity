package com.nexusfin.equity.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RepaymentSmsSendRequest(
        @NotNull @Positive Integer loanId,
        @jakarta.validation.constraints.NotBlank String bankCardId
) {
}
