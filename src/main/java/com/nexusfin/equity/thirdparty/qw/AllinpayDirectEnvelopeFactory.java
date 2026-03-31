package com.nexusfin.equity.thirdparty.qw;

import com.fasterxml.jackson.databind.JsonNode;

public class AllinpayDirectEnvelopeFactory {

    public AllinpayDirectEnvelope create(
            AllinpayDirectInvocation invocation,
            JsonNode businessPayload,
            String timestamp
    ) {
        return new AllinpayDirectEnvelope(
                invocation.operation(),
                invocation.targetUri(),
                new AllinpayDirectEnvelopeHead(
                        invocation.serviceCode(),
                        invocation.merchantId(),
                        invocation.userName(),
                        invocation.userPassword(),
                        timestamp
                ),
                businessPayload
        );
    }
}
