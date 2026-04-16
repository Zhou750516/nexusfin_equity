package com.nexusfin.equity.thirdparty.qw;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusfin.equity.config.QwProperties;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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

    private QwProperties qwProperties() {
        QwProperties properties = new QwProperties();
        properties.setEnabled(true);
        properties.setMode(QwProperties.Mode.QWEIMOBILE_HTTP);
        properties.setBaseUrl("https://t-api.test.qweimobile.com");
        properties.setMethodPath("/api/abs/method");
        properties.setPartnerNo("abs");
        properties.setVersion("v1.0");
        properties.setSignKey("h2Zh53keYy8eksijfj7HfPCncmEMCBXt");
        properties.setAesKey("FbRW7iaiwcEKk2kY");
        properties.setAesKeyEncoding(QwProperties.AesKeyEncoding.RAW);
        properties.setAesAlgorithm("AES/ECB/PKCS5Padding");
        properties.setCiphertextEncoding(QwProperties.CiphertextEncoding.HEX);
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
}
