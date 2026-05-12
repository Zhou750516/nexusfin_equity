package com.nexusfin.equity.config;

import com.nexusfin.equity.util.TraceIdUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class TraceIdFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(TraceIdFilter.class);

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            TraceIdUtil.bindTraceId(request.getHeader(TraceIdUtil.TRACE_ID_HEADER));
            TraceIdUtil.bindRemoteIp(resolveRemoteIp(request));
            response.setHeader(TraceIdUtil.TRACE_ID_HEADER, TraceIdUtil.getTraceId());
            log.info("traceId={} remoteIp={} method={} path={} request accepted",
                    TraceIdUtil.getTraceId(),
                    TraceIdUtil.getRemoteIp(),
                    request.getMethod(),
                    request.getRequestURI());
            filterChain.doFilter(request, response);
        } finally {
            TraceIdUtil.clear();
        }
    }

    private String resolveRemoteIp(HttpServletRequest request) {
        String forwardedFor = firstForwardedFor(request.getHeader("X-Forwarded-For"));
        if (forwardedFor != null) {
            return forwardedFor;
        }
        String realIp = normalize(request.getHeader("X-Real-IP"));
        if (realIp != null) {
            return realIp;
        }
        return normalize(request.getRemoteAddr());
    }

    private String firstForwardedFor(String forwardedFor) {
        if (forwardedFor == null || forwardedFor.isBlank()) {
            return null;
        }
        for (String candidate : forwardedFor.split(",")) {
            String value = normalize(candidate);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty() || "unknown".equalsIgnoreCase(trimmed)) {
            return null;
        }
        return trimmed;
    }
}
