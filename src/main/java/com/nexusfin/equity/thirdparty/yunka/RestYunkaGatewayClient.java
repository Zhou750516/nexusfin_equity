package com.nexusfin.equity.thirdparty.yunka;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.nexusfin.equity.config.YunkaProperties;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class RestYunkaGatewayClient implements YunkaGatewayClient {

    private final YunkaProperties yunkaProperties;
    private final RestClient restClient;

    public RestYunkaGatewayClient(
            YunkaProperties yunkaProperties,
            RestClient.Builder restClientBuilder
    ) {
        this.yunkaProperties = yunkaProperties;
        this.restClient = restClientBuilder
                .baseUrl(yunkaProperties.baseUrl())
                .requestFactory(requestFactory(yunkaProperties))
                .build();
    }

    @Override
    public YunkaGatewayResponse proxy(YunkaGatewayRequest request) {
        if (!yunkaProperties.enabled() || "MOCK".equalsIgnoreCase(yunkaProperties.mode())) {
            return new YunkaGatewayResponse(0, "MOCK", JsonNodeFactory.instance.objectNode());
        }
        return restClient.post()
                .uri(yunkaProperties.gatewayPath())
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(YunkaGatewayResponse.class);
    }

    private static SimpleClientHttpRequestFactory requestFactory(YunkaProperties yunkaProperties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(yunkaProperties.connectTimeoutMs());
        requestFactory.setReadTimeout(yunkaProperties.readTimeoutMs());
        return requestFactory;
    }
}
