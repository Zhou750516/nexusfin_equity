package com.nexusfin.equity.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BizExceptionTest {

    @Test
    void shouldKeepErrorNoAndErrorMsgForBusinessError() {
        BizException exception = new BizException("YUNKA_UPSTREAM_FAILED", "Failed to call Yunka gateway");

        assertThat(exception.getCode()).isEqualTo(-1);
        assertThat(exception.getErrorNo()).isEqualTo("YUNKA_UPSTREAM_FAILED");
        assertThat(exception.getErrorMsg()).isEqualTo("Failed to call Yunka gateway");
        assertThat(exception.getMessage()).isEqualTo("YUNKA_UPSTREAM_FAILED: Failed to call Yunka gateway");
    }

    @Test
    void shouldResolveDefaultErrorNoFromHttpCode() {
        BizException exception = new BizException(401, "Unauthorized");

        assertThat(exception.getCode()).isEqualTo(401);
        assertThat(exception.getErrorNo()).isEqualTo(ErrorCodes.UNAUTHORIZED);
        assertThat(exception.getErrorMsg()).isEqualTo("Unauthorized");
    }
}
