package com.nexusfin.equity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateBenefitOrderRequest(
        @NotBlank String requestId,
        @NotBlank String productCode,
        @NotNull @Positive Long loanAmount,
        @Positive Long benefitAmount,
        @NotNull Boolean agreementSigned
) {
    public CreateBenefitOrderRequest(
            String requestId,
            String productCode,
            Long loanAmount,
            Boolean agreementSigned
    ) {
        this(requestId, productCode, loanAmount, null, agreementSigned);
    }
}
