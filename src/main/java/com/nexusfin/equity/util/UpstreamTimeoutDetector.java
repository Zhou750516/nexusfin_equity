package com.nexusfin.equity.util;

import java.util.Locale;
import org.springframework.web.client.RestClientException;

public final class UpstreamTimeoutDetector {

    private UpstreamTimeoutDetector() {
    }

    public static boolean isTimeout(RestClientException exception) {
        String message = exception.getMessage();
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
