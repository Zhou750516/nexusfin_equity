package com.nexusfin.equity.dto.response;

public record BenefitOrderStatusResponse(
        String benefitOrderNo,
        String orderStatus,
        String firstDeductStatus,
        String fallbackDeductStatus,
        String exerciseStatus,
        String grantStatus
) {
}
