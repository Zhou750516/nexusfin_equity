package com.nexusfin.equity.thirdparty.qw;

public class AllinpayDirectRequestPreparer {

    private final AllinpayDirectProtocolSerializer protocolSerializer;
    private final AllinpayRequestSigner requestSigner;

    public AllinpayDirectRequestPreparer(
            AllinpayDirectProtocolSerializer protocolSerializer,
            AllinpayRequestSigner requestSigner
    ) {
        this.protocolSerializer = protocolSerializer;
        this.requestSigner = requestSigner;
    }

    public AllinpayDirectPreparedRequest prepare(AllinpayDirectEnvelope envelope) {
        AllinpayDirectSerializedRequest serializedRequest = protocolSerializer.serialize(envelope);
        return new AllinpayDirectPreparedRequest(
                serializedRequest.targetUri(),
                serializedRequest.contentType(),
                serializedRequest.requestBody(),
                requestSigner.sign(serializedRequest.signingPayload())
        );
    }
}
