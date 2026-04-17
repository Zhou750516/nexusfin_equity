package com.nexusfin.equity.dto.request;

import jakarta.validation.constraints.NotBlank;

public record BenefitDispatchResolveRequest(
        @NotBlank String benefitOrderNo
) {
}
