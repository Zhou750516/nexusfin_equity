package com.nexusfin.equity.service;

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

    @InjectMocks
    private PaymentProtocolServiceImpl paymentProtocolService;

    @Test
    void shouldPreferActiveAllinpayProtocolWhenResolvingBenefitOrder() {
        BenefitOrder order = buildOrder();
        MemberPaymentProtocol protocol = new MemberPaymentProtocol();
        protocol.setMemberId("mem-1");
        protocol.setExternalUserId("user-1");
        protocol.setProviderCode("QW_SIGN");
        protocol.setProtocolNo("AGRM-REAL-001");
        protocol.setSignRequestNo("998877");
        protocol.setProtocolStatus("ACTIVE");
        when(memberPaymentProtocolRepository.selectActiveByMemberId("mem-1", "QW_SIGN")).thenReturn(protocol);

        PaymentProtocolService.ResolvedPaymentProtocol resolved = paymentProtocolService.resolveForBenefitOrder(order);

        assertThat(resolved.protocolNo()).isEqualTo("AGRM-REAL-001");
        assertThat(resolved.signRequestNo()).isEqualTo("998877");
        assertThat(resolved.source()).isEqualTo("QW_SIGN");
        verify(memberPaymentProtocolRepository, never()).selectActiveByExternalUserId(any(), any());
    }

    @Test
    void shouldPreferActiveQwSignProtocolByExternalUserIdWhenMemberLookupMisses() {
        BenefitOrder order = buildOrder();
        MemberPaymentProtocol protocol = new MemberPaymentProtocol();
        protocol.setExternalUserId("user-1");
        protocol.setProviderCode("QW_SIGN");
        protocol.setProtocolNo("AGRM-EXT-001");
        protocol.setSignRequestNo("665544");
        protocol.setProtocolStatus("ACTIVE");
        when(memberPaymentProtocolRepository.selectActiveByMemberId("mem-1", "QW_SIGN")).thenReturn(null);
        when(memberPaymentProtocolRepository.selectActiveByExternalUserId("user-1", "QW_SIGN")).thenReturn(protocol);

        PaymentProtocolService.ResolvedPaymentProtocol resolved = paymentProtocolService.resolveForBenefitOrder(order);

        assertThat(resolved.protocolNo()).isEqualTo("AGRM-EXT-001");
        assertThat(resolved.signRequestNo()).isEqualTo("665544");
        assertThat(resolved.source()).isEqualTo("QW_SIGN");
    }

    @Test
    void shouldRejectWhenNoActiveQwSignReferenceExists() {
        BenefitOrder order = buildOrder();
        when(memberPaymentProtocolRepository.selectActiveByMemberId("mem-1", "QW_SIGN")).thenReturn(null);
        when(memberPaymentProtocolRepository.selectActiveByExternalUserId("user-1", "QW_SIGN")).thenReturn(null);

        assertThatThrownBy(() -> paymentProtocolService.resolveForBenefitOrder(order))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("PAY_PROTOCOL_NOT_FOUND");
    }

    @Test
    void shouldInsertActiveProtocolWhenNoCurrentRecordExists() {
        when(memberPaymentProtocolRepository.selectActiveByMemberId("mem-1", "QW_SIGN")).thenReturn(null);

        paymentProtocolService.saveActiveProtocol(new PaymentProtocolService.SavePaymentProtocolCommand(
                "mem-1",
                "user-1",
                "QW_SIGN",
                "AGRM-REAL-001",
                "998877",
                "KJ",
                LocalDateTime.of(2026, 4, 2, 10, 30)
        ));

        ArgumentCaptor<MemberPaymentProtocol> captor = ArgumentCaptor.forClass(MemberPaymentProtocol.class);
        verify(memberPaymentProtocolRepository).insert(captor.capture());
        assertThat(captor.getValue().getMemberId()).isEqualTo("mem-1");
        assertThat(captor.getValue().getExternalUserId()).isEqualTo("user-1");
        assertThat(captor.getValue().getProviderCode()).isEqualTo("QW_SIGN");
        assertThat(captor.getValue().getProtocolNo()).isEqualTo("AGRM-REAL-001");
        assertThat(captor.getValue().getSignRequestNo()).isEqualTo("998877");
        assertThat(captor.getValue().getProtocolStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void shouldUpdateExistingActiveProtocolWhenRecordAlreadyExists() {
        MemberPaymentProtocol existing = new MemberPaymentProtocol();
        existing.setId(1L);
        existing.setMemberId("mem-1");
        existing.setProviderCode("QW_SIGN");
        existing.setProtocolNo("AGRM-OLD-001");
        existing.setProtocolStatus("ACTIVE");
        when(memberPaymentProtocolRepository.selectActiveByMemberId("mem-1", "QW_SIGN")).thenReturn(existing);

        paymentProtocolService.saveActiveProtocol(new PaymentProtocolService.SavePaymentProtocolCommand(
                "mem-1",
                "user-1",
                "QW_SIGN",
                "AGRM-REAL-002",
                "112233",
                "KJ",
                LocalDateTime.of(2026, 4, 2, 11, 0)
        ));

        ArgumentCaptor<MemberPaymentProtocol> captor = ArgumentCaptor.forClass(MemberPaymentProtocol.class);
        verify(memberPaymentProtocolRepository).updateById(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(1L);
        assertThat(captor.getValue().getProtocolNo()).isEqualTo("AGRM-REAL-002");
        assertThat(captor.getValue().getSignRequestNo()).isEqualTo("112233");
    }

    private BenefitOrder buildOrder() {
        BenefitOrder order = new BenefitOrder();
        order.setBenefitOrderNo("ord-1");
        order.setMemberId("mem-1");
        order.setExternalUserId("user-1");
        return order;
    }
}
