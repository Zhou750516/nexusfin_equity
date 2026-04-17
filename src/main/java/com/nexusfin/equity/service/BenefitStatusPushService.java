package com.nexusfin.equity.service;

public interface BenefitStatusPushService {

    void pushStatus(BenefitStatusPushCommand command);

    record BenefitStatusPushCommand(
            String benefitOrderNo,
            String eventType,
            String statusAfter,
            String externalUserId,
            String statusBefore
    ) {
        public BenefitStatusPushCommand(String benefitOrderNo, String eventType, String statusAfter) {
            this(benefitOrderNo, eventType, statusAfter, null, null);
        }
    }
}
