package com.nexusfin.equity.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BizIdsTest {

    @Test
    void shouldGenerateCompactUuidWithoutHyphens() {
        String value = BizIds.newCompactUuid();

        assertThat(value).hasSize(32);
        assertThat(value).doesNotContain("-");
    }

    @Test
    void shouldGeneratePrefixedIds() {
        String requestId = BizIds.next("LA");

        assertThat(requestId).startsWith("LA-");
        assertThat(requestId.substring(3)).hasSize(32);
        assertThat(requestId.substring(3)).doesNotContain("-");
    }
}
