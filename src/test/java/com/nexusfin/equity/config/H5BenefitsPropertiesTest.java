package com.nexusfin.equity.config;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

class H5BenefitsPropertiesTest {

    @Test
    void shouldBindSeparateLoanAmountAndBenefitAmountForActivation() {
        H5BenefitsProperties properties = bind(Map.of(
                "nexusfin.h5.benefits.product-code", "abs001",
                "nexusfin.h5.benefits.benefit-redirect-public-base-url", "https://www.aibosoftware.com",
                "nexusfin.h5.benefits.activate.default-loan-amount", "300000",
                "nexusfin.h5.benefits.activate.default-benefit-amount", "30000",
                "nexusfin.h5.benefits.activate.supported-card-type", "huixuan_card",
                "nexusfin.h5.benefits.activate.success-message", "惠选卡开通成功"
        ));

        assertThat(properties.activate().defaultLoanAmount()).isEqualTo(300000L);
        assertThat(properties.activate().defaultBenefitAmount()).isEqualTo(30000L);
        assertThat(properties.benefitRedirectPublicBaseUrl()).isEqualTo("https://www.aibosoftware.com");
    }

    private H5BenefitsProperties bind(Map<String, String> values) {
        ConfigurationPropertySource source = new MapConfigurationPropertySource(values);
        return new Binder(new ConfigurationPropertySource[]{source})
                .bind("nexusfin.h5.benefits", H5BenefitsProperties.class)
                .orElseThrow(IllegalStateException::new);
    }
}
