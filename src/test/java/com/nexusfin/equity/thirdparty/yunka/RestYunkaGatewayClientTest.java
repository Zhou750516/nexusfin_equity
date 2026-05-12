package com.nexusfin.equity.thirdparty.yunka;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.nexusfin.equity.config.YunkaProperties;
import com.nexusfin.equity.util.TraceIdUtil;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(OutputCaptureExtension.class)
class RestYunkaGatewayClientTest {

    private static final String TEST_APP_ID = "ABS-YUNKA-TEST";
    private static final String TEST_CHANNEL_CODE = "ABS";
    private static final String TEST_APP_SECRET = "yunka-test-secret";

    @AfterEach
    void tearDown() {
        TraceIdUtil.clear();
    }

    @Test
    void shouldReturnMockResponseWithoutCallingHttpWhenModeIsMock(CapturedOutput output) {
        RestYunkaGatewayClient client = new RestYunkaGatewayClient(
                yunkaProperties("MOCK", "http://127.0.0.1:1"),
                RestClient.builder()
        );

        YunkaGatewayClient.YunkaGatewayResponse response = client.proxy(new YunkaGatewayClient.YunkaGatewayRequest(
                "REQ-MOCK-001",
                "/loan/query",
                JsonNodeFactory.instance.objectNode()
        ));

        assertThat(response.code()).isEqualTo(0);
        assertThat(response.message()).isEqualTo("MOCK");
        assertThat(output).contains("yunka gateway client initialized");
        assertThat(output).contains("mode=MOCK");
    }

