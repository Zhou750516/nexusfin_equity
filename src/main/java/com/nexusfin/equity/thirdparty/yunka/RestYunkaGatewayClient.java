package com.nexusfin.equity.thirdparty.yunka;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.nexusfin.equity.config.YunkaProperties;
import com.nexusfin.equity.exception.BizException;
import com.nexusfin.equity.exception.ErrorCodes;
import com.nexusfin.equity.exception.UpstreamTimeoutException;
import com.nexusfin.equity.util.TraceIdUtil;
import com.nexusfin.equity.util.UpstreamTimeoutDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class RestYunkaGatewayClient implements YunkaGatewayClient {

    private static final Logger log = LoggerFactory.getLogger(RestYunkaGatewayClient.class);

    private final YunkaProperties yunkaProperties;
    private final RestClient restClient;

    public RestYunkaGatewayClient(
            YunkaProperties yunkaProperties,
            RestClient.Builder restClientBuilder
    ) {
        this.yunkaProperties = yunkaProperties;
        this.restClient = restClientBuilder
                .baseUrl(yunkaProperties.baseUrl())
                .requestFactory(requestFactory(yunkaProperties))
                .build();
    }

    @Override
    public YunkaGatewayResponse proxy(YunkaGatewayRequest request) {
        if (!yunkaProperties.enabled() || "MOCK".equalsIgnoreCase(yunkaProperties.mode())) {
            log.debug("traceId={} bizOrderNo={} requestId={} path={} yunka gateway request skipped mode={}",
                    TraceIdUtil.getTraceId(),
                    request.bizOrderNo(),
                    request.requestId(),
                    request.path(),
                    yunkaProperties.mode());
            return new YunkaGatewayResponse(0, "MOCK", JsonNodeFactory.instance.objectNode());
        }
        long startNanos = System.nanoTime();
        log.info("traceId={} bizOrderNo={} requestId={} path={} yunka gateway request begin",
                TraceIdUtil.getTraceId(),
                request.bizOrderNo(),
                request.requestId(),
                request.path());
        try {
            YunkaGatewayResponse response = restClient.post()
                    .uri(yunkaProperties.gatewayPath())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(YunkaGatewayResponse.class);
            long elapsedMs = elapsedMs(startNanos);
            if (response == null) {
                log.warn("traceId={} bizOrderNo={} requestId={} path={} elapsedMs={} errorNo={} errorMsg={}",
                        TraceIdUtil.getTraceId(),
                        request.bizOrderNo(),
                        request.requestId(),
                        request.path(),
                        elapsedMs,
                        ErrorCodes.YUNKA_RESPONSE_EMPTY,
                        "Yunka gateway returned empty response");
                return null;
            }
            if (response.code() == 0) {
                log.info("traceId={} bizOrderNo={} requestId={} path={} elapsedMs={} yunkaCode={} yunka gateway request success",
                        TraceIdUtil.getTraceId(),
                        request.bizOrderNo(),
                        request.requestId(),
                        request.path(),
                        elapsedMs,
                        response.code());
            } else {
                log.warn("traceId={} bizOrderNo={} requestId={} path={} elapsedMs={} yunkaCode={} errorNo={} errorMsg={}",
                        TraceIdUtil.getTraceId(),
                        request.bizOrderNo(),
                        request.requestId(),
                        request.path(),
                        elapsedMs,
                        response.code(),
                        ErrorCodes.YUNKA_UPSTREAM_REJECTED,
                        response.message());
            }
            return response;
        } catch (RestClientException exception) {
            long elapsedMs = elapsedMs(startNanos);
            if (UpstreamTimeoutDetector.isTimeout(exception)) {
                log.error("traceId={} bizOrderNo={} requestId={} path={} elapsedMs={} errorNo={} errorMsg={}",
                        TraceIdUtil.getTraceId(),
                        request.bizOrderNo(),
                        request.requestId(),
                        request.path(),
                        elapsedMs,
                        ErrorCodes.YUNKA_UPSTREAM_TIMEOUT,
                        "Yunka gateway timeout");
                throw new UpstreamTimeoutException("Yunka gateway timeout", exception);
            }
            log.error("traceId={} bizOrderNo={} requestId={} path={} elapsedMs={} errorNo={} errorMsg={}",
                    TraceIdUtil.getTraceId(),
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

    private static SimpleClientHttpRequestFactory requestFactory(YunkaProperties yunkaProperties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(yunkaProperties.connectTimeoutMs());
        requestFactory.setReadTimeout(yunkaProperties.readTimeoutMs());
        return requestFactory;
    }
}
