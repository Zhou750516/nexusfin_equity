package com.nexusfin.equity.util;

import java.net.SocketTimeoutException;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;

import static org.assertj.core.api.Assertions.assertThat;

class UpstreamTimeoutDetectorTest {

    @Test
    void shouldDetectTimeoutFromNestedSocketTimeoutCause() {
        ResourceAccessException exception = new ResourceAccessException(
                "Error while extracting response for type [com.example.Response]",
                new SocketTimeoutException("Read timed out")
        );

        assertThat(UpstreamTimeoutDetector.isTimeout(exception)).isTrue();
    }

    @Test
    void shouldIgnoreNonTimeoutClientFailure() {
        ResourceAccessException exception = new ResourceAccessException(
                "Connection closed by peer",
                new IOException("unexpected EOF")
        );

        assertThat(UpstreamTimeoutDetector.isTimeout(exception)).isFalse();
    }
}
