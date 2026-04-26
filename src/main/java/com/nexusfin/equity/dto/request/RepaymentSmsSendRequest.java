package com.nexusfin.equity.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RepaymentSmsSendRequest(
        @NotBlank String loanId,
        @NotBlank String bankCardId
) {
}
