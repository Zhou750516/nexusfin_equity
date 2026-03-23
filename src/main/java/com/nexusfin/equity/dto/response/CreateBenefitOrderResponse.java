package com.nexusfin.equity.dto.response;

public record CreateBenefitOrderResponse(
        String benefitOrderNo,
        String orderStatus,
        String redirectUrl
) {
}
