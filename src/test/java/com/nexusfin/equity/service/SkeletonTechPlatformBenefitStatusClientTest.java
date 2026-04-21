package com.nexusfin.equity.service;

import com.nexusfin.equity.config.TechPlatformProperties;
import com.nexusfin.equity.exception.BizException;
import com.nexusfin.equity.service.impl.SkeletonTechPlatformBenefitStatusClient;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SkeletonTechPlatformBenefitStatusClientTest {

    @Test
    void shouldAcceptPushInMockMode() {
        TechPlatformProperties properties = new TechPlatformProperties();
        properties.setEnabled(true);
        properties.setMode(TechPlatformProperties.Mode.MOCK);
        SkeletonTechPlatformBenefitStatusClient client = new SkeletonTechPlatformBenefitStatusClient(properties);

        assertThatNoException().isThrownBy(() -> client.push(new TechPlatformBenefitStatusClient.BenefitStatusPushPayload(
                "evt-001",
                "BEN-20260418-001",
                "EXERCISE_SUCCESS",
                "SUCCESS",
                "xh-user-001",
                "PENDING"
        )));
    }

    @Test
    void shouldRejectPushInHttpModeBeforeContractReady() {
        TechPlatformProperties properties = new TechPlatformProperties();
        properties.setEnabled(true);
        properties.setMode(TechPlatformProperties.Mode.HTTP);
        SkeletonTechPlatformBenefitStatusClient client = new SkeletonTechPlatformBenefitStatusClient(properties);

        assertThatThrownBy(() -> client.push(new TechPlatformBenefitStatusClient.BenefitStatusPushPayload(
                "evt-001",
                "BEN-20260418-001",
                "EXERCISE_SUCCESS",
                "SUCCESS",
                "xh-user-001",
                "PENDING"
        ))).isInstanceOf(BizException.class)
                .hasMessageContaining("not configured");
    }
}
