package com.nexusfin.equity.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record LoanCalculateRequest(
        @NotNull @Min(1) Long amount,
        @NotNull @Min(1) Integer term
) {
}
