package com.nexusfin.equity.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(OutputCaptureExtension.class)
class QwFullPlaintextLogGuardTest {

    @Test
    void shouldLogWarningWhenFullPlaintextEnabledInAllowedProfile(CapturedOutput output) {
        QwProperties properties = new QwProperties();
        properties.getHttp().setLogPlaintextPayload(true);
        properties.getHttp().setLogFullPlaintextPayload(true);
        properties.getHttp().setLogFullPlaintextPayloadAllowedProfiles(java.util.List.of("test", "mysql-it"));
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("test");
        QwFullPlaintextLogGuard guard = new QwFullPlaintextLogGuard(properties, environment);

        guard.validateOnStartup();

        assertThat(output).contains("traceId=SYSTEM bizOrderNo=SYSTEM");
        assertThat(output).contains("errorNo=QW_FULL_PLAINTEXT_LOG_ENABLED");
        assertThat(output).contains("errorMsg=QW full plaintext payload logging is enabled");
        assertThat(output).contains("qw full plaintext payload logging enabled");
        assertThat(output).contains("profile=test");
    }

    @Test
    void shouldFailWhenFullPlaintextEnabledOutsideAllowedProfiles() {
        QwProperties properties = new QwProperties();
        properties.getHttp().setLogPlaintextPayload(true);
        properties.getHttp().setLogFullPlaintextPayload(true);
        properties.getHttp().setLogFullPlaintextPayloadAllowedProfiles(java.util.List.of("test", "mysql-it"));
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        QwFullPlaintextLogGuard guard = new QwFullPlaintextLogGuard(properties, environment);

        assertThatThrownBy(guard::validateOnStartup)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("QW_FULL_PLAINTEXT_LOG_NOT_ALLOWED")
                .hasMessageContaining("activeProfiles=[prod]")
                .hasMessageContaining("allowedProfiles=[test, mysql-it]");
    }
}
