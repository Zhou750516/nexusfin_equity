package com.nexusfin.equity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RepaymentSmsConfirmRequest(
        @NotNull @Positive Integer loanId,
        @NotBlank String captcha
) {
}
