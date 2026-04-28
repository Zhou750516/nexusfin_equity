package com.nexusfin.equity.thirdparty.qw;

import com.nexusfin.equity.exception.BizException;

public class AllinpayDirectUnsupportedProtocolHandler
        implements AllinpayDirectHttpExecutor, AllinpayDirectResponseVerificationStage, AllinpayDirectResponseParser {

    @Override
    public AllinpayDirectRawResponse execute(AllinpayDirectTransportRequest transportRequest) {
        throw new BizException(
                "ALLINPAY_DIRECT_PROTOCOL_UNIMPLEMENTED",
                "Allinpay direct transport is not implemented for targetUri=" + transportRequest.targetUri()
        );
    }

    @Override
    public AllinpayDirectVerifiedResponse verify(AllinpayDirectRawResponse rawResponse) {
        throw new BizException(
                "ALLINPAY_DIRECT_PROTOCOL_UNIMPLEMENTED",
                "Allinpay direct response verification is not implemented"
        );
    }

    @Override
    public <T> T parse(
            AllinpayDirectOperation operation,
            String serviceCode,
            AllinpayDirectVerifiedResponse verifiedResponse,
            Class<T> responseType
    ) {
        throw new BizException(
                "ALLINPAY_DIRECT_PROTOCOL_UNIMPLEMENTED",
                "Allinpay direct response parsing is not implemented for "
                        + operation
                        + " with serviceCode="
                        + serviceCode
        );
    }
}
