package com.nexusfin.equity.thirdparty.qw;

import com.nexusfin.equity.exception.BizException;

public class AllinpayDirectSkeletonResponseParser implements AllinpayDirectResponseParser {

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
