package com.nexusfin.equity.dto.response;

public record BenefitOrderStatusResponse(
        String benefitOrderNo,
        String orderStatus,
        String qwFirstDeductStatus,
        String qwFallbackDeductStatus,
        String qwExerciseStatus,
        String grantStatus
) {
}
