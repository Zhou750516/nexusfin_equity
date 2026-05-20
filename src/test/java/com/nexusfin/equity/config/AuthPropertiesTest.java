package com.nexusfin.equity.config;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

class AuthPropertiesTest {

    @Test
    void shouldBindJointLoginRefreshExistingProfile() {
        AuthProperties disabled = bind(Map.of(
                "nexusfin.auth.joint-login.refresh-existing-profile", "false"
        ));
        AuthProperties enabled = bind(Map.of(
                "nexusfin.auth.joint-login.refresh-existing-profile", "true"
        ));

        assertThat(disabled.getJointLogin().isRefreshExistingProfile()).isFalse();
        assertThat(enabled.getJointLogin().isRefreshExistingProfile()).isTrue();
    }

    private AuthProperties bind(Map<String, String> values) {
        ConfigurationPropertySource source = new MapConfigurationPropertySource(values);
        return new Binder(new ConfigurationPropertySource[]{source})
                .bind("nexusfin.auth", AuthProperties.class)
                .orElseThrow(IllegalStateException::new);
    }
}
