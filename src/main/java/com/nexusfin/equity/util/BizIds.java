package com.nexusfin.equity.util;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public final class BizIds {

    private static final AtomicInteger LOAN_ID_SEQUENCE = new AtomicInteger((int) Instant.now().getEpochSecond());

    private BizIds() {
    }

    public static String newCompactUuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static String next(String prefix) {
        return prefix + "-" + newCompactUuid();
    }

    public static int newLoanId() {
        return LOAN_ID_SEQUENCE.updateAndGet(previous -> previous == Integer.MAX_VALUE ? 1 : previous + 1);
    }
}
