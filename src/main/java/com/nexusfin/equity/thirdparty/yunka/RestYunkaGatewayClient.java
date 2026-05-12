package com.nexusfin.equity.thirdparty.yunka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.nexusfin.equity.config.YunkaProperties;
import com.nexusfin.equity.exception.BizException;
import com.nexusfin.equity.exception.ErrorCodes;
import com.nexusfin.equity.exception.UpstreamTimeoutException;
import com.nexusfin.equity.util.TraceIdUtil;
import com.nexusfin.equity.util.UpstreamTimeoutDetector;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import java.util.function.Supplier;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class RestYunkaGatewayClient implements YunkaGatewayClient {

    private static final Logger log = LoggerFactory.getLogger(RestYunkaGatewayClient.class);
    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String TIMESTAMP_HEADER = "X-Timestamp";
    private static final String CHANNEL_CODE_HEADER = "X-Channel-Code";
    private static final String SIGNATURE_HEADER = "X-Signature";

    private final YunkaProperties yunkaProperties;
    private final YunkaMode mode;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final Supplier<String> timestampSupplier;

    @Autowired
    public RestYunkaGatewayClient(
            YunkaProperties yunkaProperties,
            RestClient.Builder restClientBuilder
    ) {
        this(
                yunkaProperties,
                restClientBuilder
                        .baseUrl(yunkaProperties.baseUrl())
                        .requestFactory(requestFactory(yunkaProperties))
                        .build(),
                new ObjectMapper(),
                () -> Long.toString(System.currentTimeMillis())
        );
    }

    RestYunkaGatewayClient(
            YunkaProperties yunkaProperties,
            RestClient restClient,
            Supplier<String> timestampSupplier
    ) {
        this(yunkaProperties, restClient, new ObjectMapper(), timestampSupplier);
    }

    RestYunkaGatewayClient(
            YunkaProperties yunkaProperties,
            RestClient restClient,
            ObjectMapper objectMapper,
            Supplier<String> timestampSupplier
    ) {
        this.yunkaProperties = yunkaProperties;
        this.mode = YunkaMode.from(yunkaProperties.mode());
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.timestampSupplier = timestampSupplier;
        log.info("yunka gateway client initialized enabled={} mode={} baseUrl={} gatewayPath={} yunka_channel_code={}",
                yunkaProperties.enabled(),
                mode,
                yunkaProperties.baseUrl(),
                yunkaProperties.gatewayPath(),
                maskChannelCode(yunkaProperties.channelCode()));
    }

    @Override
    public YunkaGatewayResponse proxy(YunkaGatewayRequest request) {
        if (!yunkaProperties.enabled() || mode == YunkaMode.MOCK) {
            log.debug("traceId={} requestId={} path={} yunka gateway request skipped mode={}",
                    TraceIdUtil.getTraceId(),
                    request.requestId(),
                    request.path(),
                    mode);
            return new YunkaGatewayResponse(0, "MOCK", JsonNodeFactory.instance.objectNode());
        }
        long startNanos = System.nanoTime();
        String traceId = TraceIdUtil.getTraceId();
        String timestamp = timestampSupplier.get();
        String dataJson = toJson(request.data());
        String requestBodyJson = toJson(request);
        String signature = sign(dataJson, request.requestId(), timestamp);
        String xTimestamp = timestamp;
        String xRequestId = headerValue(request.requestId());
        String xChannelCode = headerValue(yunkaProperties.channelCode());
        String signaturePrefix = maskSignature(signature);
        // X-Request-Id is the idempotency key. Same-business retries should reuse it instead of generating a new value.
        log.info("traceId={} requestId={} path={} xTimestamp={} xChannelCode={} xRequestId={} signaturePrefix={} requestBodyJson={} yunka gateway request begin",
                traceId,
                request.requestId(),
                request.path(),
                xTimestamp,
                xChannelCode,
                xRequestId,
                signaturePrefix,
                requestBodyJson);
        try {
            byte[] responseBodyBytes = restClient.post()
                    .uri(yunkaProperties.gatewayPath())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(TraceIdUtil.TRACE_ID_HEADER, traceId)
                    .header(REQUEST_ID_HEADER, xRequestId)
                    .header(TIMESTAMP_HEADER, xTimestamp)
                    .header(CHANNEL_CODE_HEADER, xChannelCode)
                    .header(SIGNATURE_HEADER, signature)
                    .body(requestBodyJson)
                    .retrieve()
                    .body(byte[].class);
            String responseBody = decodeResponseBody(responseBodyBytes);
            YunkaGatewayResponse response = parseResponse(responseBody);
            long elapsedMs = elapsedMs(startNanos);
            if (response == null) {
                log.warn("traceId={} requestId={} path={} xTimestamp={} xChannelCode={} xRequestId={} elapsedMs={} errorNo={} errorMsg={} responseBodyJson={}",
                        traceId,
                        request.requestId(),
                        request.path(),
                        xTimestamp,
                        xChannelCode,
                        xRequestId,
                        elapsedMs,
                        ErrorCodes.YUNKA_RESPONSE_EMPTY,
                        "Yunka gateway returned empty response",
                        "null");
                return null;
            }
            if (response.code() == 0) {
                log.info("traceId={} requestId={} path={} xTimestamp={} xChannelCode={} xRequestId={} elapsedMs={} yunkaCode={} responseBodyJson={} yunka gateway request success",
                        traceId,
                        request.requestId(),
                        request.path(),
                        xTimestamp,
                        xChannelCode,
                        xRequestId,
                        elapsedMs,
                        response.code(),
                        toJson(response));
            } else {
                log.warn("traceId={} requestId={} path={} xTimestamp={} xChannelCode={} xRequestId={} elapsedMs={} yunkaCode={} errorNo={} errorMsg={} responseBodyJson={}",
                        traceId,
                        request.requestId(),
                        request.path(),
                        xTimestamp,
                        xChannelCode,
                        xRequestId,
                        elapsedMs,
                        response.code(),
                        ErrorCodes.YUNKA_UPSTREAM_REJECTED,
                        response.message(),
                        toJson(response));
            }
            return response;
        } catch (RestClientException exception) {
            long elapsedMs = elapsedMs(startNanos);
            if (UpstreamTimeoutDetector.isTimeout(exception)) {
                log.error("traceId={} requestId={} path={} xTimestamp={} xChannelCode={} xRequestId={} elapsedMs={} errorNo={} errorMsg={}",
                        traceId,
                        request.requestId(),
                        request.path(),
                        xTimestamp,
                        xChannelCode,
                        xRequestId,
                        elapsedMs,
                        ErrorCodes.YUNKA_UPSTREAM_TIMEOUT,
                        "Yunka gateway timeout");
                throw new UpstreamTimeoutException("Yunka gateway timeout", exception);
            }
            log.error("traceId={} requestId={} path={} xTimestamp={} xChannelCode={} xRequestId={} elapsedMs={} errorNo={} errorMsg={}",
                    traceId,
                    request.requestId(),
                    request.path(),
                    xTimestamp,
                    xChannelCode,
                    xRequestId,
                    elapsedMs,
                    ErrorCodes.YUNKA_UPSTREAM_FAILED,
                    exception.getMessage());
            throw new BizException(ErrorCodes.YUNKA_UPSTREAM_FAILED, "Failed to call Yunka gateway");
        }
    }

    private long elapsedMs(long startNanos) {
        return java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
    }

    private String toJson(Object value) {
        if (value == null) {
            return "null";
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            return "\"SERIALIZE_FAILED\"";
        }
    }

    private String headerValue(String value) {
        return value == null ? "" : value;
    }

    private String decodeResponseBody(byte[] responseBodyBytes) {
        if (responseBodyBytes == null || responseBodyBytes.length == 0) {
            return null;
        }
        return new String(responseBodyBytes, StandardCharsets.UTF_8);
    }

    private YunkaGatewayResponse parseResponse(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(responseBody, YunkaGatewayResponse.class);
        } catch (JsonProcessingException exception) {
            throw new BizException(ErrorCodes.YUNKA_UPSTREAM_FAILED, "Failed to parse Yunka gateway response");
        }
    }

    private String sign(String dataJson, String requestId, String timestamp) {
        try {
            String payload = "data=" + dataJson + "&requestId=" + requestId + "&timestamp=" + timestamp;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(yunkaProperties.signSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getEncoder().encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new BizException(ErrorCodes.YUNKA_UPSTREAM_FAILED, "Failed to build Yunka signature");
        }
    }

    private static String maskChannelCode(String channelCode) {
        if (channelCode == null || channelCode.isBlank()) {
            return "-";
        }
        return channelCode;
    }

    private static String maskSignature(String signature) {
        if (signature == null || signature.isBlank()) {
            return "-";
        }
        return signature.substring(0, Math.min(signature.length(), 8));
    }

    private static SimpleClientHttpRequestFactory requestFactory(YunkaProperties yunkaProperties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(yunkaProperties.connectTimeoutMs());
        requestFactory.setReadTimeout(yunkaProperties.readTimeoutMs());
        return requestFactory;
    }

    private enum YunkaMode {
        MOCK,
        REST;

        private static YunkaMode from(String rawMode) {
            if (rawMode == null || rawMode.isBlank()) {
                throw new IllegalArgumentException("Unsupported Yunka mode: blank. Supported modes: MOCK, REST");
            }
            try {
                return YunkaMode.valueOf(rawMode.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException(
                        "Unsupported Yunka mode: " + rawMode + ". Supported modes: MOCK, REST",
                        exception
                );
            }
        }
    }
}
