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
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
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
    private static final String BIZ_ORDER_NO_HEADER = "X-Biz-Order-No";
    private static final String TIMESTAMP_HEADER = "X-Timestamp";
    private static final String CHANNEL_CODE_HEADER = "X-Channel-Code";
    private static final String SIGNATURE_HEADER = "X-Signature";
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final YunkaProperties yunkaProperties;
    private final YunkaMode mode;
    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

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
                        .build()
        );
    }

    RestYunkaGatewayClient(YunkaProperties yunkaProperties, RestClient restClient) {
        this.yunkaProperties = yunkaProperties;
        this.mode = YunkaMode.from(yunkaProperties.mode());
        this.restClient = restClient;
        log.info("yunka gateway client initialized enabled={} mode={} baseUrl={} gatewayPath={}",
                yunkaProperties.enabled(),
                mode,
                yunkaProperties.baseUrl(),
                yunkaProperties.gatewayPath());
    }

    @Override
    public YunkaGatewayResponse proxy(YunkaGatewayRequest request) {
        if (!yunkaProperties.enabled() || mode == YunkaMode.MOCK) {
            log.debug("traceId={} bizOrderNo={} requestId={} path={} yunka gateway request skipped mode={}",
                    TraceIdUtil.getTraceId(),
                    request.bizOrderNo(),
                    request.requestId(),
                    request.path(),
                    mode);
            return new YunkaGatewayResponse(0, "MOCK", JsonNodeFactory.instance.objectNode());
        }
        long startNanos = System.nanoTime();
        String traceId = TraceIdUtil.getTraceId();
        String timestamp = currentTimestamp();
        log.info("traceId={} bizOrderNo={} requestId={} path={} requestBodyJson={} yunka gateway request begin",
                traceId,
                request.bizOrderNo(),
                request.requestId(),
                request.path(),
                toJson(request));
        try {
            YunkaGatewayResponse response = restClient.post()
                    .uri(yunkaProperties.gatewayPath())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(TraceIdUtil.TRACE_ID_HEADER, traceId)
                    .header(REQUEST_ID_HEADER, headerValue(request.requestId()))
                    .header(BIZ_ORDER_NO_HEADER, headerValue(request.bizOrderNo()))
                    .header(TIMESTAMP_HEADER, timestamp)
                    .header(CHANNEL_CODE_HEADER, headerValue(yunkaProperties.channelCode()))
                    .header(SIGNATURE_HEADER, headerValue(yunkaProperties.signature()))
                    .body(request)
                    .retrieve()
                    .body(YunkaGatewayResponse.class);
            long elapsedMs = elapsedMs(startNanos);
            if (response == null) {
                log.warn("traceId={} bizOrderNo={} requestId={} path={} elapsedMs={} errorNo={} errorMsg={} responseBodyJson={}",
                        traceId,
                        request.bizOrderNo(),
                        request.requestId(),
                        request.path(),
                        elapsedMs,
                        ErrorCodes.YUNKA_RESPONSE_EMPTY,
                        "Yunka gateway returned empty response",
                        "null");
                return null;
            }
            if (response.code() == 0) {
                log.info("traceId={} bizOrderNo={} requestId={} path={} elapsedMs={} yunkaCode={} responseBodyJson={} yunka gateway request success",
                        traceId,
                        request.bizOrderNo(),
                        request.requestId(),
                        request.path(),
                        elapsedMs,
                        response.code(),
                        toJson(response));
            } else {
                log.warn("traceId={} bizOrderNo={} requestId={} path={} elapsedMs={} yunkaCode={} errorNo={} errorMsg={} responseBodyJson={}",
                        traceId,
                        request.bizOrderNo(),
                        request.requestId(),
                        request.path(),
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
                log.error("traceId={} bizOrderNo={} requestId={} path={} elapsedMs={} errorNo={} errorMsg={}",
                        traceId,
                        request.bizOrderNo(),
                        request.requestId(),
                        request.path(),
                        elapsedMs,
                        ErrorCodes.YUNKA_UPSTREAM_TIMEOUT,
                        "Yunka gateway timeout");
                throw new UpstreamTimeoutException("Yunka gateway timeout", exception);
            }
            log.error("traceId={} bizOrderNo={} requestId={} path={} elapsedMs={} errorNo={} errorMsg={}",
                    traceId,
                    request.bizOrderNo(),
                    request.requestId(),
                    request.path(),
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

    private String currentTimestamp() {
        return OffsetDateTime.now(ZoneOffset.ofHours(8)).format(TIMESTAMP_FORMATTER);
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
