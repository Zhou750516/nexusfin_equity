package com.nexusfin.equity.exception;

import com.nexusfin.equity.dto.response.Result;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldReturnBusinessErrorWithErrorNoPrefixedMessage() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/loan/apply");
        request.addParameter("applicationId", "APP-001");

        Result<Void> result = handler.handleBizException(
                new BizException("YUNKA_UPSTREAM_FAILED", "Failed to call Yunka gateway"),
                request
        );

        assertThat(result.code()).isEqualTo(-1);
        assertThat(result.message()).isEqualTo("YUNKA_UPSTREAM_FAILED:Failed to call Yunka gateway");
    }

    @Test
    void shouldReturnInternalServerErrorWithoutLeakingExceptionMessage() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/repayment/submit");

        Result<Void> result = handler.handleUnexpected(new IllegalStateException("db password leaked"), request);

        assertThat(result.code()).isEqualTo(500);
        assertThat(result.message()).isEqualTo("Internal server error");
    }
}
