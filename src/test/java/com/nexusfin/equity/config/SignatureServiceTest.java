package com.nexusfin.equity.config;

import com.nexusfin.equity.util.SignatureUtil;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SignatureServiceTest {

    @Test
    void shouldGenerateDeterministicSignatureForSameInput() {
        String first = SignatureUtil.sign("app", "1711195200", "nonce-1", "secret");
        String second = SignatureUtil.sign("app", "1711195200", "nonce-1", "secret");

        assertThat(first).isEqualTo(second);
        assertThat(first).hasSize(64);
    }

    @Test
    void shouldChangeSignatureWhenNonceChanges() {
        String first = SignatureUtil.sign("app", "1711195200", "nonce-1", "secret");
        String second = SignatureUtil.sign("app", "1711195200", "nonce-2", "secret");

        assertThat(first).isNotEqualTo(second);
    }
}
