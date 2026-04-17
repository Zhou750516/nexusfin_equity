package com.nexusfin.equity.dto.response;

public record BenefitDispatchContextResponse(
        String benefitOrderNo,
        String scene,
        String orderStatus,
        boolean allowRedirect,
        String redirectMode,
        String message
) {
}
