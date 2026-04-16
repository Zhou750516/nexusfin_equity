package com.nexusfin.equity.dto.request;

import jakarta.validation.constraints.NotBlank;

public record JointLoginRequest(
        @NotBlank(message = "token must not be blank")
        String token,
        @NotBlank(message = "scene must not be blank")
        String scene,
        String orderNo,
        String benefitOrderNo,
        String productCode
) {
}
