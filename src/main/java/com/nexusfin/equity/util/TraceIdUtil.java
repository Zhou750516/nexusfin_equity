package com.nexusfin.equity.util;

import java.util.UUID;
import org.slf4j.MDC;

public final class TraceIdUtil {

    public static final String TRACE_ID = "traceId";
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

    public static void clear() {
        MDC.remove(TRACE_ID);
    }

    private static String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
