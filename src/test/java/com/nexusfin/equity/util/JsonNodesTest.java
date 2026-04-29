package com.nexusfin.equity.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JsonNodesTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldReadTextAndRemarkWithFallbackWhenNodeIsMissingBlankOrNull() throws Exception {
        var data = objectMapper.readTree("""
                {
                  "loanId": "LN-001",
                  "remark": ""
                }
                """);

        assertThat(JsonNodes.readText(data, "loanId", "fallback")).isEqualTo("LN-001");
        assertThat(JsonNodes.readText(data, "missing", "fallback")).isEqualTo("fallback");
        assertThat(JsonNodes.readText(data, "remark", "fallback")).isEqualTo("fallback");
        assertThat(JsonNodes.readText(null, "loanId", "fallback")).isEqualTo("fallback");
        assertThat(JsonNodes.readRemark(data, "default remark")).isEqualTo("default remark");
    }

    @Test
    void shouldReadLongFromNumberTextAndFallbackToZeroForInvalidValues() throws Exception {
        var data = objectMapper.readTree("""
                {
                  "repayAmount": 101850,
                  "discount": "26.5",
                  "broken": "oops"
                }
                """);

        assertThat(JsonNodes.readLong(data, "repayAmount", "amount")).isEqualTo(101850L);
        assertThat(JsonNodes.readLong(data, "missing", "discount")).isEqualTo(27L);
        assertThat(JsonNodes.readLong(data, "broken")).isZero();
        assertThat(JsonNodes.readLong(data, "missing")).isZero();
    }
}
