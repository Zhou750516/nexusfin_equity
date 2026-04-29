package com.nexusfin.equity.thirdparty.qw;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusfin.equity.config.QwProperties;
import com.nexusfin.equity.exception.BizException;
import com.nexusfin.equity.exception.UpstreamTimeoutException;
import com.nexusfin.equity.util.TraceIdUtil;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QwBenefitClientImplTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldSendHexEncryptedRequestBodyForQweimobileProtocol() throws Exception {
        QwBenefitClientImpl client = new QwBenefitClientImpl(qwProperties(), objectMapper);

        String ciphertext = (String) invoke(client, "encodeBusinessData", new Class<?>[]{Object.class},
                new QwExerciseUrlRequest("user-1", "ord-1"));

        assertThat(ciphertext).matches("[0-9a-f]+$");
        assertThat(decryptHex("FbRW7iaiwcEKk2kY", ciphertext))
                .isEqualTo("{\"uniqueId\":\"user-1\",\"partnerOrderNo\":\"ord-1\"}");
    }

    @Test
    void shouldDecryptHexEncryptedResponseBodyForQweimobileProtocol() throws Exception {
        String responsePayload = encryptHex(
                "FbRW7iaiwcEKk2kY",
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

        assertThat(decryptHex("FbRW7iaiwcEKk2kY", ciphertext))
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

        assertThat(decryptHex("FbRW7iaiwcEKk2kY", ciphertext))
                .isEqualTo("{\"userSignId\":5678,\"verCode\":\"123456\"}");
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
        properties.getSecurity().setSignKey("h2Zh53keYy8eksijfj7HfPCncmEMCBXt");
        properties.getSecurity().setAesKey("FbRW7iaiwcEKk2kY");
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
}
