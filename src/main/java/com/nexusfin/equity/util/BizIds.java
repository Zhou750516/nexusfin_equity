package com.nexusfin.equity.util;

import java.util.UUID;

public final class BizIds {

    private BizIds() {
    }

    public static String newCompactUuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static String next(String prefix) {
        return prefix + "-" + newCompactUuid();
    }
}
