package com.nexusfin.equity.thirdparty.yunka;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BenefitOrderSyncRequest(
        String platformBenefitOrderNo,
        String benefitOrderNo,
        Long orderAmount,
        Integer status,
        Long createTime,
        Long payTime,
        Long expireTime,
        String memberPayType,
        String paymentNo,
        String benefitServiceProvider,
        @JsonProperty("benefiturl")
        String benefitUrl
) {
}
