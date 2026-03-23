package com.nexusfin.equity.service;

import com.nexusfin.equity.dto.request.ExerciseCallbackRequest;
import com.nexusfin.equity.dto.request.GrantForwardCallbackRequest;
import com.nexusfin.equity.dto.request.RepaymentForwardCallbackRequest;
import com.nexusfin.equity.dto.request.RefundCallbackRequest;
import com.nexusfin.equity.entity.BenefitOrder;
import com.nexusfin.equity.entity.NotificationReceiveLog;
import com.nexusfin.equity.repository.BenefitOrderRepository;
import com.nexusfin.equity.repository.NotificationReceiveLogRepository;
import com.nexusfin.equity.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private BenefitOrderRepository benefitOrderRepository;

    @Mock
    private NotificationReceiveLogRepository notificationReceiveLogRepository;

    @Mock
    private FallbackDeductService fallbackDeductService;

    @Mock
    private IdempotencyService idempotencyService;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @Test
    void shouldTriggerFallbackOnGrantSuccessForFirstDeductFailOrder() {
        BenefitOrder order = new BenefitOrder();
        order.setBenefitOrderNo("ord-1");
        order.setOrderStatus("FIRST_DEDUCT_FAIL");
        when(idempotencyService.isProcessed("req-1")).thenReturn(false);
        when(benefitOrderRepository.selectById("ord-1")).thenReturn(order);
        when(notificationReceiveLogRepository.selectOne(any())).thenReturn(null);

        notificationService.handleGrant(new GrantForwardCallbackRequest(
                "req-1", "ord-1", "SUCCESS", 680000L, "loan-1", null, "2026-03-23T20:32:00", 1711197120L));

        verify(fallbackDeductService).triggerFallback(any(BenefitOrder.class), any(GrantForwardCallbackRequest.class));
        verify(benefitOrderRepository).updateById(order);
        verify(idempotencyService).markProcessed("req-1", "GRANT", "ord-1", "SUCCESS");
    }

    @Test
    void shouldIgnoreDuplicateRepaymentNotification() {
        when(idempotencyService.isProcessed("req-2")).thenReturn(true);

        notificationService.handleRepayment(new RepaymentForwardCallbackRequest(
                "req-2", "ord-2", "loan-2", 1, "PAID", 100L, "2026-03-23T20:33:00", 0, 1711197180L));

        verify(notificationReceiveLogRepository, never()).insert(any());
    }

    @Test
    void shouldUpdateOrderForExerciseAndRefund() {
        BenefitOrder exerciseOrder = new BenefitOrder();
        exerciseOrder.setBenefitOrderNo("ord-3");
        BenefitOrder refundOrder = new BenefitOrder();
        refundOrder.setBenefitOrderNo("ord-4");
        when(idempotencyService.isProcessed("req-3")).thenReturn(false);
        when(idempotencyService.isProcessed("req-4")).thenReturn(false);
        when(benefitOrderRepository.selectById("ord-3")).thenReturn(exerciseOrder);
        when(benefitOrderRepository.selectById("ord-4")).thenReturn(refundOrder);
        when(notificationReceiveLogRepository.selectOne(any())).thenReturn(null);

        notificationService.handleExercise(new ExerciseCallbackRequest("req-3", "ord-3", "SUCCESS", null, null));
        notificationService.handleRefund(new RefundCallbackRequest("req-4", "ord-4", "FAIL", 100L, null, null));

        verify(benefitOrderRepository).updateById(exerciseOrder);
        verify(benefitOrderRepository).updateById(refundOrder);
        assertThat(exerciseOrder.getOrderStatus()).isEqualTo("EXERCISE_SUCCESS");
        assertThat(refundOrder.getOrderStatus()).isEqualTo("REFUND_FAIL");
    }

    @Test
    void shouldWriteNotificationLogWhenRequestFirstSeen() {
        BenefitOrder order = new BenefitOrder();
        order.setBenefitOrderNo("ord-5");
        order.setOrderStatus("FIRST_DEDUCT_SUCCESS");
        when(idempotencyService.isProcessed("req-5")).thenReturn(false);
        when(benefitOrderRepository.selectById("ord-5")).thenReturn(order);
        when(notificationReceiveLogRepository.selectOne(any())).thenReturn(null);

        notificationService.handleGrant(new GrantForwardCallbackRequest(
                "req-5", "ord-5", "FAIL", 0L, "loan-5", "reason", "2026-03-23T20:34:00", 1711197240L));

        ArgumentCaptor<NotificationReceiveLog> captor = ArgumentCaptor.forClass(NotificationReceiveLog.class);
        verify(notificationReceiveLogRepository).insert(captor.capture());
        assertThat(captor.getValue().getNotifyType()).isEqualTo("GRANT_RESULT");
        assertThat(captor.getValue().getRequestId()).isEqualTo("req-5");
        verify(notificationReceiveLogRepository).updateById(captor.capture());
        assertThat(captor.getAllValues().get(1).getProcessStatus()).isEqualTo("PROCESSED");
    }

    @Test
    void shouldMarkNotificationFailedWhenRepaymentOrderMissing() {
        when(idempotencyService.isProcessed("req-6")).thenReturn(false);
        when(benefitOrderRepository.selectById("ord-missing")).thenReturn(null);
        when(notificationReceiveLogRepository.selectOne(any())).thenReturn(null);

        notificationService.handleRepayment(new RepaymentForwardCallbackRequest(
                "req-6", "ord-missing", "loan-missing", 1, "PAID", 100L, "2026-03-23T20:35:00", 0, 1711197300L));

        ArgumentCaptor<NotificationReceiveLog> captor = ArgumentCaptor.forClass(NotificationReceiveLog.class);
        verify(notificationReceiveLogRepository).updateById(captor.capture());
        assertThat(captor.getValue().getProcessStatus()).isEqualTo("FAILED");
        verify(idempotencyService, never()).markProcessed(any(), any(), any(), any());
    }
}
