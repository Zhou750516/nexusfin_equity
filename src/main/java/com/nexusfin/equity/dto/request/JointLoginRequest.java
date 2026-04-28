package com.nexusfin.equity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record JointLoginRequest(
        @NotBlank(message = "token must not be blank")
        String token,
        @NotBlank(message = "scene must not be blank")
        @Pattern(
                regexp = "^(?i)(push|exercise|refund)$",
                message = "must be one of [push, exercise, refund]"
        )
        String scene,
        String orderNo,
        String benefitOrderNo,
        String productCode
) {
}
