package com.nexusfin.equity.service;

public interface TechPlatformBenefitStatusClient {

    void push(BenefitStatusPushPayload payload);

    record BenefitStatusPushPayload(
            String eventId,
            String benefitOrderNo,
            String eventType,
            String statusAfter,
            String externalUserId,
            String statusBefore
    ) {
    }
}
