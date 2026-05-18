package com.nexusfin.equity.thirdparty.qw;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusfin.equity.config.QwProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class QwBenefitClientSpringContextTest {

    @Test
    void shouldStartSpringContextInMockMode() {
        QwProperties properties = new QwProperties();
        properties.setMode(QwProperties.Mode.MOCK);

        contextRunner(properties)
                .run(context -> assertThat(context).hasSingleBean(QwBenefitClientImpl.class));
    }

    @Test
    void shouldStartSpringContextInHttpModeWithCompleteConfig() {
        contextRunner(httpProperties())
                .run(context -> assertThat(context).hasSingleBean(QwBenefitClientImpl.class));
    }

    @Test
    void shouldFailSpringContextInHttpModeWhenDefaultPlaceholderSecretsRemainConfigured() {
        QwProperties properties = new QwProperties();
        properties.setMode(QwProperties.Mode.HTTP);

        contextRunner(properties)
                .run(context -> assertThat(context.getStartupFailure())
                        .isNotNull()
                        .rootCause()
                        .isInstanceOf(com.nexusfin.equity.exception.BizException.class)
                        .hasMessageContaining("QW_HTTP_CONFIG_INVALID"));
    }

    private ApplicationContextRunner contextRunner(QwProperties properties) {
        return new ApplicationContextRunner()
                .withBean(ObjectMapper.class, ObjectMapper::new)
                .withBean(QwProperties.class, () -> properties)
                .withBean(QwBenefitClientImpl.class);
    }

    private QwProperties httpProperties() {
        QwProperties properties = new QwProperties();
        properties.setMode(QwProperties.Mode.HTTP);
        properties.getHttp().setBaseUrl("https://t-api.test.qweimobile.com");
        properties.getHttp().setMethodPath("/api/abs/method");
        properties.setPartnerNo("abs");
        properties.setVersion("v1.0");
        properties.getSecurity().setSignKey("unit-test-sign-key-not-secret");
        properties.getSecurity().setAesKey("unit-test-aes-16");
        properties.getSecurity().setAesKeyEncoding(QwProperties.AesKeyEncoding.RAW);
        properties.getSecurity().setAesAlgorithm("AES/ECB/PKCS5Padding");
        properties.getSecurity().setCiphertextEncoding(QwProperties.CiphertextEncoding.HEX);
        return properties;
    }
}
