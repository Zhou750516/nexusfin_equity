package com.nexusfin.equity.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(OutputCaptureExtension.class)
class QwPayProtocolOverrideGuardTest {

    @Test
    void shouldLogWarningWhenOverrideEnabledInAllowedProfile(CapturedOutput output) {
        QwProperties properties = new QwProperties();
        properties.getPayment().setMemberSyncPayProtocolNoOverride("AIP-MOCK-001");
        properties.getPayment().setAllowMemberSyncPayProtocolNoOverride(true);
        properties.getPayment().setMemberSyncPayProtocolNoOverrideAllowedProfiles(java.util.List.of("test", "mysql-it"));
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("test");
        QwPayProtocolOverrideGuard guard = new QwPayProtocolOverrideGuard(properties, environment);

        guard.validateOnStartup();

        assertThat(output).contains("traceId=SYSTEM bizOrderNo=SYSTEM");
        assertThat(output).contains("qw payProtocolNo override enabled");
        assertThat(output).contains("profile=test");
    }

    @Test
    void shouldFailWhenOverrideConfiguredButSwitchDisabled() {
        QwProperties properties = new QwProperties();
        properties.getPayment().setMemberSyncPayProtocolNoOverride("AIP-MOCK-001");
        properties.getPayment().setAllowMemberSyncPayProtocolNoOverride(false);
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("test");
        QwPayProtocolOverrideGuard guard = new QwPayProtocolOverrideGuard(properties, environment);

        assertThatThrownBy(guard::validateOnStartup)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("QW_MEMBER_SYNC_PAY_PROTOCOL_NO_OVERRIDE")
                .hasMessageContaining("QW_ALLOW_MEMBER_SYNC_PAY_PROTOCOL_NO_OVERRIDE");
    }

    @Test
    void shouldFailWhenOverrideConfiguredOutsideAllowedProfiles() {
        QwProperties properties = new QwProperties();
        properties.getPayment().setMemberSyncPayProtocolNoOverride("AIP-MOCK-001");
        properties.getPayment().setAllowMemberSyncPayProtocolNoOverride(true);
        properties.getPayment().setMemberSyncPayProtocolNoOverrideAllowedProfiles(java.util.List.of("test", "mysql-it"));
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        QwPayProtocolOverrideGuard guard = new QwPayProtocolOverrideGuard(properties, environment);

        assertThatThrownBy(guard::validateOnStartup)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("activeProfiles=[prod]")
                .hasMessageContaining("allowedProfiles=[test, mysql-it]");
    }
}
