package com.nexusfin.equity.util;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.math.RoundingMode;

public final class JsonNodes {

    private JsonNodes() {
    }

    public static String readText(JsonNode data, String fieldName, String fallback) {
        if (data == null || data.isNull()) {
            return fallback;
        }
        String value = data.path(fieldName).asText();
        return value == null || value.isBlank() ? fallback : value;
    }

    public static long readLong(JsonNode data, String... fields) {
        if (data == null || data.isNull()) {
            return 0L;
        }
        for (String field : fields) {
            JsonNode value = data.path(field);
            if (!value.isMissingNode() && !value.isNull()) {
                if (value.isNumber()) {
                    return value.asLong();
                }
                String text = value.asText();
                if (!text.isBlank()) {
                    try {
                        return new BigDecimal(text).setScale(0, RoundingMode.HALF_UP).longValue();
                    } catch (NumberFormatException ignored) {
                        return 0L;
                    }
                }
            }
        }
        return 0L;
    }

    public static String readRemark(JsonNode data, String fallback) {
        return readText(data, "remark", fallback);
    }
}
