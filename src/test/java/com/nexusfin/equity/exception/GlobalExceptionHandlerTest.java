package com.nexusfin.equity.exception;

import com.nexusfin.equity.dto.response.Result;
import com.nexusfin.equity.util.TraceIdUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.MDC;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @AfterEach
    void tearDown() {
        TraceIdUtil.clear();
    }

    @Test
    void shouldReturnBusinessErrorWithErrorNoPrefixedMessage(CapturedOutput output) {
        TraceIdUtil.bindTraceId("TRACE-BIZ-001");
        MDC.put("remoteIp", "198.51.100.10");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/loan/apply");
        request.addParameter("applicationId", "APP-001");

        Result<Void> result = handler.handleBizException(
                new BizException("YUNKA_UPSTREAM_FAILED", "Failed to call Yunka gateway"),
                request
        );

        assertThat(result.code()).isEqualTo(-1);
        assertThat(result.message()).isEqualTo("YUNKA_UPSTREAM_FAILED:Failed to call Yunka gateway");
        assertThat(output).contains("traceId=TRACE-BIZ-001");
        assertThat(output).contains("remoteIp=198.51.100.10");
        assertThat(output).contains("path=/api/loan/apply");
    }

    @Test
    void shouldReturnInternalServerErrorWithoutLeakingExceptionMessage() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/repayment/submit");

        Result<Void> result = handler.handleUnexpected(new IllegalStateException("db password leaked"), request);

        assertThat(result.code()).isEqualTo(500);
        assertThat(result.message()).isEqualTo("Internal server error");
    }
}
