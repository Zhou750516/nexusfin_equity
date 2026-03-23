package com.nexusfin.equity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateBenefitOrderRequest(
        @NotBlank String memberId,
        @NotBlank String productCode,
        @NotNull @Positive Long loanAmount,
        @NotNull Boolean agreementSigned
) {
}
