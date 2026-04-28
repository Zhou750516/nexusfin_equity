package com.nexusfin.equity.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class MoneyUnits {

    private static final BigDecimal CENTS_PER_YUAN = BigDecimal.valueOf(100L);

    private MoneyUnits() {
    }

    public static long yuanToCent(long yuanAmount) {
        return Math.multiplyExact(yuanAmount, 100L);
    }

    public static long yuanToCent(BigDecimal yuanAmount) {
        return yuanAmount.multiply(CENTS_PER_YUAN).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    public static BigDecimal centsToYuan(long cents) {
        return BigDecimal.valueOf(cents).divide(CENTS_PER_YUAN, 2, RoundingMode.UNNECESSARY);
    }
}
