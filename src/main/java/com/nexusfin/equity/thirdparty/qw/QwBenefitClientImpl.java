package com.nexusfin.equity.thirdparty.qw;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusfin.equity.config.QwProperties;
import com.nexusfin.equity.exception.BizException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HexFormat;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class QwBenefitClientImpl implements QwBenefitClient {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final QwProperties qwProperties;
    private final ObjectMapper objectMapper;
    private final SecureRandom secureRandom = new SecureRandom();
    private final SecretKey secretKey;

    public QwBenefitClientImpl(QwProperties qwProperties, ObjectMapper objectMapper) {
        this.qwProperties = qwProperties;
        this.objectMapper = objectMapper;
        this.secretKey = buildSecretKey(qwProperties.getAesKeyBase64());
    }

    @Override
    public QwMemberSyncResponse syncMemberOrder(QwMemberSyncRequest request) {
        ensureEnabled();
        if (qwProperties.getMode() == QwProperties.Mode.MOCK) {
            return mockMemberSync(request);
        }
        return invoke("abs.push.order", request, QwMemberSyncResponse.class);
    }

    @Override
    public QwExerciseUrlResponse getExerciseUrl(QwExerciseUrlRequest request) {
        ensureEnabled();
        if (qwProperties.getMode() == QwProperties.Mode.MOCK) {
            return mockExerciseUrl(request);
        }
        return invoke("abs.token.get", request, QwExerciseUrlResponse.class);
    }

    @Override
    public QwLendingNotifyResponse notifyLending(QwLendingNotifyRequest request) {
        ensureEnabled();
        if (qwProperties.getMode() == QwProperties.Mode.MOCK) {
            return mockLendingNotify(request);
        }
        return invoke("abs.lending.notify", request, QwLendingNotifyResponse.class);
    }

    private <T> T invoke(String method, Object businessRequest, Class<T> responseType) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        QwEnvelopeRequest request = new QwEnvelopeRequest(
                encodeBusinessData(businessRequest),
                new QwRequestHead(
                        qwProperties.getPartnerNo(),
                        timestamp,
                        method,
                        qwProperties.getVersion(),
                        sign(method, timestamp)
                )
        );
        try {
            RestClient restClient = RestClient.builder()
                    .requestFactory(requestFactory())
                    .build();
            String rawResponse = restClient.post()
                    .uri(URI.create(qwProperties.getBaseUrl() + qwProperties.getMethodPath()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(String.class);
            return parseResponse(rawResponse, responseType);
        } catch (RestClientException exception) {
            throw new BizException("QW_UPSTREAM_FAILED", "Failed to call QW upstream service");
        }
    }

    private <T> T parseResponse(String rawResponse, Class<T> responseType) {
        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            int code = root.path("code").asInt(500);
            if (code != 200) {
                throw new BizException("QW_UPSTREAM_REJECTED", root.path("msg").asText("QW upstream rejected request"));
            }
            JsonNode data = root.get("data");
            if (data == null || data.isNull() || data.asText().isBlank()) {
                throw new BizException("QW_RESPONSE_EMPTY", "QW response payload is empty");
            }
            String ciphertext = data.asText();
            String plaintext = decodeBusinessData(ciphertext);
            return objectMapper.readValue(plaintext, responseType);
        } catch (BizException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BizException("QW_RESPONSE_INVALID", "Failed to parse QW response");
        }
    }

    private String encodeBusinessData(Object businessRequest) {
        try {
            byte[] jsonBytes = objectMapper.writeValueAsBytes(businessRequest);
            byte[] iv = new byte[qwProperties.getIvLengthBytes()];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(qwProperties.getAesAlgorithm());
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(qwProperties.getGcmTagBits(), iv));
            byte[] encrypted = cipher.doFinal(jsonBytes);
            byte[] payload = ByteBuffer.allocate(iv.length + encrypted.length)
                    .put(iv)
                    .put(encrypted)
                    .array();
            return Base64.getEncoder().encodeToString(payload);
        } catch (JsonProcessingException exception) {
            throw new BizException("QW_REQUEST_INVALID", "Failed to serialize QW request payload");
        } catch (GeneralSecurityException exception) {
            throw new BizException("QW_ENCRYPT_FAILED", "Failed to encrypt QW request payload");
        }
    }

    private String decodeBusinessData(String ciphertext) {
        try {
            byte[] payload = Base64.getDecoder().decode(ciphertext);
            if (payload.length <= qwProperties.getIvLengthBytes()) {
                throw new BizException("QW_RESPONSE_INVALID", "QW response payload is invalid");
            }
            byte[] iv = new byte[qwProperties.getIvLengthBytes()];
            byte[] encrypted = new byte[payload.length - qwProperties.getIvLengthBytes()];
            System.arraycopy(payload, 0, iv, 0, iv.length);
            System.arraycopy(payload, iv.length, encrypted, 0, encrypted.length);
            Cipher cipher = Cipher.getInstance(qwProperties.getAesAlgorithm());
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(qwProperties.getGcmTagBits(), iv));
            byte[] plaintext = cipher.doFinal(encrypted);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (BizException exception) {
            throw exception;
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new BizException("QW_DECRYPT_FAILED", "Failed to decrypt QW response payload");
        }
    }

    private String sign(String method, String timestamp) {
        try {
            String payload = method + qwProperties.getPartnerNo() + timestamp + qwProperties.getVersion() + qwProperties.getSignKey();
            byte[] digest = MessageDigest.getInstance("MD5").digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Failed to build QW request signature", exception);
        }
    }

    private SimpleClientHttpRequestFactory requestFactory() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(qwProperties.getConnectTimeoutMs());
        requestFactory.setReadTimeout(qwProperties.getReadTimeoutMs());
        return requestFactory;
    }

    private SecretKey buildSecretKey(String keyBase64) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(keyBase64);
            return new SecretKeySpec(keyBytes, "AES");
        } catch (IllegalArgumentException exception) {
            throw new BizException("QW_AES_KEY_INVALID", "QW AES key must be valid base64");
        }
    }

    private void ensureEnabled() {
        if (!qwProperties.isEnabled()) {
            throw new BizException("QW_DISABLED", "QW integration is disabled");
        }
    }

    private QwMemberSyncResponse mockMemberSync(QwMemberSyncRequest request) {
        String now = DATE_TIME_FORMATTER.format(LocalDateTime.now());
        return new QwMemberSyncResponse(
                "qw-order-" + request.partnerOrderNo(),
                "card-" + request.partnerOrderNo(),
                String.valueOf(System.currentTimeMillis()),
                0,
                request.productCode(),
                request.productName(),
                "independence",
                now,
                DATE_TIME_FORMATTER.format(LocalDateTime.now().plusYears(1))
        );
    }

    private QwExerciseUrlResponse mockExerciseUrl(QwExerciseUrlRequest request) {
        String redirectUrl = qwProperties.getMockExerciseBaseUrl()
                + "?partnerOrderNo=" + request.partnerOrderNo()
                + "&uniqueId=" + request.uniqueId()
                + "&token=mock-token-" + request.partnerOrderNo();
        return new QwExerciseUrlResponse(
                0,
                redirectUrl,
                "mock-token-" + request.partnerOrderNo(),
                DATE_TIME_FORMATTER.format(LocalDateTime.now()),
                DATE_TIME_FORMATTER.format(LocalDateTime.now().plusYears(1))
        );
    }

    private QwLendingNotifyResponse mockLendingNotify(QwLendingNotifyRequest request) {
        return new QwLendingNotifyResponse("qw-order-" + request.partnerOrderNo());
    }

    private record QwEnvelopeRequest(
            String requestBody,
            QwRequestHead requestHead
    ) {
    }

    private record QwRequestHead(
            String partnerNo,
            String timestamp,
            String method,
            String version,
            String sign
    ) {
    }
}
