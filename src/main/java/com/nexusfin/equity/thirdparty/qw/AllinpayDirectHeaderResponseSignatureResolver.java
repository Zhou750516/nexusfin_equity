package com.nexusfin.equity.thirdparty.qw;

import com.nexusfin.equity.exception.BizException;
import java.util.List;

public class AllinpayDirectHeaderResponseSignatureResolver implements AllinpayDirectResponseSignatureResolver {

    private final String headerName;

    public AllinpayDirectHeaderResponseSignatureResolver(String headerName) {
        this.headerName = headerName;
    }

    @Override
    public String resolve(AllinpayDirectRawResponse rawResponse) {
        List<String> values = rawResponse.headers().get(headerName);
        if (values == null || values.isEmpty() || values.get(0) == null || values.get(0).isBlank()) {
            throw new BizException(
                    "ALLINPAY_DIRECT_RESPONSE_SIGNATURE_MISSING",
                    "Missing allinpay direct response signature header: " + headerName
            );
        }
        return values.get(0);
    }
}
