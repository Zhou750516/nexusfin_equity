package com.nexusfin.equity.util;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MoneyUnitsTest {

    @Test
    void shouldConvertLongYuanToCentExactly() {
        assertThat(MoneyUnits.yuanToCent(3000L)).isEqualTo(300000L);
    }

    @Test
    void shouldConvertDecimalYuanToCentWithHalfUpRounding() {
        assertThat(MoneyUnits.yuanToCent(new BigDecimal("1018.50"))).isEqualTo(101850L);
    }

    @Test
    void shouldConvertCentToYuanWithTwoDecimalPlaces() {
        assertThat(MoneyUnits.centsToYuan(104500L)).isEqualByComparingTo("1045.00");
    }
}
