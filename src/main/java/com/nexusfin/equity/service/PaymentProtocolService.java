package com.nexusfin.equity.service;

import com.nexusfin.equity.entity.BenefitOrder;
import java.time.LocalDateTime;

public interface PaymentProtocolService {

    void saveActiveProtocol(SavePaymentProtocolCommand command);

    ResolvedPaymentProtocol resolveForBenefitOrder(BenefitOrder order);

    record SavePaymentProtocolCommand(
            String memberId,
            String externalUserId,
            String providerCode,
            String protocolNo,
            String signRequestNo,
            String channelCode,
            LocalDateTime signedTs
    ) {
    }

    record ResolvedPaymentProtocol(String protocolNo, String source) {
    }
}
