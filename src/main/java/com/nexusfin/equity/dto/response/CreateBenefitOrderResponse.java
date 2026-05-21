package com.nexusfin.equity.dto.response;

public record CreateBenefitOrderResponse(
        String benefitOrderNo,
        String orderStatus,
        String redirectUrl,
        String qwOrderNo,
        Long createTime,
        Long payTime,
        Long expireTime
) {
    public CreateBenefitOrderResponse(
            String benefitOrderNo,
            String orderStatus,
            String redirectUrl
    ) {
        this(benefitOrderNo, orderStatus, redirectUrl, null, null, null, null);
    }
}
