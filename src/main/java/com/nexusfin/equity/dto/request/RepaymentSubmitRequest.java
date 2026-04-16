package com.nexusfin.equity.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record RepaymentSubmitRequest(
        @NotBlank String loanId,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotBlank String bankCardId,
        @NotBlank String repaymentType
) {
}
