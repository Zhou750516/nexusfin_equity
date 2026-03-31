package com.nexusfin.equity.thirdparty.qw;

import com.nexusfin.equity.exception.BizException;

public class AllinpayDirectSkeletonHttpExecutor implements AllinpayDirectHttpExecutor {

    @Override
    public AllinpayDirectRawResponse execute(AllinpayDirectTransportRequest transportRequest) {
        throw new BizException(
                "ALLINPAY_DIRECT_PROTOCOL_UNIMPLEMENTED",
                "Allinpay direct transport is not implemented for targetUri=" + transportRequest.targetUri()
        );
    }
}
