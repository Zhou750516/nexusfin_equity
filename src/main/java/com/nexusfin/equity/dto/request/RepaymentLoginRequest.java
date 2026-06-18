package com.nexusfin.equity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RepaymentLoginRequest(
        @NotBlank(message = "token must not be blank")
        String token,
        @NotNull(message = "loanId must not be null")
        @Positive(message = "loanId must be positive")
        Integer loanId
) {
}
