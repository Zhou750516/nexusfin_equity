package com.nexusfin.equity.util;

import java.util.UUID;

public final class RequestIdUtil {

    private RequestIdUtil() {
    }

    public static String nextId(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "");
    }
}
