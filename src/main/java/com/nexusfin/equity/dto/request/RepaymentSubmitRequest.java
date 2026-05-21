package com.nexusfin.equity.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record RepaymentSubmitRequest(
        @NotNull @Positive Integer loanId,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @jakarta.validation.constraints.NotBlank String bankCardId,
        @jakarta.validation.constraints.NotBlank String repaymentType
) {
}
