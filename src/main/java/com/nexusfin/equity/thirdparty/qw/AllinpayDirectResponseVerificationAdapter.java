package com.nexusfin.equity.thirdparty.qw;

import com.nexusfin.equity.exception.BizException;

public class AllinpayDirectResponseVerificationAdapter implements AllinpayDirectResponseVerificationStage {

    private final AllinpayDirectResponseSignatureResolver signatureResolver;
    private final AllinpayResponseVerifier responseVerifier;

    public AllinpayDirectResponseVerificationAdapter(
            AllinpayDirectResponseSignatureResolver signatureResolver,
            AllinpayResponseVerifier responseVerifier
    ) {
        this.signatureResolver = signatureResolver;
        this.responseVerifier = responseVerifier;
    }

    @Override
    public AllinpayDirectVerifiedResponse verify(AllinpayDirectRawResponse rawResponse) {
        String signature = signatureResolver.resolve(rawResponse);
        if (!responseVerifier.verify(rawResponse.responseBody(), signature)) {
            throw new BizException(
                    "ALLINPAY_DIRECT_RESPONSE_SIGNATURE_INVALID",
                    "Invalid allinpay direct response signature"
            );
        }
        return new AllinpayDirectVerifiedResponse(
                rawResponse.httpStatus(),
                rawResponse.responseBody(),
                signature
        );
    }
}
