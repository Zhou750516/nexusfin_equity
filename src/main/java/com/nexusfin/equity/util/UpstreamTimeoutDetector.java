package com.nexusfin.equity.util;

import java.util.Locale;
import java.util.Set;
import org.springframework.web.client.RestClientException;

public final class UpstreamTimeoutDetector {

    private static final Set<String> TIMEOUT_CLASS_NAMES = Set.of(
            "sockettimeoutexception",
            "httptimeoutexception"
    );

    private UpstreamTimeoutDetector() {
    }

    public static boolean isTimeout(RestClientException exception) {
        for (Throwable current = exception; current != null; current = current.getCause()) {
            if (hasTimeoutMarker(current)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasTimeoutMarker(Throwable throwable) {
        String className = throwable.getClass().getSimpleName().toLowerCase(Locale.ROOT);
        if (TIMEOUT_CLASS_NAMES.contains(className)) {
            return true;
        }
        String message = throwable.getMessage();
        if (message == null || message.isBlank()) {
            return false;
        }
        String normalized = message.toLowerCase(Locale.ROOT);
        return normalized.contains("timed out")
                || normalized.contains("timeout")
                || normalized.contains("read timed out")
                || normalized.contains("connect timed out");
    }
}
