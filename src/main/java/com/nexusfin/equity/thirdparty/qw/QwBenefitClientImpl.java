package com.nexusfin.equity.thirdparty.qw;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusfin.equity.config.QwProperties;
import com.nexusfin.equity.exception.BizException;
import com.nexusfin.equity.exception.UpstreamTimeoutException;
import com.nexusfin.equity.util.TraceIdUtil;
import com.nexusfin.equity.util.UpstreamTimeoutDetector;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class QwBenefitClientImpl implements QwBenefitClient {

    private static final Logger log = LoggerFactory.getLogger(QwBenefitClientImpl.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String QW_UPSTREAM_REJECTED = "QW_UPSTREAM_REJECTED";
    private static final String DEFAULT_SIGN_KEY_PLACEHOLDER = "abs-secret-key";
    private static final String DEFAULT_AES_KEY_PLACEHOLDER = "0123456789abcdef";
    private static final String DEFAULT_AES_KEY_BASE64_PLACEHOLDER = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
    private static final Pattern REJECT_PATTERN = Pattern.compile(".*_FAULT_REJECT_(\\d+)$");
    private static final Pattern DELAY_PATTERN = Pattern.compile(".*_FAULT_DELAY_(\\d+)$");

    private final QwProperties qwProperties;
    private final ObjectMapper objectMapper;
    private final SecureRandom secureRandom = new SecureRandom();
    private final SecretKey secretKey;
    private final SimpleClientHttpRequestFactory requestFactory;
    private final RestClient restClient;

    @Autowired
    public QwBenefitClientImpl(QwProperties qwProperties, ObjectMapper objectMapper) {
        this(qwProperties, objectMapper, null);
    }

    QwBenefitClientImpl(QwProperties qwProperties, ObjectMapper objectMapper, RestClient restClient) {
        this.qwProperties = qwProperties;
        this.objectMapper = objectMapper;
        validateHttpModeConfiguration();
        this.secretKey = buildSecretKey();
        this.requestFactory = buildRequestFactory();
        this.restClient = restClient == null
                ? RestClient.builder()
                        .requestFactory(requestFactory)
                        .build()
                : restClient;
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
    public QwDeductionNotifyResponse notifyDeduction(QwDeductionNotifyRequest request) {
        ensureEnabled();
        if (qwProperties.getMode() == QwProperties.Mode.MOCK) {
            return mockDeductionNotify(request);
        }
        return invoke("abs.deduction.notify", request, QwDeductionNotifyResponse.class);
    }

    @Override
    public QwDeductionQueryResponse queryDeduction(QwDeductionQueryRequest request) {
        ensureEnabled();
        if (qwProperties.getMode() == QwProperties.Mode.MOCK) {
            return mockDeductionQuery(request);
        }
        return invoke("abs.deduction.query", request, QwDeductionQueryResponse.class);
    }

    @Override
    public QwOrderCancelResponse cancelOrder(QwOrderCancelRequest request) {
        ensureEnabled();
        if (qwProperties.getMode() == QwProperties.Mode.MOCK) {
            return mockOrderCancel(request);
        }
        return invoke("abs.order.cancel", request, QwOrderCancelResponse.class);
    }

    @Override
    public QwSignStatusResponse querySignStatus(QwSignStatusRequest request) {
        ensureEnabled();
        if (qwProperties.getMode() == QwProperties.Mode.MOCK) {
            return mockSignStatus(request);
        }
        return invoke("abs.sign.query", request, QwSignStatusResponse.class);
    }

    @Override
    public QwSignApplyResponse applySign(QwSignApplyRequest request) {
        ensureEnabled();
        if (qwProperties.getMode() == QwProperties.Mode.MOCK) {
            return mockSignApply(request);
        }
        return invoke("abs.sign.apply", request, QwSignApplyResponse.class);
    }

    @Override
    public QwSignConfirmResponse confirmSign(QwSignConfirmRequest request) {
        ensureEnabled();
        if (qwProperties.getMode() == QwProperties.Mode.MOCK) {
            return mockSignConfirm(request);
        }
        return invoke("abs.sign.confirm", request, QwSignConfirmResponse.class);
    }

    private <T> T invoke(String method, Object businessRequest, Class<T> responseType) {
        long timestamp = System.currentTimeMillis();
        URI targetUri = URI.create(qwProperties.getHttp().getBaseUrl() + qwProperties.getHttp().getMethodPath());
        log.info("qw http request prepared qw_mode={} qw_target_uri={} qw_method={} qw_partner_no={}",
                qwProperties.getMode(),
                targetUri,
                method,
                qwProperties.getPartnerNo());
        QwEnvelopeRequest request = new QwEnvelopeRequest(
                new QwRequestHead(
                        qwProperties.getPartnerNo(),
                        timestamp,
                        method,
                        qwProperties.getVersion(),
                        sign(method, String.valueOf(timestamp))
                ),
                encodeBusinessData(businessRequest)
        );
        try {
            String rawResponse = restClient.post()
                    .uri(targetUri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(String.class);
            return parseResponse(rawResponse, responseType);
        } catch (RestClientException exception) {
            if (UpstreamTimeoutDetector.isTimeout(exception)) {
                throw new UpstreamTimeoutException("QW upstream timeout", exception);
            }
            throw new BizException("QW_UPSTREAM_FAILED", "Failed to call QW upstream service");
        }
    }

    private void validateHttpModeConfiguration() {
        if (!isQweimobileHttpMode()) {
            return;
        }
        requireConfigured(qwProperties.getHttp().getBaseUrl(), "baseUrl");
        requireConfigured(qwProperties.getHttp().getMethodPath(), "methodPath");
        requireConfigured(qwProperties.getPartnerNo(), "partnerNo");
        requireConfigured(qwProperties.getVersion(), "version");
        requireConfigured(qwProperties.getSecurity().getSignKey(), "signKey");
        if (DEFAULT_SIGN_KEY_PLACEHOLDER.equals(qwProperties.getSecurity().getSignKey())) {
            throwInvalidHttpConfig("signKey must be provided by environment or secure configuration");
        }
        requireConfigured(qwProperties.getSecurity().getAesAlgorithm(), "aesAlgorithm");
        if (qwProperties.getSecurity().getCiphertextEncoding() == null) {
            throwInvalidHttpConfig("ciphertextEncoding must not be null");
        }
        if (qwProperties.getSecurity().getAesKeyEncoding() == QwProperties.AesKeyEncoding.RAW) {
            requireConfigured(qwProperties.getSecurity().getAesKey(), "aesKey");
            if (DEFAULT_AES_KEY_PLACEHOLDER.equals(qwProperties.getSecurity().getAesKey())) {
                throwInvalidHttpConfig("aesKey must be provided by environment or secure configuration");
            }
            return;
        }
        requireConfigured(qwProperties.getSecurity().getAesKeyBase64(), "aesKeyBase64");
        if (DEFAULT_AES_KEY_BASE64_PLACEHOLDER.equals(qwProperties.getSecurity().getAesKeyBase64())) {
            throwInvalidHttpConfig("aesKeyBase64 must be provided by environment or secure configuration");
        }
    }

    private boolean isQweimobileHttpMode() {
        return qwProperties.getMode() == QwProperties.Mode.HTTP
                || qwProperties.getMode() == QwProperties.Mode.QWEIMOBILE_HTTP;
    }

    private void requireConfigured(String value, String propertyName) {
        if (value == null || value.isBlank()) {
            throwInvalidHttpConfig(propertyName + " must not be blank");
        }
    }

    private void throwInvalidHttpConfig(String message) {
        throw new BizException("QW_HTTP_CONFIG_INVALID", message);
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
            Cipher cipher = Cipher.getInstance(qwProperties.getSecurity().getAesAlgorithm());
            byte[] payload;
            if (requiresGcm()) {
                byte[] iv = new byte[qwProperties.getSecurity().getIvLengthBytes()];
                secureRandom.nextBytes(iv);
                cipher.init(
                        Cipher.ENCRYPT_MODE,
                        secretKey,
                        new GCMParameterSpec(qwProperties.getSecurity().getGcmTagBits(), iv)
                );
                byte[] encrypted = cipher.doFinal(jsonBytes);
                payload = ByteBuffer.allocate(iv.length + encrypted.length)
                        .put(iv)
                        .put(encrypted)
                        .array();
            } else {
                cipher.init(Cipher.ENCRYPT_MODE, secretKey);
                payload = cipher.doFinal(jsonBytes);
            }
            return encodeCiphertext(payload);
        } catch (JsonProcessingException exception) {
            throw new BizException("QW_REQUEST_INVALID", "Failed to serialize QW request payload");
        } catch (GeneralSecurityException exception) {
            throw new BizException("QW_ENCRYPT_FAILED", "Failed to encrypt QW request payload");
        }
    }

    private String decodeBusinessData(String ciphertext) {
        try {
            byte[] payload = decodeCiphertext(ciphertext);
            Cipher cipher = Cipher.getInstance(qwProperties.getSecurity().getAesAlgorithm());
            byte[] plaintext;
            if (requiresGcm()) {
                if (payload.length <= qwProperties.getSecurity().getIvLengthBytes()) {
                    throw new BizException("QW_RESPONSE_INVALID", "QW response payload is invalid");
                }
                byte[] iv = new byte[qwProperties.getSecurity().getIvLengthBytes()];
                byte[] encrypted = new byte[payload.length - qwProperties.getSecurity().getIvLengthBytes()];
                System.arraycopy(payload, 0, iv, 0, iv.length);
                System.arraycopy(payload, iv.length, encrypted, 0, encrypted.length);
                cipher.init(
                        Cipher.DECRYPT_MODE,
                        secretKey,
                        new GCMParameterSpec(qwProperties.getSecurity().getGcmTagBits(), iv)
                );
                plaintext = cipher.doFinal(encrypted);
            } else {
                cipher.init(Cipher.DECRYPT_MODE, secretKey);
                plaintext = cipher.doFinal(payload);
            }
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (BizException exception) {
            throw exception;
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new BizException("QW_DECRYPT_FAILED", "Failed to decrypt QW response payload");
        }
    }

    private String sign(String method, String timestamp) {
        try {
            String payload = method
                    + qwProperties.getPartnerNo()
                    + timestamp
                    + qwProperties.getVersion()
                    + qwProperties.getSecurity().getSignKey();
            byte[] digest = MessageDigest.getInstance("MD5").digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Failed to build QW request signature", exception);
        }
    }

    private SimpleClientHttpRequestFactory buildRequestFactory() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(qwProperties.getHttp().getConnectTimeoutMs());
        requestFactory.setReadTimeout(qwProperties.getHttp().getReadTimeoutMs());
        return requestFactory;
    }

    private SecretKey buildSecretKey() {
        if (qwProperties.getSecurity().getAesKeyEncoding() == QwProperties.AesKeyEncoding.RAW) {
            String aesKey = qwProperties.getSecurity().getAesKey();
            if (aesKey == null || aesKey.isBlank()) {
                throw new BizException("QW_AES_KEY_INVALID", "QW AES key must not be blank");
            }
            byte[] keyBytes = aesKey.getBytes(StandardCharsets.UTF_8);
            validateAesKeyLength(keyBytes);
            return new SecretKeySpec(keyBytes, "AES");
        }
        return buildBase64SecretKey(qwProperties.getSecurity().getAesKeyBase64());
    }

    private SecretKey buildBase64SecretKey(String keyBase64) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(keyBase64);
            validateAesKeyLength(keyBytes);
            return new SecretKeySpec(keyBytes, "AES");
        } catch (IllegalArgumentException exception) {
            throw new BizException("QW_AES_KEY_INVALID", "QW AES key must be valid base64");
        }
    }

    private void validateAesKeyLength(byte[] keyBytes) {
        if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
            throw new BizException("QW_AES_KEY_INVALID", "QW AES key must be 16, 24 or 32 bytes");
        }
    }

    private String encodeCiphertext(byte[] payload) {
        return switch (qwProperties.getSecurity().getCiphertextEncoding()) {
            case BASE64 -> Base64.getEncoder().encodeToString(payload);
            case HEX -> HexFormat.of().formatHex(payload);
        };
    }

    private byte[] decodeCiphertext(String ciphertext) {
        return switch (qwProperties.getSecurity().getCiphertextEncoding()) {
            case BASE64 -> Base64.getDecoder().decode(ciphertext);
            case HEX -> HexFormat.of().parseHex(ciphertext);
        };
    }

    private boolean requiresGcm() {
        return qwProperties.getSecurity().getAesAlgorithm() != null
                && qwProperties.getSecurity().getAesAlgorithm().contains("/GCM/");
    }

    private void ensureEnabled() {
        if (!qwProperties.isEnabled()) {
            throw new BizException("QW_DISABLED", "QW integration is disabled");
        }
    }

    private QwMemberSyncResponse mockMemberSync(QwMemberSyncRequest request) {
        applyMockFault(
                request.partnerOrderNo(),
                request.uniqueId(),
                request.cardNo(),
                request.productCode(),
                TraceIdUtil.getTraceId()
        );
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
        applyMockFault(request.partnerOrderNo(), request.uniqueId(), TraceIdUtil.getTraceId());
        String redirectUrl = qwProperties.getHttp().getMockExerciseBaseUrl()
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

    private QwDeductionNotifyResponse mockDeductionNotify(QwDeductionNotifyRequest request) {
        applyMockFault(request.partnerOrderNo(), request.uniqueId(), TraceIdUtil.getTraceId());
        return new QwDeductionNotifyResponse("qw-order-" + request.partnerOrderNo());
    }

    private QwDeductionQueryResponse mockDeductionQuery(QwDeductionQueryRequest request) {
        applyMockFault(request.partnerOrderNo(), request.uniqueId(), TraceIdUtil.getTraceId());
        return new QwDeductionQueryResponse(2);
    }

    private QwOrderCancelResponse mockOrderCancel(QwOrderCancelRequest request) {
        applyMockFault(request.partnerOrderNo(), TraceIdUtil.getTraceId());
        return new QwOrderCancelResponse(true);
    }

    private QwSignStatusResponse mockSignStatus(QwSignStatusRequest request) {
        applyMockFault(request.accountNo(), request.phone(), request.merchantId(), TraceIdUtil.getTraceId());
        return new QwSignStatusResponse(request.accountNo() != null && request.accountNo().endsWith("8") ? 1 : 0);
    }

    private QwSignApplyResponse mockSignApply(QwSignApplyRequest request) {
        applyMockFault(request.accountNo(), request.phone(), request.merchantId(), TraceIdUtil.getTraceId());
        return new QwSignApplyResponse(mockUserSignId(request.accountNo()), "2026-04-29 10:00:00");
    }

    private QwSignConfirmResponse mockSignConfirm(QwSignConfirmRequest request) {
        applyMockFault(
                String.valueOf(request.userSignId()),
                request.verCode(),
                TraceIdUtil.getTraceId()
        );
        return new QwSignConfirmResponse(
                request.userSignId(),
                mockAgreementNo(request.userSignId())
        );
    }

    private void applyMockFault(String... candidates) {
        String rejectSource = null;
        String delaySource = null;
        for (String candidate : candidates) {
            if (candidate == null || candidate.isBlank()) {
                continue;
            }
            if (candidate.contains("_FAULT_TIMEOUT")) {
                throw new UpstreamTimeoutException("Mock QW timeout: " + candidate);
            }
            if (rejectSource == null && REJECT_PATTERN.matcher(candidate).matches()) {
                rejectSource = candidate;
            }
            if (delaySource == null && DELAY_PATTERN.matcher(candidate).matches()) {
                delaySource = candidate;
            }
        }
        if (rejectSource != null) {
            Matcher matcher = REJECT_PATTERN.matcher(rejectSource);
            matcher.matches();
            int code = Integer.parseInt(matcher.group(1));
            throw new BizException(code, QW_UPSTREAM_REJECTED, "Mock QW rejection code=" + code);
        }
        if (delaySource != null) {
            Matcher matcher = DELAY_PATTERN.matcher(delaySource);
            matcher.matches();
            long delayMs = Long.parseLong(matcher.group(1));
            try {
                Thread.sleep(Math.max(0L, delayMs));
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private Long mockUserSignId(String accountNo) {
        String lastFour = lastFour(accountNo);
        try {
            return Long.valueOf(lastFour);
        } catch (NumberFormatException exception) {
            return 0L;
        }
    }

    private String mockAgreementNo(Long userSignId) {
        String base = "mock-agreement-" + userSignId;
        String traceId = MDC.get(TraceIdUtil.TRACE_ID);
        if (traceId == null || traceId.isBlank()) {
            return base;
        }
        return base + "-" + shortTraceSuffix(traceId);
    }

    private String shortTraceSuffix(String traceId) {
        String normalized = traceId.replaceAll("[^A-Za-z0-9]", "");
        if (normalized.isBlank()) {
            return "trace";
        }
        return Integer.toUnsignedString(normalized.hashCode(), 16);
    }

    private String lastFour(String accountNo) {
        if (accountNo == null || accountNo.length() <= 4) {
            return accountNo == null ? "0000" : accountNo;
        }
        return accountNo.substring(accountNo.length() - 4);
    }

    private record QwEnvelopeRequest(
            QwRequestHead requestHead,
            String requestBody
    ) {
    }

    private record QwRequestHead(
            String partnerNo,
            Long timestamp,
            String method,
            String version,
            String sign
    ) {
    }
}
