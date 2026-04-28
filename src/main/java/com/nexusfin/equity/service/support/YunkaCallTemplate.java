package com.nexusfin.equity.service.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.nexusfin.equity.exception.BizException;
import com.nexusfin.equity.exception.ErrorCodes;
import com.nexusfin.equity.thirdparty.yunka.YunkaGatewayClient;
import com.nexusfin.equity.util.TraceIdUtil;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class YunkaCallTemplate {

    private static final Logger log = LoggerFactory.getLogger(YunkaCallTemplate.class);

    private final YunkaGatewayClient yunkaGatewayClient;

    public YunkaCallTemplate(YunkaGatewayClient yunkaGatewayClient) {
        this.yunkaGatewayClient = yunkaGatewayClient;
    }

    public YunkaGatewayClient.YunkaGatewayResponse execute(YunkaCall call) {
        return execute(call, this::requirePresentResponse);
    }

    public JsonNode executeForData(YunkaCall call) {
        return execute(call, this::requireSuccessfulData);
    }

    public <T> T execute(YunkaCall call, Function<YunkaGatewayClient.YunkaGatewayResponse, T> mapper) {
        long startNanos = System.nanoTime();
        log.info(
                "traceId={} bizOrderNo={} requestId={} memberId={} benefitOrderNo={} path={} scene={} yunka request begin",
                TraceIdUtil.getTraceId(),
                call.bizOrderNo(),
                call.requestId(),
                normalize(call.memberId()),
                normalize(call.benefitOrderNo()),
                call.path(),
                call.scene()
        );
        try {
            YunkaGatewayClient.YunkaGatewayResponse response = yunkaGatewayClient.proxy(call.toRequest());
            T result = mapper.apply(response);
            log.info(
                    "traceId={} bizOrderNo={} requestId={} memberId={} benefitOrderNo={} path={} scene={} elapsedMs={} yunka request success",
                    TraceIdUtil.getTraceId(),
                    call.bizOrderNo(),
                    call.requestId(),
                    normalize(call.memberId()),
                    normalize(call.benefitOrderNo()),
                    call.path(),
                    call.scene(),
                    elapsedMs(startNanos)
            );
            return result;
        } catch (BizException exception) {
            log.warn(
                    "traceId={} bizOrderNo={} requestId={} memberId={} benefitOrderNo={} path={} scene={} elapsedMs={} errorNo={} errorMsg={}",
                    TraceIdUtil.getTraceId(),
                    call.bizOrderNo(),
                    call.requestId(),
                    normalize(call.memberId()),
                    normalize(call.benefitOrderNo()),
                    call.path(),
                    call.scene(),
                    elapsedMs(startNanos),
                    exception.getErrorNo(),
                    exception.getErrorMsg()
            );
            throw exception;
        } catch (RuntimeException exception) {
            log.warn(
                    "traceId={} bizOrderNo={} requestId={} memberId={} benefitOrderNo={} path={} scene={} elapsedMs={} errorNo={} errorMsg={}",
                    TraceIdUtil.getTraceId(),
                    call.bizOrderNo(),
                    call.requestId(),
                    normalize(call.memberId()),
                    normalize(call.benefitOrderNo()),
                    call.path(),
                    call.scene(),
                    elapsedMs(startNanos),
                    ErrorCodes.YUNKA_UPSTREAM_FAILED,
                    defaultMessage(exception)
            );
            throw exception;
        }
    }

    public boolean isSuccessful(YunkaGatewayClient.YunkaGatewayResponse response) {
        return response != null && response.code() == 0;
    }

    public boolean hasData(YunkaGatewayClient.YunkaGatewayResponse response) {
        return isSuccessful(response)
                && response.data() != null
                && !response.data().isNull()
                && !response.data().isMissingNode();
    }

    public YunkaGatewayClient.YunkaGatewayResponse requirePresentResponse(
            YunkaGatewayClient.YunkaGatewayResponse response
    ) {
        if (response == null) {
            throw new BizException(ErrorCodes.YUNKA_RESPONSE_EMPTY, "Yunka gateway response is empty");
        }
        return response;
    }

    public JsonNode requireSuccessfulData(YunkaGatewayClient.YunkaGatewayResponse response) {
        YunkaGatewayClient.YunkaGatewayResponse presentResponse = requirePresentResponse(response);
        if (presentResponse.code() != 0) {
            throw new BizException(ErrorCodes.YUNKA_UPSTREAM_REJECTED, presentResponse.message());
        }
        return presentResponse.data() == null ? JsonNodeFactory.instance.objectNode() : presentResponse.data();
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private static String defaultMessage(RuntimeException exception) {
        return exception.getMessage() == null || exception.getMessage().isBlank()
                ? exception.getClass().getSimpleName()
                : exception.getMessage();
    }

    private static long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000L;
    }

    public record YunkaCall(
            String scene,
            String requestId,
            String path,
            String bizOrderNo,
            Object payload,
            String memberId,
            String benefitOrderNo
    ) {

        public static YunkaCall of(String scene, String requestId, String path, String bizOrderNo, Object payload) {
            return new YunkaCall(scene, requestId, path, bizOrderNo, payload, null, null);
        }

        public YunkaCall withMemberId(String memberId) {
            return new YunkaCall(scene, requestId, path, bizOrderNo, payload, memberId, benefitOrderNo);
        }

        public YunkaCall withBenefitOrderNo(String benefitOrderNo) {
            return new YunkaCall(scene, requestId, path, bizOrderNo, payload, memberId, benefitOrderNo);
        }

        public YunkaGatewayClient.YunkaGatewayRequest toRequest() {
            return new YunkaGatewayClient.YunkaGatewayRequest(requestId, path, bizOrderNo, payload);
        }
    }
}
