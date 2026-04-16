package com.nexusfin.equity.service;

import com.nexusfin.equity.config.QwProperties;
import com.nexusfin.equity.entity.BenefitOrder;
import com.nexusfin.equity.entity.MemberPaymentProtocol;
import com.nexusfin.equity.exception.BizException;
import com.nexusfin.equity.repository.MemberPaymentProtocolRepository;
import com.nexusfin.equity.service.impl.PaymentProtocolServiceImpl;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentProtocolServiceTest {

    @Mock
    private MemberPaymentProtocolRepository memberPaymentProtocolRepository;

    @Mock
    private QwProperties qwProperties;

    @InjectMocks
    private PaymentProtocolServiceImpl paymentProtocolService;

    @Test
    void shouldPreferActiveAllinpayProtocolWhenResolvingBenefitOrder() {
        BenefitOrder order = buildOrder();
        MemberPaymentProtocol protocol = new MemberPaymentProtocol();
        protocol.setMemberId("mem-1");
        protocol.setExternalUserId("user-1");
        protocol.setProviderCode("ALLINPAY");
        protocol.setProtocolNo("AIP-REAL-001");
        protocol.setProtocolStatus("ACTIVE");
        when(memberPaymentProtocolRepository.selectActiveByMemberId("mem-1", "ALLINPAY")).thenReturn(protocol);

        PaymentProtocolService.ResolvedPaymentProtocol resolved = paymentProtocolService.resolveForBenefitOrder(order);

        assertThat(resolved.protocolNo()).isEqualTo("AIP-REAL-001");
        assertThat(resolved.source()).isEqualTo("ALLINPAY");
        verify(memberPaymentProtocolRepository, never()).selectActiveByExternalUserId(any(), any());
    }

    @Test
    void shouldUseConfiguredOverrideWhenRealProtocolMissingAndOverrideAllowed() {
        BenefitOrder order = buildOrder();
        when(memberPaymentProtocolRepository.selectActiveByMemberId("mem-1", "ALLINPAY")).thenReturn(null);
        when(memberPaymentProtocolRepository.selectActiveByExternalUserId("user-1", "ALLINPAY")).thenReturn(null);
        when(qwProperties.isAllowMemberSyncPayProtocolNoOverride()).thenReturn(true);
        when(qwProperties.getMemberSyncPayProtocolNoOverride()).thenReturn("AIP-MOCK-001");

        PaymentProtocolService.ResolvedPaymentProtocol resolved = paymentProtocolService.resolveForBenefitOrder(order);

        assertThat(resolved.protocolNo()).isEqualTo("AIP-MOCK-001");
        assertThat(resolved.source()).isEqualTo("TEST_OVERRIDE");
    }

    @Test
    void shouldRejectWhenNoRealProtocolAndOverrideUnavailable() {
        BenefitOrder order = buildOrder();
        when(memberPaymentProtocolRepository.selectActiveByMemberId("mem-1", "ALLINPAY")).thenReturn(null);
        when(memberPaymentProtocolRepository.selectActiveByExternalUserId("user-1", "ALLINPAY")).thenReturn(null);
        when(qwProperties.isAllowMemberSyncPayProtocolNoOverride()).thenReturn(false);

        assertThatThrownBy(() -> paymentProtocolService.resolveForBenefitOrder(order))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("PAY_PROTOCOL_NOT_FOUND");
    }

    @Test
    void shouldInsertActiveProtocolWhenNoCurrentRecordExists() {
        when(memberPaymentProtocolRepository.selectActiveByMemberId("mem-1", "ALLINPAY")).thenReturn(null);

        paymentProtocolService.saveActiveProtocol(new PaymentProtocolService.SavePaymentProtocolCommand(
                "mem-1",
                "user-1",
                "ALLINPAY",
                "AIP-REAL-001",
                "sign-req-1",
                "KJ",
                LocalDateTime.of(2026, 4, 2, 10, 30)
        ));

        ArgumentCaptor<MemberPaymentProtocol> captor = ArgumentCaptor.forClass(MemberPaymentProtocol.class);
        verify(memberPaymentProtocolRepository).insert(captor.capture());
        assertThat(captor.getValue().getMemberId()).isEqualTo("mem-1");
        assertThat(captor.getValue().getExternalUserId()).isEqualTo("user-1");
        assertThat(captor.getValue().getProviderCode()).isEqualTo("ALLINPAY");
        assertThat(captor.getValue().getProtocolNo()).isEqualTo("AIP-REAL-001");
        assertThat(captor.getValue().getProtocolStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void shouldUpdateExistingActiveProtocolWhenRecordAlreadyExists() {
        MemberPaymentProtocol existing = new MemberPaymentProtocol();
        existing.setId(1L);
        existing.setMemberId("mem-1");
        existing.setProviderCode("ALLINPAY");
        existing.setProtocolNo("AIP-OLD-001");
        existing.setProtocolStatus("ACTIVE");
        when(memberPaymentProtocolRepository.selectActiveByMemberId("mem-1", "ALLINPAY")).thenReturn(existing);

        paymentProtocolService.saveActiveProtocol(new PaymentProtocolService.SavePaymentProtocolCommand(
                "mem-1",
                "user-1",
                "ALLINPAY",
                "AIP-REAL-002",
                "sign-req-2",
                "KJ",
                LocalDateTime.of(2026, 4, 2, 11, 0)
        ));

        ArgumentCaptor<MemberPaymentProtocol> captor = ArgumentCaptor.forClass(MemberPaymentProtocol.class);
        verify(memberPaymentProtocolRepository).updateById(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(1L);
        assertThat(captor.getValue().getProtocolNo()).isEqualTo("AIP-REAL-002");
        assertThat(captor.getValue().getSignRequestNo()).isEqualTo("sign-req-2");
    }

    private BenefitOrder buildOrder() {
        BenefitOrder order = new BenefitOrder();
        order.setBenefitOrderNo("ord-1");
        order.setMemberId("mem-1");
        order.setExternalUserId("user-1");
        return order;
    }
}
