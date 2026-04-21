package com.nexusfin.equity.exception;

import com.nexusfin.equity.dto.response.Result;
import com.nexusfin.equity.util.TraceIdUtil;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BizException.class)
    public Result<Void> handleBizException(BizException exception, HttpServletRequest request) {
        String errorNo = exception.getErrorNo();
        String errorMsg = exception.getErrorMsg();
        if (shouldLogAsError(exception.getCode())) {
            log.error("traceId={} bizOrderNo={} method={} path={} errorNo={} errorMsg={}",
                    TraceIdUtil.getTraceId(),
                    resolveBizOrderNo(request),
                    request.getMethod(),
                    request.getRequestURI(),
                    errorNo,
                    errorMsg);
        } else {
            log.warn("traceId={} bizOrderNo={} method={} path={} errorNo={} errorMsg={}",
                    TraceIdUtil.getTraceId(),
                    resolveBizOrderNo(request),
                    request.getMethod(),
                    request.getRequestURI(),
                    errorNo,
                    errorMsg);
        }
        if (exception.getCode() >= 0) {
            return Result.failure(exception.getCode(), errorMsg);
        }
        return Result.failure(errorNo, errorMsg);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidationException(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .orElse("Invalid request");
        log.warn("traceId={} bizOrderNo={} method={} path={} errorNo={} errorMsg={}",
                TraceIdUtil.getTraceId(),
                resolveBizOrderNo(request),
                request.getMethod(),
                request.getRequestURI(),
                ErrorCodes.PARAM_INVALID,
                message);
        return Result.failure(400, message);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<Void> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request
    ) {
        String message = exception.getName() + " format is invalid";
        log.warn("traceId={} bizOrderNo={} method={} path={} errorNo={} errorMsg={}",
                TraceIdUtil.getTraceId(),
                resolveBizOrderNo(request),
                request.getMethod(),
                request.getRequestURI(),
                ErrorCodes.PARAM_TYPE_INVALID,
                message);
        return Result.failure(400, message);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public Result<Void> handleNotFound(NoResourceFoundException exception, HttpServletRequest request) {
        String message = exception.getMessage();
        log.warn("traceId={} bizOrderNo={} method={} path={} errorNo={} errorMsg={}",
                TraceIdUtil.getTraceId(),
                resolveBizOrderNo(request),
                request.getMethod(),
                request.getRequestURI(),
                ErrorCodes.RESOURCE_NOT_FOUND,
                message);
        return Result.failure(404, message);
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleUnexpected(Exception exception, HttpServletRequest request) {
        log.error("traceId={} bizOrderNo={} method={} path={} errorNo={} errorMsg={}",
                TraceIdUtil.getTraceId(),
                resolveBizOrderNo(request),
                request.getMethod(),
                request.getRequestURI(),
                ErrorCodes.INTERNAL_SERVER_ERROR,
                exception.getMessage(),
                exception);
        return Result.failure(500, "Internal server error");
    }

    private boolean shouldLogAsError(int code) {
        return code >= 500;
    }

    @SuppressWarnings("unchecked")
    private String resolveBizOrderNo(HttpServletRequest request) {
        Object uriTemplateVariables = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (uriTemplateVariables instanceof Map<?, ?> variables) {
            for (String key : new String[]{"applicationId", "benefitOrderNo", "loanId", "repaymentId", "refundId"}) {
                Object value = variables.get(key);
                if (value instanceof String stringValue && !stringValue.isBlank()) {
                    return stringValue;
                }
            }
        }
        for (String paramName : new String[]{"applicationId", "benefitOrderNo", "loanId", "repaymentId", "refundId"}) {
            String value = request.getParameter(paramName);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "UNKNOWN";
    }
}
