package com.nexusfin.equity.dto.request;

import jakarta.validation.constraints.NotBlank;

public record BenefitRedirectUrlRequest(
        @NotBlank(message = "token must not be blank")
        String token,
        @NotBlank(message = "benefitOrderNo must not be blank")
        String benefitOrderNo
) {
}
