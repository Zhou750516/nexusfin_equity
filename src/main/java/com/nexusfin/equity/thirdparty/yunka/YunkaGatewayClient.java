package com.nexusfin.equity.thirdparty.yunka;

import com.fasterxml.jackson.databind.JsonNode;

public interface YunkaGatewayClient {

    YunkaGatewayResponse proxy(YunkaGatewayRequest request);

    record YunkaGatewayRequest(
            String requestId,
            String path,
            String bizOrderNo,
            Object data
    ) {
    }

    record YunkaGatewayResponse(
            int code,
            String message,
            JsonNode data
    ) {
    }
}
