package com.nexusfin.equity.service;

import com.nexusfin.equity.entity.BenefitStatusPushLog;
import com.nexusfin.equity.repository.BenefitStatusPushLogRepository;
import com.nexusfin.equity.service.BenefitStatusPushService.BenefitStatusPushCommand;
import com.nexusfin.equity.service.impl.BenefitStatusPushServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BenefitStatusPushServiceTest {

    @Mock
    private BenefitStatusPushLogRepository benefitStatusPushLogRepository;

    @Mock
    private TechPlatformBenefitStatusClient techPlatformBenefitStatusClient;

    @Test
    void shouldPersistPushLogWhenBenefitStatusEventTriggered() {
        BenefitStatusPushService benefitStatusPushService = new BenefitStatusPushServiceImpl(
                benefitStatusPushLogRepository,
                techPlatformBenefitStatusClient
        );

        benefitStatusPushService.pushStatus(new BenefitStatusPushCommand(
                "BEN-20260418-001",
                "EXERCISE_SUCCESS",
                "SUCCESS"
        ));

        ArgumentCaptor<BenefitStatusPushLog> captor = ArgumentCaptor.forClass(BenefitStatusPushLog.class);
        verify(benefitStatusPushLogRepository).insert(captor.capture());
        assertThat(captor.getValue().getBenefitOrderNo()).isEqualTo("BEN-20260418-001");
        assertThat(captor.getValue().getEventType()).isEqualTo("EXERCISE_SUCCESS");
        assertThat(captor.getValue().getPushStatus()).isEqualTo("INIT");
        verify(techPlatformBenefitStatusClient).push(any());
        verify(benefitStatusPushLogRepository).updateById(captor.capture());
        assertThat(captor.getAllValues().get(1).getPushStatus()).isEqualTo("SUCCESS");
    }
}
