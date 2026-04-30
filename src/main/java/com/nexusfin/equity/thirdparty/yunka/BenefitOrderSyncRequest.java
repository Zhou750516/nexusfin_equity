package com.nexusfin.equity.thirdparty.yunka;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BenefitOrderSyncRequest(
        String userId,
        String platformBenefitOrderNo,
        String benefitStatus,
        Long benefitAmount,
        @JsonProperty("benefiturl")
        String benefitUrl
) {
}
