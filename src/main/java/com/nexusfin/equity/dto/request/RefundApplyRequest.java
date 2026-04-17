package com.nexusfin.equity.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RefundApplyRequest(
        @NotBlank String benefitOrderNo,
        @NotBlank String reason
) {
}