    @Test
    void shouldLogBeginAndSuccessWithTraceableGatewayFields(CapturedOutput output) {
        TraceIdUtil.bindTraceId("TRACE-REQ-001");
        String timestamp = "1746955200123";
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        server.expect(requestTo("http://127.0.0.1:18081/api/gateway/proxy"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(request -> {
                    assertThat(request.getHeaders().getFirst("X-Trace-Id")).isEqualTo("TRACE-REQ-001");
                    assertThat(request.getHeaders().getFirst("AppID")).isEqualTo(TEST_APP_ID);
                    assertThat(request.getHeaders().getFirst("X-Timestamp")).isEqualTo(timestamp);
                    assertThat(request.getHeaders().getFirst("X-Channel-Code")).isEqualTo(TEST_CHANNEL_CODE);
                    assertThat(request.getHeaders().getFirst("X-Signature")).isEqualTo(
                            sign("{}", "REQ-001", timestamp, TEST_APP_SECRET)
                    );
                    assertThat(request.getHeaders().getFirst("X-Request-Id")).isEqualTo("REQ-001");
                    assertThat(request.getHeaders().containsKey("X-Biz-Order-No")).isFalse();
                    assertThat(request.getHeaders().containsKey("Nonce")).isFalse();
                    assertThat(request.getHeaders().containsKey("Signature")).isFalse();
                    assertThat(request.getHeaders().containsKey("Timestamp")).isFalse();
                    assertThat(((MockClientHttpRequest) request).getBodyAsString())
                            .contains("\"requestId\":\"REQ-001\"")
                            .contains("\"path\":\"/loan/apply\"")
                            .doesNotContain("bizOrderNo");
                })
                .andExpect(content().json("""
                        {
                          "requestId": "REQ-001",
                          "path": "/loan/apply",
                          "data": {}
                        }
                        """))
                .andRespond(withSuccess("""
                        {
                          "code": 0,
                          "message": "OK",
                          "traceId": "YUNKA-TRACE-001",
                          "requestId": "REQ-001",
                          "data": {
                            "loanId": "LN-001"
                          }
                        }
                        """, MediaType.APPLICATION_JSON));
        RestClient restClient = restClientBuilder
                .baseUrl("http://127.0.0.1:18081")
                .build();
        RestYunkaGatewayClient client = new RestYunkaGatewayClient(
                yunkaProperties("REST", "http://127.0.0.1:18081"),
                restClient,
                timestamp::toString
        );

        YunkaGatewayClient.YunkaGatewayResponse response = client.proxy(new YunkaGatewayClient.YunkaGatewayRequest(
                "REQ-001",
                "/loan/apply",
                JsonNodeFactory.instance.objectNode()
        ));

        assertThat(response.code()).isEqualTo(0);
        assertThat(response.traceId()).isEqualTo("YUNKA-TRACE-001");
        assertThat(response.requestId()).isEqualTo("REQ-001");
        assertThat(output).contains("yunka gateway client initialized");
        assertThat(output).contains("mode=REST");
        assertThat(output).contains("yunka gateway request begin");
        assertThat(output).contains("yunka gateway request success");
        assertThat(output).contains("requestId=REQ-001");
        assertThat(output).contains("path=/loan/apply");
        assertThat(output).contains("appId=" + TEST_APP_ID);
        assertThat(output).contains("xChannelCode=" + TEST_CHANNEL_CODE);
        assertThat(output).contains("xRequestId=REQ-001");
        assertThat(output).contains("xTimestamp=" + timestamp);
        assertThat(output).contains("signaturePrefix=");
        assertThat(output).contains("yunka_app_id=" + TEST_APP_ID);
        assertThat(output).contains("yunka_channel_code=" + TEST_CHANNEL_CODE);
        assertThat(output).doesNotContain("X-Biz-Order-No");
        assertThat(output).contains("elapsedMs=");
        assertThat(output).contains("requestBodyJson=");
        assertThat(output).contains("\"requestId\":\"REQ-001\"");
        assertThat(output).contains("\"path\":\"/loan/apply\"");
        assertThat(output).doesNotContain("\"bizOrderNo\"");
        assertThat(output).doesNotContain(sign("{}", "REQ-001", timestamp, TEST_APP_SECRET));
        assertThat(output).contains("responseBodyJson=");
        assertThat(output).contains("\"code\":0");
        assertThat(output).contains("\"message\":\"OK\"");
        assertThat(output).contains("\"data\":{\"loanId\":\"LN-001\"}");
        server.verify();
    }

    @Test
    void shouldRejectUnsupportedModeAtConstructionTime() {
        assertThatThrownBy(() -> new RestYunkaGatewayClient(
                yunkaProperties("BROKEN", "http://127.0.0.1:18081"),
                RestClient.builder()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported Yunka mode")
                .hasMessageContaining("BROKEN");
    }

    @Test
    void shouldLogUtf8ProviderMessageWithoutGarbledCharacters(CapturedOutput output) {
        TraceIdUtil.bindTraceId("TRACE-UTF8-001");
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        server.expect(requestTo("http://127.0.0.1:18081/api/gateway/proxy"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        """
                        {
                          "code": 1002,
                          "message": "SIGNATURE_INVALID",
                          "traceId": "TRACE-UTF8-001",
                          "requestId": "REQ-UTF8-001",
                          "data": {
                            "status": "UNKNOWN",
                            "providerMessage": "签名IP未授权",
                            "retryable": false,
                            "errorType": "SIGNATURE_INVALID",
                            "payload": null
                          }
                        }
                        """.getBytes(StandardCharsets.UTF_8),
                        MediaType.APPLICATION_JSON
                ));
        RestClient restClient = restClientBuilder
                .baseUrl("http://127.0.0.1:18081")
                .build();
        RestYunkaGatewayClient client = new RestYunkaGatewayClient(
                yunkaProperties("REST", "http://127.0.0.1:18081"),
                restClient,
                () -> "1746955200999"
        );

        YunkaGatewayClient.YunkaGatewayResponse response = client.proxy(new YunkaGatewayClient.YunkaGatewayRequest(
                "REQ-UTF8-001",
                "/user/token",
                JsonNodeFactory.instance.objectNode()
        ));

        assertThat(response.code()).isEqualTo(1002);
        assertThat(output).contains("requestId=REQ-UTF8-001");
        assertThat(output).contains("path=/user/token");
        assertThat(output).contains("appId=" + TEST_APP_ID);
        assertThat(output).contains("xChannelCode=" + TEST_CHANNEL_CODE);
        assertThat(output).contains("xRequestId=REQ-UTF8-001");
        assertThat(output).contains("xTimestamp=1746955200999");
        assertThat(output).contains("signaturePrefix=");
        assertThat(output).contains("\"providerMessage\":\"签名IP未授权\"");
        assertThat(output).doesNotContain("???IP??????");
        server.verify();
    }

    private YunkaProperties yunkaProperties(String mode, String baseUrl) {
        return new YunkaProperties(
                true,
                mode,
                baseUrl,
                "/api/gateway/proxy",
                2000,
                3000,
                new YunkaProperties.Paths(
                        "/loan/trial",
                        "/loan/query",
                        "/loan/apply",
                        "/repay/trial",
                        "/repay/apply",
                        "/repay/query",
                        "/protocol/queryProtocolAggregationLink",
                        "/user/token",
                        "/user/query",
                        "/loan/repayPlan",
                        "/card/smsSend",
                        "/card/smsConfirm",
                        "/card/userCards",
                        "/credit/image/query",
                        "/benefit/sync"
                ),
                TEST_CHANNEL_CODE,
                TEST_APP_ID,
                TEST_APP_SECRET
        );
    }

    private String sign(String dataJson, String requestId, String timestamp, String secret) {
        try {
            String payload = "data=" + dataJson + "&requestId=" + requestId + "&timestamp=" + timestamp;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getEncoder().encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
