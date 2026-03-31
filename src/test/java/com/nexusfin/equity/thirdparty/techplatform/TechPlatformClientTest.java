package com.nexusfin.equity.thirdparty.techplatform;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusfin.equity.config.TechPlatformProperties;
import com.nexusfin.equity.exception.BizException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TechPlatformClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldSendLoanInfoNoticeWithSignedEncryptedPayload() throws Exception {
        TechPlatformProperties properties = buildProperties();
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();

        TechPlatformPayloadCodec codec = new TechPlatformPayloadCodec(properties, objectMapper);
        server.expect(requestTo("https://tech-platform.test/guide/api/loanInfoNotice"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("channelId", "abs-001"))
                .andExpect(header("version", "1.0.0"))
                .andExpect(request -> {
                    MockClientHttpRequest mockRequest = (MockClientHttpRequest) request;
                    String body = mockRequest.getBodyAsString(StandardCharsets.UTF_8);
                    JsonNode jsonNode = objectMapper.readTree(body);
                    String param = jsonNode.path("param").asText();
                    assertThat(param).isNotBlank();
                    assertThat(param).doesNotContain("order-1");
                    String timestamp = request.getHeaders().getFirst("timestamp");
                    assertThat(timestamp).isNotBlank();
                    assertThat(request.getHeaders().getFirst("sign"))
                            .isEqualTo(codec.sign(timestamp, param));
                })
                .andRespond(withSuccess("{\"code\":\"0\",\"msg\":\"ok\"}", MediaType.APPLICATION_JSON));

        TechPlatformClient client = new TechPlatformClientImpl(properties, objectMapper, restClientBuilder);
        TechPlatformNotifyResponse response = client.notifyLoanInfo(new LoanInfoNoticeRequest(
                "order-1",
                "loan-1",
                new BigDecimal("3000.00"),
                12,
                "2026-03-30",
                "2027-03-30",
                "SUCCESS",
                1711785600000L,
                "contract-1",
                "622202******6688",
                "招商银行",
                null,
                5,
                2,
                true,
                "0"
        ));

        assertThat(response.code()).isEqualTo("0");
        assertThat(response.msg()).isEqualTo("ok");
        server.verify();
    }

    @Test
    void shouldParseEncryptedNotifyResponse() throws Exception {
        TechPlatformProperties properties = buildProperties();
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();

        TechPlatformPayloadCodec codec = new TechPlatformPayloadCodec(properties, objectMapper);
        String encryptedResponse = codec.encrypt("{\"code\":\"0\",\"msg\":\"accepted\"}");

        server.expect(requestTo("https://tech-platform.test/guide/api/creditStatusNotice"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"param\":\"" + encryptedResponse + "\"}", MediaType.APPLICATION_JSON));

        TechPlatformClient client = new TechPlatformClientImpl(properties, objectMapper, restClientBuilder);
        TechPlatformNotifyResponse response = client.notifyCreditStatus(new CreditStatusNoticeRequest(
                "credit-1",
                "user-1",
                0,
                "审批通过",
                "2026-03-30 10:00:00",
                "12;24",
                "MONTH",
                "MONTHLY",
                new CreditStatusNoticeRequest.LoanOption(
                        new BigDecimal("1000.00"),
                        new BigDecimal("3000.00"),
                        new BigDecimal("100.00"),
                        "00-24"
                ),
                new CreditStatusNoticeRequest.CreditLimit(
                        new BigDecimal("3000.00"),
                        new BigDecimal("3000.00"),
                        1711785600000L,
                        1743321600000L,
                        "ACTIVE",
                        "2026-03-30",
                        "2027-03-30",
                        "CIRCLE"
                )
        ));

        assertThat(response.code()).isEqualTo("0");
        assertThat(response.msg()).isEqualTo("accepted");
        server.verify();
    }

    @Test
    void shouldRejectNonSuccessNotifyResponse() {
        TechPlatformProperties properties = buildProperties();
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();

        server.expect(requestTo("https://tech-platform.test/guide/api/repayInfoNotice"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"code\":\"1\",\"msg\":\"rejected\"}", MediaType.APPLICATION_JSON));

        TechPlatformClient client = new TechPlatformClientImpl(properties, objectMapper, restClientBuilder);

        assertThatThrownBy(() -> client.notifyRepaymentInfo(new RepayInfoNoticeRequest(List.of(
                new RepayInfoNoticeRequest.RepayItem(
                        "order-1",
                        "repay-1",
                        1,
                        "1",
                        1711785600000L,
                        "FAIL",
                        "rejected",
                        0,
                        new BigDecimal("500.00"),
                        new BigDecimal("0.00"),
                        new BigDecimal("0.00"),
                        new BigDecimal("0.00"),
                        new BigDecimal("0.00"),
                        new BigDecimal("500.00"),
                        new BigDecimal("0.00"),
                        new BigDecimal("500.00"),
                        new BigDecimal("0.00"),
                        new BigDecimal("0.00"),
                        List.of(new RepayInfoNoticeRequest.FeePlanItem(
                                "PRINCIPAL",
                                "本金",
                                new BigDecimal("500.00"),
                                new BigDecimal("0.00")
                        )),
                        null,
                        null
                )
        ))))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("TECH_PLATFORM_REJECTED");
    }

    private TechPlatformProperties buildProperties() {
        TechPlatformProperties properties = new TechPlatformProperties();
        properties.setEnabled(true);
        properties.setMode(TechPlatformProperties.Mode.HTTP);
        properties.setBaseUrl("https://tech-platform.test");
        properties.setChannelId("abs-001");
        properties.setVersion("1.0.0");
        properties.setSignSecret("tech-platform-secret");
        properties.setSignAlgorithm(TechPlatformProperties.SignAlgorithm.HMAC_SHA256);
        properties.setAesKeyBase64("MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");
        properties.setAesAlgorithm("AES/ECB/PKCS5Padding");
        properties.setConnectTimeoutMs(2000);
        properties.setReadTimeoutMs(3000);
        properties.getPaths().setCreditStatusNotice("/guide/api/creditStatusNotice");
        properties.getPaths().setLoanInfoNotice("/guide/api/loanInfoNotice");
        properties.getPaths().setRepayInfoNotice("/guide/api/repayInfoNotice");
        return properties;
    }
}
