package com.nexusfin.equity.dto.response;

public record BenefitsActivateResponse(
        String activationId,
        String status,
        String message
) {
}
