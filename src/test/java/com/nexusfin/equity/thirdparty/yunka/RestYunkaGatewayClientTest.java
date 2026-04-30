package com.nexusfin.equity.thirdparty.yunka;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.nexusfin.equity.config.YunkaProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(OutputCaptureExtension.class)
class RestYunkaGatewayClientTest {

    @Test
    void shouldReturnMockResponseWithoutCallingHttpWhenModeIsMock(CapturedOutput output) {
        RestYunkaGatewayClient client = new RestYunkaGatewayClient(
                yunkaProperties("MOCK", "http://127.0.0.1:1"),
                RestClient.builder()
        );

        YunkaGatewayClient.YunkaGatewayResponse response = client.proxy(new YunkaGatewayClient.YunkaGatewayRequest(
                "REQ-MOCK-001",
                "/loan/query",
                "APP-MOCK-001",
                JsonNodeFactory.instance.objectNode()
        ));

        assertThat(response.code()).isEqualTo(0);
        assertThat(response.message()).isEqualTo("MOCK");
        assertThat(output).contains("yunka gateway client initialized");
        assertThat(output).contains("mode=MOCK");
    }

    @Test
    void shouldLogBeginAndSuccessWithTraceableGatewayFields(CapturedOutput output) {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        server.expect(requestTo("http://127.0.0.1:18081/api/gateway/proxy"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {
                          "code": 0,
                          "message": "OK",
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
                restClient
        );

        YunkaGatewayClient.YunkaGatewayResponse response = client.proxy(new YunkaGatewayClient.YunkaGatewayRequest(
                "REQ-001",
                "/loan/apply",
                "APP-001",
                JsonNodeFactory.instance.objectNode()
        ));

        assertThat(response.code()).isEqualTo(0);
        assertThat(output).contains("yunka gateway client initialized");
        assertThat(output).contains("mode=REST");
        assertThat(output).contains("yunka gateway request begin");
        assertThat(output).contains("yunka gateway request success");
        assertThat(output).contains("requestId=REQ-001");
        assertThat(output).contains("path=/loan/apply");
        assertThat(output).contains("bizOrderNo=APP-001");
        assertThat(output).contains("elapsedMs=");
        assertThat(output).contains("requestBodyJson=");
        assertThat(output).contains("\"requestId\":\"REQ-001\"");
        assertThat(output).contains("\"path\":\"/loan/apply\"");
        assertThat(output).contains("\"bizOrderNo\":\"APP-001\"");
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

    private YunkaProperties yunkaProperties(String mode, String baseUrl) {
        return new YunkaProperties(
                true,
                mode,
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
                )
        );
    }
}
