package com.nexusfin.equity.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusfin.equity.config.TechPlatformProperties;
import com.nexusfin.equity.service.impl.TechPlatformBenefitStatusClientImpl;
import com.nexusfin.equity.thirdparty.techplatform.BenefitOrderNoticeRequest;
import com.nexusfin.equity.thirdparty.techplatform.TechPlatformClient;
import com.nexusfin.equity.thirdparty.techplatform.TechPlatformNotifyResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TechPlatformBenefitStatusClientImplTest {

    @Test
    void shouldAcceptPushInMockMode() {
        TechPlatformProperties properties = new TechPlatformProperties();
        properties.setEnabled(true);
        properties.setMode(TechPlatformProperties.Mode.MOCK);
        TechPlatformBenefitStatusClient client = new TechPlatformBenefitStatusClientImpl(properties, new ObjectMapper());

        assertThatNoException().isThrownBy(() -> client.push(new TechPlatformBenefitStatusClient.BenefitStatusPushPayload(
                "evt-001",
                "BEN-20260418-001",
                "EXERCISE_SUCCESS",
                "SUCCESS",
                "cid-001",
                "PENDING"
        )));
    }

    @Test
    void shouldDelegateBenefitStatusPushToTechPlatformOrderNotice() {
        TechPlatformClient techPlatformClient = mock(TechPlatformClient.class);
        when(techPlatformClient.notifyBenefitOrder(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new TechPlatformNotifyResponse("0", "ok"));
        TechPlatformBenefitStatusClient client =
                new TechPlatformBenefitStatusClientImpl(techPlatformClient, "/huijuapi/vip/orderNotice");

        assertThatNoException().isThrownBy(() -> client.push(new TechPlatformBenefitStatusClient.BenefitStatusPushPayload(
                "evt-001",
                "BEN-20260418-001",
                "EXERCISE_SUCCESS",
                "SUCCESS",
                "cid-001",
                "PENDING"
        )));

        ArgumentCaptor<BenefitOrderNoticeRequest> captor = ArgumentCaptor.forClass(BenefitOrderNoticeRequest.class);
        verify(techPlatformClient).notifyBenefitOrder(captor.capture());
        assertThat(captor.getValue().eventId()).isEqualTo("evt-001");
        assertThat(captor.getValue().benefitOrderNo()).isEqualTo("BEN-20260418-001");
        assertThat(captor.getValue().eventType()).isEqualTo("EXERCISE_SUCCESS");
        assertThat(captor.getValue().statusAfter()).isEqualTo("SUCCESS");
        assertThat(captor.getValue().externalUserId()).isEqualTo("cid-001");
        assertThat(captor.getValue().statusBefore()).isEqualTo("PENDING");
    }
}
