package com.nexusfin.equity.thirdparty.yunka;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

public interface YunkaGatewayClient {

    YunkaGatewayResponse proxy(YunkaGatewayRequest request);

    record YunkaGatewayRequest(
            String requestId,
            String path,
            Object data
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record YunkaGatewayResponse(
            int code,
            String message,
            String traceId,
            String requestId,
            JsonNode data
    ) {
        public YunkaGatewayResponse(int code, String message, JsonNode data) {
            this(code, message, null, null, data);
        }
    }
}
