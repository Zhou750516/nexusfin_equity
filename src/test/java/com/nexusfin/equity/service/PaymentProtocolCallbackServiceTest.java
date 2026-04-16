package com.nexusfin.equity.service;

import com.nexusfin.equity.entity.MemberInfo;
import com.nexusfin.equity.exception.BizException;
import com.nexusfin.equity.repository.MemberInfoRepository;
import com.nexusfin.equity.service.impl.PaymentProtocolCallbackServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentProtocolCallbackServiceTest {

    @Mock
    private MemberInfoRepository memberInfoRepository;

    @Mock
    private PaymentProtocolService paymentProtocolService;

    @Mock
    private IdempotencyService idempotencyService;

    @InjectMocks
    private PaymentProtocolCallbackServiceImpl paymentProtocolCallbackService;

    @Test
    void shouldResolveMemberByExternalUserIdAndSaveProtocol() {
        MemberInfo memberInfo = new MemberInfo();
        memberInfo.setMemberId("mem-1");
        memberInfo.setExternalUserId("user-1");
        when(idempotencyService.isProcessed("req-1")).thenReturn(false);
        when(memberInfoRepository.selectByExternalUserId("user-1")).thenReturn(memberInfo);

        paymentProtocolCallbackService.handleCallback(new PaymentProtocolCallbackService.PaymentProtocolCallbackCommand(
                "req-1",
                null,
                "user-1",
                "ALLINPAY",
                "AIP-REAL-001",
                "ACTIVE",
                "sign-1",
                "KJ",
                "2026-04-02T14:30:00"
        ));

        verify(paymentProtocolService).saveActiveProtocol(any(PaymentProtocolService.SavePaymentProtocolCommand.class));
        verify(idempotencyService).markProcessed("req-1", "PAYMENT_PROTOCOL_SYNC", "AIP-REAL-001", "ACTIVE");
    }

    @Test
    void shouldIgnoreDuplicateCallbackRequest() {
        when(idempotencyService.isProcessed("req-dup")).thenReturn(true);

        paymentProtocolCallbackService.handleCallback(new PaymentProtocolCallbackService.PaymentProtocolCallbackCommand(
                "req-dup",
                "mem-1",
                "user-1",
                "ALLINPAY",
                "AIP-REAL-001",
                "ACTIVE",
                "sign-1",
                "KJ",
                "2026-04-02T14:30:00"
        ));

        verify(paymentProtocolService, never()).saveActiveProtocol(any());
    }

    @Test
    void shouldRejectWhenMemberIdentifiersMissing() {
        assertThatThrownBy(() -> paymentProtocolCallbackService.handleCallback(
                new PaymentProtocolCallbackService.PaymentProtocolCallbackCommand(
                        "req-missing",
                        null,
                        null,
                        "ALLINPAY",
                        "AIP-REAL-001",
                        "ACTIVE",
                        "sign-1",
                        "KJ",
                        "2026-04-02T14:30:00"
                )))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("MEMBER_IDENTIFIER_REQUIRED");
    }
}
