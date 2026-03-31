package com.nexusfin.equity.thirdparty.qw;

public record AllinpayDirectEnvelopeHead(
        String serviceCode,
        String merchantId,
        String userName,
        String userPassword,
        String timestamp
) {
}
