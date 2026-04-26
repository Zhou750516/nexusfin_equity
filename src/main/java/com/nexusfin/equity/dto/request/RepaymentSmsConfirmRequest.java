package com.nexusfin.equity.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RepaymentSmsConfirmRequest(
        @NotBlank String loanId,
        @NotBlank String captcha
) {
}
