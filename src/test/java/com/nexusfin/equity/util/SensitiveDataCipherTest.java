package com.nexusfin.equity.util;

import com.nexusfin.equity.config.CryptoProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SensitiveDataCipherTest {

    @Test
    void shouldEncryptAndDecryptWithConfiguredDek() {
        SensitiveDataCipher cipher = new SensitiveDataCipher(new CryptoProperties());

        String encrypted = cipher.encrypt("13800138000");

        assertThat(encrypted).startsWith("DEK_USER_PROFILE:1:");
        assertThat(cipher.decrypt(encrypted)).isEqualTo("13800138000");
    }

    @Test
    void shouldDecodeInboundBase64PayloadByChannelMode() {
        CryptoProperties cryptoProperties = new CryptoProperties();
        cryptoProperties.getInbound().getChannelModes().put("QW", "BASE64");
        SensitiveDataCipher cipher = new SensitiveDataCipher(cryptoProperties);

        String decoded = cipher.decodeInbound("QW", "MTM4MDAxMzgwMDA=");

        assertThat(decoded).isEqualTo("13800138000");
    }
}
