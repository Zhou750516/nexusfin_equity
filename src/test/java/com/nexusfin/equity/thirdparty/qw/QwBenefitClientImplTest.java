package com.nexusfin.equity.thirdparty.qw;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusfin.equity.config.QwProperties;
import com.nexusfin.equity.exception.BizException;
import com.nexusfin.equity.exception.UpstreamTimeoutException;
import com.nexusfin.equity.util.TraceIdUtil;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class QwBenefitClientImplTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldSendHexEncryptedRequestBodyForQweimobileProtocol() throws Exception {
        QwBenefitClientImpl client = new QwBenefitClientImpl(qwProperties(), objectMapper);

        String ciphertext = (String) invoke(client, "encodeBusinessData", new Class<?>[]{Object.class},
                new QwExerciseUrlRequest("user-1", "ord-1"));

        assertThat(ciphertext).matches("[0-9a-f]+$");
        assertThat(decryptHex("unit-test-aes-16", ciphertext))
                .isEqualTo("{\"uniqueId\":\"user-1\",\"partnerOrderNo\":\"ord-1\"}");
    }

    @Test
    void shouldDecryptHexEncryptedResponseBodyForQweimobileProtocol() throws Exception {
        String responsePayload = encryptHex(
                "unit-test-aes-16",
                "{\"memberFlag\":0,\"redirectUrl\":\"https://redirect.test\",\"token\":\"token-1\",\"cardCreatedDate\":\"2026-04-01 00:00:00\",\"cardExpiryDate\":\"2027-04-01 00:00:00\"}"
        );
        QwBenefitClientImpl client = new QwBenefitClientImpl(qwProperties(), objectMapper);

        QwExerciseUrlResponse response = (QwExerciseUrlResponse) invoke(
                client,
                "parseResponse",
                new Class<?>[]{String.class, Class.class},
                "{\"code\":200,\"msg\":\"success\",\"data\":\"" + responsePayload + "\"}",
                QwExerciseUrlResponse.class
        );

        assertThat(response.memberFlag()).isEqualTo(0);
        assertThat(response.redirectUrl()).isEqualTo("https://redirect.test");
        assertThat(response.token()).isEqualTo("token-1");
    }

    @Test
    void shouldReturnSignedMockStatusForSignQuery() {
        QwProperties properties = qwProperties();
        properties.setMode(QwProperties.Mode.MOCK);
        QwBenefitClientImpl client = new QwBenefitClientImpl(properties, objectMapper);

        QwSignStatusResponse response = client.querySignStatus(new QwSignStatusRequest(
                "200000000007804",
                "13800138000",
                "测试用户",
                "6222020202020208"
        ));

        assertThat(response.status()).isEqualTo(1);
    }

    @Test
    void shouldEncodeMerchantIdInSignStatusRequestBody() throws Exception {
        QwBenefitClientImpl client = new QwBenefitClientImpl(qwProperties(), objectMapper);

        String ciphertext = (String) invoke(client, "encodeBusinessData", new Class<?>[]{Object.class},
                new QwSignStatusRequest("200000000007804", "13800138000", "测试用户", "6222020202020208"));

        assertThat(decryptHex("unit-test-aes-16", ciphertext))
                .isEqualTo("{\"merchantId\":\"200000000007804\",\"phone\":\"13800138000\",\"name\":\"测试用户\",\"accountNo\":\"6222020202020208\"}");
    }

    @Test
    void shouldReturnMockExerciseUrlForMockMode() {
        QwProperties properties = qwProperties();
        properties.setMode(QwProperties.Mode.MOCK);
        properties.getHttp().setMockExerciseBaseUrl("https://mock-qw.test/exercise");
        QwBenefitClientImpl client = new QwBenefitClientImpl(properties, objectMapper);

        QwExerciseUrlResponse response = client.getExerciseUrl(new QwExerciseUrlRequest("user-1", "ord-1"));

        assertThat(response.memberFlag()).isEqualTo(0);
        assertThat(response.redirectUrl()).contains("https://mock-qw.test/exercise");
        assertThat(response.redirectUrl()).contains("partnerOrderNo=ord-1");
        assertThat(response.token()).isEqualTo("mock-token-ord-1");
    }

    @Test
    void shouldReturnMockApplyRequestNoForSignApply() {
        QwProperties properties = qwProperties();
        properties.setMode(QwProperties.Mode.MOCK);
        QwBenefitClientImpl client = new QwBenefitClientImpl(properties, objectMapper);

        QwSignApplyResponse response = client.applySign(new QwSignApplyRequest(
                "200000000007804",
                "13800138000",
                "测试用户",
                "6222020202021234",
                "110101199003071234"
        ));

        assertThat(response.userSignId()).isEqualTo(1234L);
        assertThat(response.applyTime()).isEqualTo("2026-04-29 10:00:00");
    }

    @Test
    void shouldReturnMockAgreementForSignConfirm() {
        QwProperties properties = qwProperties();
        properties.setMode(QwProperties.Mode.MOCK);
        QwBenefitClientImpl client = new QwBenefitClientImpl(properties, objectMapper);
        try {
            TraceIdUtil.bindTraceId("trace-sign-confirm");
            QwSignConfirmResponse response = client.confirmSign(new QwSignConfirmRequest(
                    5678L,
                    "123456"
            ));

            assertThat(response.userSignId()).isEqualTo(5678L);
            assertThat(response.agreementNo()).startsWith("mock-agreement-5678");
        } finally {
            TraceIdUtil.clear();
        }
    }

    @Test
    void shouldReturnUniqueMockAgreementPerTraceForSameUserSignId() {
        QwProperties properties = qwProperties();
        properties.setMode(QwProperties.Mode.MOCK);
        QwBenefitClientImpl client = new QwBenefitClientImpl(properties, objectMapper);

        try {
            TraceIdUtil.bindTraceId("trace-clean-user-sign-confirm");
            QwSignConfirmResponse cleanUserResponse = client.confirmSign(new QwSignConfirmRequest(1234L, "123456"));
            TraceIdUtil.bindTraceId("trace-happy-user-sign-confirm");
            QwSignConfirmResponse happyUserResponse = client.confirmSign(new QwSignConfirmRequest(1234L, "123456"));

            assertThat(cleanUserResponse.agreementNo()).startsWith("mock-agreement-1234-");
            assertThat(happyUserResponse.agreementNo()).startsWith("mock-agreement-1234-");
            assertThat(cleanUserResponse.agreementNo()).isNotEqualTo(happyUserResponse.agreementNo());
        } finally {
            TraceIdUtil.clear();
        }
    }

    @Test
    void shouldInjectTimeoutForMockSignConfirmViaTraceId() {
        QwProperties properties = qwProperties();
        properties.setMode(QwProperties.Mode.MOCK);
        QwBenefitClientImpl client = new QwBenefitClientImpl(properties, objectMapper);
        TraceIdUtil.bindTraceId("REQ_EX_QW_CONFIRM_FAULT_TIMEOUT");

        try {
            assertThatThrownBy(() -> client.confirmSign(new QwSignConfirmRequest(5678L, "123456")))
                    .isInstanceOf(UpstreamTimeoutException.class)
                    .hasMessageContaining("Mock QW timeout");
        } finally {
            TraceIdUtil.clear();
        }
    }

    @Test
    void shouldInjectRejectForMockSignConfirmViaVerificationCode() {
        QwProperties properties = qwProperties();
        properties.setMode(QwProperties.Mode.MOCK);
        QwBenefitClientImpl client = new QwBenefitClientImpl(properties, objectMapper);

        assertThatThrownBy(() -> client.confirmSign(new QwSignConfirmRequest(5678L, "123456_FAULT_REJECT_503")))
                .isInstanceOf(BizException.class)
                .extracting(error -> ((BizException) error).getCode(), error -> ((BizException) error).getErrorNo())
                .containsExactly(503, "QW_UPSTREAM_REJECTED");
    }

    @Test
    void shouldInjectRejectForMockMemberSyncViaPartnerOrderNo() {
        QwProperties properties = qwProperties();
        properties.setMode(QwProperties.Mode.MOCK);
        QwBenefitClientImpl client = new QwBenefitClientImpl(properties, objectMapper);

        assertThatThrownBy(() -> client.syncMemberOrder(new QwMemberSyncRequest(
                "user-1",
                "ord-1_FAULT_REJECT_502",
                680000L,
                "P-1",
                "权益产品",
                99887766L,
                null,
                0,
                null,
                null,
                null,
                null
        ))).isInstanceOf(BizException.class)
                .extracting(error -> ((BizException) error).getCode(), error -> ((BizException) error).getErrorNo())
                .containsExactly(502, "QW_UPSTREAM_REJECTED");
    }

    @Test
    void shouldInjectDelayForMockMemberSync() {
        QwProperties properties = qwProperties();
        properties.setMode(QwProperties.Mode.MOCK);
        QwBenefitClientImpl client = new QwBenefitClientImpl(properties, objectMapper);
        long startNanos = System.nanoTime();

        QwMemberSyncResponse response = client.syncMemberOrder(new QwMemberSyncRequest(
                "user-1",
                "ord-1_FAULT_DELAY_25",
                680000L,
                "P-1",
                "权益产品",
                99887766L,
                null,
                0,
                null,
                null,
                null,
                null
        ));

        assertThat(response.orderNo()).isEqualTo("qw-order-ord-1_FAULT_DELAY_25");
        assertThat((System.nanoTime() - startNanos) / 1_000_000L).isGreaterThanOrEqualTo(20L);
    }

    @Test
    void shouldEncodeSimplifiedSignConfirmRequestFor0429Contract() throws Exception {
        QwBenefitClientImpl client = new QwBenefitClientImpl(qwProperties(), objectMapper);

        String ciphertext = (String) invoke(client, "encodeBusinessData", new Class<?>[]{Object.class},
                new QwSignConfirmRequest(5678L, "123456"));

        assertThat(decryptHex("unit-test-aes-16", ciphertext))
                .isEqualTo("{\"userSignId\":5678,\"verCode\":\"123456\"}");
    }

    @Test
    void shouldEncodeDeductionNotifyRequestFor0515Contract() throws Exception {
        QwBenefitClientImpl client = new QwBenefitClientImpl(qwProperties(), objectMapper);

        String ciphertext = (String) invoke(client, "encodeBusinessData", new Class<?>[]{Object.class},
                new QwDeductionNotifyRequest("user-1", "ord-1", "serial-1", 1, 99887766L));

        assertThat(decryptHex("unit-test-aes-16", ciphertext))
                .isEqualTo("{\"uniqueId\":\"user-1\",\"partnerOrderNo\":\"ord-1\",\"serialNo\":\"serial-1\",\"status\":1,\"userSignId\":99887766}");
    }

    @Test
    void shouldReturnMockDeductionQueryStatusForMockMode() {
        QwProperties properties = qwProperties();
        properties.setMode(QwProperties.Mode.MOCK);
        QwBenefitClientImpl client = new QwBenefitClientImpl(properties, objectMapper);

        QwDeductionQueryResponse response = client.queryDeduction(new QwDeductionQueryRequest("user-1", "ord-1"));

        assertThat(response.status()).isEqualTo(2);
    }

    @Test
    void shouldReturnMockOrderCancelSuccessForMockMode() {
        QwProperties properties = qwProperties();
        properties.setMode(QwProperties.Mode.MOCK);
        QwBenefitClientImpl client = new QwBenefitClientImpl(properties, objectMapper);

        QwOrderCancelResponse response = client.cancelOrder(new QwOrderCancelRequest("ord-1"));

        assertThat(response.success()).isTrue();
    }

    @Test
    void shouldPostHttpMode0515MethodsToConfiguredMethodEndpointWithEncryptedEnvelope() throws Exception {
        List<CapturedQwRequest> capturedRequests = new ArrayList<>();
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        server.expect(requestTo("https://t-api.test.qweimobile.com/api/abs/method"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(request -> captureRequest(request, capturedRequests))
                .andRespond(withSuccess(responseBody("{\"orderNo\":\"qw-order-real\"}"), MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://t-api.test.qweimobile.com/api/abs/method"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(request -> captureRequest(request, capturedRequests))
                .andRespond(withSuccess(responseBody("{\"status\":2}"), MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://t-api.test.qweimobile.com/api/abs/method"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(request -> captureRequest(request, capturedRequests))
                .andRespond(withSuccess(responseBody("{\"success\":true}"), MediaType.APPLICATION_JSON));
        QwProperties properties = qwProperties();
        properties.setMode(QwProperties.Mode.HTTP);
        QwBenefitClientImpl client = new QwBenefitClientImpl(properties, objectMapper, restClientBuilder.build());

        QwDeductionNotifyResponse notifyResponse = client.notifyDeduction(
                new QwDeductionNotifyRequest("user-1", "ord-1", "serial-1", 1, 99887766L));
        QwDeductionQueryResponse queryResponse = client.queryDeduction(new QwDeductionQueryRequest("user-1", "ord-1"));
        QwOrderCancelResponse cancelResponse = client.cancelOrder(new QwOrderCancelRequest("ord-1"));

        assertThat(notifyResponse.orderNo()).isEqualTo("qw-order-real");
        assertThat(queryResponse.status()).isEqualTo(2);
        assertThat(cancelResponse.success()).isTrue();
        assertThat(capturedRequests)
                .extracting(CapturedQwRequest::path)
                .containsExactly("/api/abs/method", "/api/abs/method", "/api/abs/method");
        assertThat(capturedRequests)
                .extracting(CapturedQwRequest::method)
                .containsExactly("abs.deduction.notify", "abs.deduction.query", "abs.order.cancel");
        assertThat(capturedRequests)
                .extracting(CapturedQwRequest::partnerNo)
                .containsExactly("abs", "abs", "abs");
        assertThat(capturedRequests.get(0).requestBody()).matches("[0-9a-f]+");
        assertThat(decryptHex("unit-test-aes-16", capturedRequests.get(0).requestBody()))
                .isEqualTo("{\"uniqueId\":\"user-1\",\"partnerOrderNo\":\"ord-1\",\"serialNo\":\"serial-1\",\"status\":1,\"userSignId\":99887766}");
        assertThat(capturedRequests)
                .allSatisfy(request -> assertThat(request.rawBody())
                        .doesNotContain("unit-test-sign-key-not-secret")
                        .doesNotContain("unit-test-aes-16"));
        server.verify();
    }

    @Test
    void shouldRejectHttpModeWhenDefaultPlaceholderSecretsAreStillConfigured() {
        QwProperties properties = new QwProperties();
        properties.setMode(QwProperties.Mode.HTTP);

        assertThatThrownBy(() -> new QwBenefitClientImpl(properties, objectMapper))
                .isInstanceOf(BizException.class)
                .extracting(error -> ((BizException) error).getErrorNo())
                .isEqualTo("QW_HTTP_CONFIG_INVALID");
    }

    @Test
    void shouldPrepareReusableRestInfrastructureAtConstructionTime() throws Exception {
        QwBenefitClientImpl client = new QwBenefitClientImpl(qwProperties(), objectMapper);

        Object requestFactory = readField(client, "requestFactory");
        Object restClient = readField(client, "restClient");

        assertThat(requestFactory).isInstanceOf(SimpleClientHttpRequestFactory.class);
        assertThat(restClient).isInstanceOf(RestClient.class);
    }

    private QwProperties qwProperties() {
        QwProperties properties = new QwProperties();
        properties.setEnabled(true);
        properties.setMode(QwProperties.Mode.QWEIMOBILE_HTTP);
        properties.getHttp().setBaseUrl("https://t-api.test.qweimobile.com");
        properties.getHttp().setMethodPath("/api/abs/method");
        properties.setPartnerNo("abs");
        properties.setVersion("v1.0");
        properties.getSecurity().setSignKey("unit-test-sign-key-not-secret");
        properties.getSecurity().setAesKey("unit-test-aes-16");
        properties.getSecurity().setAesKeyEncoding(QwProperties.AesKeyEncoding.RAW);
        properties.getSecurity().setAesAlgorithm("AES/ECB/PKCS5Padding");
        properties.getSecurity().setCiphertextEncoding(QwProperties.CiphertextEncoding.HEX);
        return properties;
    }

    private String encryptHex(String key, String plaintext) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES"));
        return HexFormat.of().formatHex(cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8)));
    }

    private String decryptHex(String key, String ciphertext) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES"));
        return new String(cipher.doFinal(HexFormat.of().parseHex(ciphertext)), StandardCharsets.UTF_8);
    }

    private Object invoke(Object target, String methodName, Class<?>[] parameterTypes, Object... args) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return method.invoke(target, args);
    }

    private Object readField(Object target, String fieldName) throws Exception {
        var field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }

    private void captureRequest(org.springframework.http.client.ClientHttpRequest request,
                                List<CapturedQwRequest> capturedRequests) throws IOException {
        MockClientHttpRequest mockRequest = (MockClientHttpRequest) request;
        String rawBody = mockRequest.getBodyAsString(StandardCharsets.UTF_8);
        var root = objectMapper.readTree(rawBody);
        capturedRequests.add(new CapturedQwRequest(
                request.getURI().getPath(),
                root.path("requestHead").path("method").asText(),
                root.path("requestHead").path("partnerNo").asText(),
                root.path("requestBody").asText(),
                rawBody
        ));
    }

    private String responseBody(String responsePayload) throws IOException {
        try {
            return "{\"code\":200,\"msg\":\"success\",\"data\":\""
                    + encryptHex("unit-test-aes-16", responsePayload)
                    + "\"}";
        } catch (Exception exception) {
            throw new IOException(exception);
        }
    }

    private record CapturedQwRequest(
            String path,
            String method,
            String partnerNo,
            String requestBody,
            String rawBody
    ) {
    }
}
