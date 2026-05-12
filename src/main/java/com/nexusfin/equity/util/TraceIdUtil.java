package com.nexusfin.equity.util;

import java.util.UUID;
import org.slf4j.MDC;

public final class TraceIdUtil {

    public static final String TRACE_ID = "traceId";
    public static final String REMOTE_IP = "remoteIp";
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    private TraceIdUtil() {
    }

    public static void bindTraceId(String traceId) {
        MDC.put(TRACE_ID, traceId == null || traceId.isBlank() ? generateTraceId() : traceId);
    }

    public static String getTraceId() {
        String traceId = MDC.get(TRACE_ID);
        if (traceId == null) {
            traceId = generateTraceId();
            MDC.put(TRACE_ID, traceId);
        }
        return traceId;
    }

    public static void bindRemoteIp(String remoteIp) {
        MDC.put(REMOTE_IP, normalize(remoteIp));
    }

    public static String getRemoteIp() {
        return normalize(MDC.get(REMOTE_IP));
    }

    public static void clear() {
        MDC.remove(TRACE_ID);
        MDC.remove(REMOTE_IP);
    }

    private static String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private static String normalize(String value) {
        if (value == null) {
            return "-";
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? "-" : trimmed;
    }
}
