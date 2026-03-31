package com.nexusfin.equity.thirdparty.qw;

import com.nexusfin.equity.exception.BizException;

public class AllinpayDirectSkeletonResponseVerificationStage implements AllinpayDirectResponseVerificationStage {

    @Override
    public AllinpayDirectVerifiedResponse verify(AllinpayDirectRawResponse rawResponse) {
        throw new BizException(
                "ALLINPAY_DIRECT_PROTOCOL_UNIMPLEMENTED",
                "Allinpay direct response verification is not implemented"
        );
    }
}
