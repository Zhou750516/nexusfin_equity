package com.nexusfin.equity.config;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

class TechPlatformPropertiesTest {

    @Test
    void shouldBindBenefitOrderNoticeDefaultPathFromApplicationYaml() throws IOException {
        TechPlatformProperties properties = bindApplicationDefaults();

        assertThat(properties.getPaths().getCreditStatusNotice()).isEqualTo("/guide/api/creditStatusNotice");
        assertThat(properties.getPaths().getLoanInfoNotice()).isEqualTo("/guide/api/loanInfoNotice");
        assertThat(properties.getPaths().getRepayInfoNotice()).isEqualTo("/guide/api/repayInfoNotice");
        assertThat(properties.getPaths().getBenefitOrderNotice()).isEqualTo("/huijuapi/vip/orderNotice");
    }

    private TechPlatformProperties bindApplicationDefaults() throws IOException {
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        List<PropertySource<?>> sources = loader.load("application.yml", new ClassPathResource("application.yml"));
        Map<String, Object> flattened = new java.util.LinkedHashMap<>();
        for (PropertySource<?> source : sources) {
            if (source.getSource() instanceof Map<?, ?> map) {
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    flattened.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
                }
            }
        }
        MapConfigurationPropertySource source = new MapConfigurationPropertySource(flattened);
        return new Binder(new ConfigurationPropertySource[]{source})
                .bind("nexusfin.third-party.tech-platform", TechPlatformProperties.class)
                .orElseThrow(IllegalStateException::new);
    }
}
