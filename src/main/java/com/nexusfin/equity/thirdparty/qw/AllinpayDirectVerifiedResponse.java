package com.nexusfin.equity.thirdparty.qw;

public record AllinpayDirectVerifiedResponse(
        int httpStatus,
        String responseBody,
        String signature
) {
}
