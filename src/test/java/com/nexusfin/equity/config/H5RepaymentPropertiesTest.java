package com.nexusfin.equity.config;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.FileSystemResource;

import static org.assertj.core.api.Assertions.assertThat;

class H5RepaymentPropertiesTest {

    @Test
    void shouldDefaultRepaymentSmsRequiredToFalse() throws IOException {
        H5RepaymentProperties properties = bindApplicationDefaults();

        assertThat(properties.smsRequired()).isFalse();
    }

    @Test
    void shouldAllowRepaymentSmsRequiredOverride() {
        H5RepaymentProperties properties = bind(Map.of(
                "nexusfin.h5.repayment.sms-required", "true"
        ));

        assertThat(properties.smsRequired()).isTrue();
    }

    private H5RepaymentProperties bindApplicationDefaults() throws IOException {
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        List<PropertySource<?>> sources = loader.load("application.yml", new FileSystemResource("src/main/resources/application.yml"));
        Map<String, Object> flattened = new LinkedHashMap<>();
        for (PropertySource<?> source : sources) {
            if (source.getSource() instanceof Map<?, ?> map) {
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    flattened.put(String.valueOf(entry.getKey()), resolveDefaultPlaceholder(String.valueOf(entry.getValue())));
                }
            }
        }
        return bind(flattened);
    }

    private H5RepaymentProperties bind(Map<String, ?> values) {
        ConfigurationPropertySource source = new MapConfigurationPropertySource(values);
        return new Binder(new ConfigurationPropertySource[]{source})
                .bind("nexusfin.h5.repayment", H5RepaymentProperties.class)
                .orElseThrow(IllegalStateException::new);
    }

    private String resolveDefaultPlaceholder(String value) {
        if (value.startsWith("${") && value.endsWith("}") && value.contains(":")) {
            return value.substring(value.indexOf(':') + 1, value.length() - 1);
        }
        return value;
    }
}
