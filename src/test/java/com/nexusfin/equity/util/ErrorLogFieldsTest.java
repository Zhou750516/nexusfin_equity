package com.nexusfin.equity.util;

import com.nexusfin.equity.exception.BizException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorLogFieldsTest {

    @Test
    void shouldResolveBizExceptionFields() {
        BizException exception = new BizException("QW_UPSTREAM_REJECTED", "用户未签约");

        assertThat(ErrorLogFields.errorNo(exception, "DEFAULT_ERROR"))
                .isEqualTo("QW_UPSTREAM_REJECTED");
        assertThat(ErrorLogFields.errorMsg(exception, "default message"))
                .isEqualTo("用户未签约");
    }

    @Test
    void shouldResolveRuntimeExceptionClassAndMessage() {
        RuntimeException exception = new IllegalStateException("worker tick failed");

        assertThat(ErrorLogFields.errorNo(exception, null)).isEqualTo("IllegalStateException");
        assertThat(ErrorLogFields.errorMsg(exception, null)).isEqualTo("worker tick failed");
    }

    @Test
    void shouldResolveRootCauseMessage() {
        RuntimeException exception = new RuntimeException("wrapper", new IllegalArgumentException("root cause failed"));

        assertThat(ErrorLogFields.errorMsg(exception, null)).isEqualTo("root cause failed");
    }

    @Test
    void shouldFallbackWhenMessageIsBlank() {
        RuntimeException exception = new IllegalStateException();

        assertThat(ErrorLogFields.errorMsg(exception, "default message")).isEqualTo("default message");
        assertThat(ErrorLogFields.errorMsg(exception, null)).isEqualTo("IllegalStateException");
    }

    @Test
    void shouldUseDefaultsWhenExceptionIsNull() {
        assertThat(ErrorLogFields.errorNo(null, "DEFAULT_ERROR")).isEqualTo("DEFAULT_ERROR");
        assertThat(ErrorLogFields.errorMsg(null, "default message")).isEqualTo("default message");
    }

    @Test
    void shouldTruncateLongMessages() {
        String longMessage = "x".repeat(700);

        assertThat(ErrorLogFields.errorMsg(new RuntimeException(longMessage), null))
                .hasSize(500)
                .isEqualTo("x".repeat(500));
    }
}
