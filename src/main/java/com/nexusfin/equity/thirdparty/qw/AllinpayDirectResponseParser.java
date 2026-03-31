package com.nexusfin.equity.thirdparty.qw;

public interface AllinpayDirectResponseParser {

    <T> T parse(
            AllinpayDirectOperation operation,
            String serviceCode,
            AllinpayDirectVerifiedResponse verifiedResponse,
            Class<T> responseType
    );
}
