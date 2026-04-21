package com.nexusfin.equity.thirdparty.yunka;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.sun.net.httpserver.HttpServer;
import com.nexusfin.equity.config.YunkaProperties;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class RestYunkaGatewayClientTest {

    @Test
    void shouldLogBeginAndSuccessWithTraceableGatewayFields(CapturedOutput output) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/gateway/proxy", exchange -> {
            byte[] response = """
                    {
                      "code": 0,
                      "message": "OK",
                      "data": {
                        "loanId": "LN-001"
                      }
                    }
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        RestYunkaGatewayClient client = new RestYunkaGatewayClient(
                yunkaProperties("http://127.0.0.1:" + server.getAddress().getPort()),
                RestClient.builder()
        );

        try {
            YunkaGatewayClient.YunkaGatewayResponse response = client.proxy(new YunkaGatewayClient.YunkaGatewayRequest(
                    "REQ-001",
                    "/loan/apply",
                    "APP-001",
                    JsonNodeFactory.instance.objectNode()
            ));

            assertThat(response.code()).isEqualTo(0);
            assertThat(output).contains("yunka gateway request begin");
            assertThat(output).contains("yunka gateway request success");
            assertThat(output).contains("requestId=REQ-001");
            assertThat(output).contains("path=/loan/apply");
            assertThat(output).contains("bizOrderNo=APP-001");
            assertThat(output).contains("elapsedMs=");
        } finally {
            server.stop(0);
        }
    }

    private YunkaProperties yunkaProperties(String baseUrl) {
        return new YunkaProperties(
                true,
                "REST",
                baseUrl,
                "/api/gateway/proxy",
                2000,
                3000,
                new YunkaProperties.Paths(
                        "/loan/trail",
                        "/loan/query",
                        "/loan/apply",
                        "/repay/trial",
                        "/repay/apply",
                        "/repay/query"
                )
        );
    }
}
