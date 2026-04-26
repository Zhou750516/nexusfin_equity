package com.nexusfin.equity.thirdparty.yunka;

public record BenefitOrderSyncRequest(
        String userId,
        String platformBenefitOrderNo,
        String benefitStatus,
        Long benefitAmount
) {
}
