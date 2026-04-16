package com.nexusfin.equity.service;

public interface PaymentProtocolCallbackService {

    void handleCallback(PaymentProtocolCallbackCommand command);

    record PaymentProtocolCallbackCommand(
            String requestId,
            String memberId,
            String externalUserId,
            String providerCode,
            String protocolNo,
            String protocolStatus,
            String signRequestNo,
            String channelCode,
            String signedTs
    ) {
    }
}
