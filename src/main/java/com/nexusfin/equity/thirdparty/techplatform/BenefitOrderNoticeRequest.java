package com.nexusfin.equity.thirdparty.techplatform;

public record BenefitOrderNoticeRequest(
        String eventId,
        String benefitOrderNo,
        String eventType,
        String statusAfter,
        String externalUserId,
        String statusBefore
) {
}
