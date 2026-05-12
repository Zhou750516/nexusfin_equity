package com.nexusfin.equity.config;

import com.nexusfin.equity.util.TraceIdUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class TraceIdFilterTest {

    private final TraceIdFilter filter = new TraceIdFilter();

    @AfterEach
    void tearDown() {
        TraceIdUtil.clear();
    }

    @Test
    void shouldPreferFirstForwardedForIpAndClearMdcAfterRequest() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/joint-login");
        request.addHeader("X-Forwarded-For", "203.0.113.10, 10.0.0.8");
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> remoteIpDuringChain = new AtomicReference<>();

        FilterChain chain = (req, res) -> remoteIpDuringChain.set(MDC.get("remoteIp"));

        filter.doFilter(request, response, chain);

        assertThat(remoteIpDuringChain.get()).isEqualTo("203.0.113.10");
        assertThat(response.getHeader(TraceIdUtil.TRACE_ID_HEADER)).isNotBlank();
        assertThat(MDC.get("remoteIp")).isNull();
        assertThat(MDC.get(TraceIdUtil.TRACE_ID)).isNull();
    }

    @Test
    void shouldFallbackToXRealIpWhenForwardedForMissing() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/repayment/submit");
        request.addHeader("X-Real-IP", "198.51.100.25");
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> remoteIpDuringChain = new AtomicReference<>();

        FilterChain chain = (req, res) -> remoteIpDuringChain.set(MDC.get("remoteIp"));

        filter.doFilter(request, response, chain);

        assertThat(remoteIpDuringChain.get()).isEqualTo("198.51.100.25");
    }

    @Test
    void shouldFallbackToRemoteAddrWhenProxyHeadersMissing() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/loan/approval-result/APP-001");
        request.setRemoteAddr("192.0.2.88");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> remoteIpDuringChain = new AtomicReference<>();

        FilterChain chain = (req, res) -> remoteIpDuringChain.set(MDC.get("remoteIp"));

        filter.doFilter(request, response, chain);

        assertThat(remoteIpDuringChain.get()).isEqualTo("192.0.2.88");
    }
}
