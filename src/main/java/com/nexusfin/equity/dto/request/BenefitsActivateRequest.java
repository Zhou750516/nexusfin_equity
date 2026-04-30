package com.nexusfin.equity.dto.request;

import jakarta.validation.constraints.NotBlank;

public record BenefitsActivateRequest(
        @NotBlank String applicationId,
        @NotBlank String cardType,
        @NotBlank(message = "token must not be blank")
        String token
) {
}
