package com.nexusfin.equity.dto.response;

public record BenefitDispatchResolveResponse(
        String benefitOrderNo,
        boolean allowRedirect,
        String redirectMode,
        String supplierUrl,
        String message
) {
}
