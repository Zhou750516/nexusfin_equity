package com.nexusfin.equity.thirdparty.qw;

import java.net.URI;

public record AllinpayDirectInvocation(
        AllinpayDirectOperation operation,
        String serviceCode,
        URI targetUri,
        String merchantId,
        String userName,
        String userPassword,
        Object businessRequest
) {
}
