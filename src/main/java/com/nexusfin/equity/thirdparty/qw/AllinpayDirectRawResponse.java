package com.nexusfin.equity.thirdparty.qw;

import java.util.List;
import java.util.Map;

public record AllinpayDirectRawResponse(
        int httpStatus,
        String responseBody,
        String signature,
        Map<String, List<String>> headers
) {

    public AllinpayDirectRawResponse(int httpStatus, String responseBody, String signature) {
        this(httpStatus, responseBody, signature, Map.of());
    }
}
