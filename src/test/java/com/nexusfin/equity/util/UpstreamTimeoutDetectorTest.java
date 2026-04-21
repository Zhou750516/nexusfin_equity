package com.nexusfin.equity.util;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import static org.assertj.core.api.Assertions.assertThat;

class UpstreamTimeoutDetectorTest {

    @Test
    void shouldDetectTimeoutFromResourceAccessExceptionMessage() {
        ResourceAccessException exception = new ResourceAccessException("Read timed out");

        assertThat(UpstreamTimeoutDetector.isTimeout(exception)).isTrue();
    }

    @Test
    void shouldReturnFalseForGenericRestClientException() {
        RestClientException exception = new RestClientException("503 upstream unavailable");

        assertThat(UpstreamTimeoutDetector.isTimeout(exception)).isFalse();
    }
}
