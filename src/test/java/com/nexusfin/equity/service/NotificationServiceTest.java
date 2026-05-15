package com.nexusfin.equity.service;

import com.nexusfin.equity.dto.request.ExerciseCallbackRequest;
import com.nexusfin.equity.dto.request.LoanResultCallbackRequest;
import com.nexusfin.equity.dto.request.RepaymentResultCallbackRequest;
import com.nexusfin.equity.dto.request.RefundCallbackRequest;
import com.nexusfin.equity.entity.BenefitOrder;
import com.nexusfin.equity.entity.NotificationReceiveLog;
import com.nexusfin.equity.entity.PaymentRecord;
import com.nexusfin.equity.enums.PaymentStatusEnum;
import com.nexusfin.equity.enums.PaymentTypeEnum;
import com.nexusfin.equity.repository.BenefitOrderRepository;
import com.nexusfin.equity.repository.NotificationReceiveLogRepository;
import com.nexusfin.equity.repository.PaymentRecordRepository;
import com.nexusfin.equity.service.impl.NotificationServiceImpl;
import com.nexusfin.equity.thirdparty.qw.QwBenefitClient;
import com.nexusfin.equity.thirdparty.qw.QwDeductionNotifyRequest;
import com.nexusfin.equity.thirdparty.qw.QwDeductionNotifyResponse;
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
    private PaymentRecordRepository paymentRecordRepository;

    @Mock
    private FallbackDeductService fallbackDeductService;

    @Mock
    private IdempotencyService idempotencyService;

    @Mock
    private QwBenefitClient qwBenefitClient;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @Test
    void shouldTriggerFallbackOnGrantSuccessForFirstDeductFailOrder() {
        BenefitOrder order = new BenefitOrder();
        order.setBenefitOrderNo("ord-1");
        order.setOrderStatus("FIRST_DEDUCT_FAIL");
        order.setExternalUserId("user-1");
        order.setQwUserSignIdSnapshot(99887766L);
        when(idempotencyService.isProcessed("req-1")).thenReturn(false);
        when(benefitOrderRepository.selectById("ord-1")).thenReturn(order);
        when(notificationReceiveLogRepository.selectOne(any())).thenReturn(null);
        when(paymentRecordRepository.selectOne(any())).thenReturn(firstDeductRecord("ord-1", "serial-1"));
        when(qwBenefitClient.notifyDeduction(any())).thenReturn(new QwDeductionNotifyResponse("qw-order-1"));

        notificationService.handleGrant(new LoanResultCallbackRequest(
                "req-1", "user-1", null, "ord-1", "ord-1", "loan-1",
                7001, null, 680000L, 710000L, 1711197120L, null, null, null));

        verify(fallbackDeductService).triggerFallback(any(BenefitOrder.class), any(LoanResultCallbackRequest.class));
        ArgumentCaptor<QwDeductionNotifyRequest> deductionCaptor = ArgumentCaptor.forClass(QwDeductionNotifyRequest.class);
        verify(qwBenefitClient).notifyDeduction(deductionCaptor.capture());
        assertThat(deductionCaptor.getValue().uniqueId()).isEqualTo("user-1");
        assertThat(deductionCaptor.getValue().partnerOrderNo()).isEqualTo("ord-1");
        assertThat(deductionCaptor.getValue().serialNo()).isEqualTo("serial-1");
        assertThat(deductionCaptor.getValue().status()).isEqualTo(1);
        assertThat(deductionCaptor.getValue().userSignId()).isEqualTo(99887766L);
        verify(benefitOrderRepository).updateById(order);
        verify(idempotencyService).markProcessed("req-1", "GRANT", "ord-1", "7001");
    }

    @Test
    void shouldIgnoreDuplicateRepaymentNotification() {
        when(idempotencyService.isProcessed("req-2")).thenReturn(true);

        notificationService.handleRepayment(new RepaymentResultCallbackRequest(
                "req-2", "user-2", null, "ord-2", "ord-2", "loan-2", "swift-2",
                8001, 1, "1", null, 100L, 1711197180L, null, null));

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
        order.setExternalUserId("user-5");
        order.setQwUserSignIdSnapshot(99887765L);
        when(idempotencyService.isProcessed("req-5")).thenReturn(false);
        when(benefitOrderRepository.selectById("ord-5")).thenReturn(order);
        when(notificationReceiveLogRepository.selectOne(any())).thenReturn(null);
        when(paymentRecordRepository.selectOne(any())).thenReturn(firstDeductRecord("ord-5", "serial-5"));
        when(qwBenefitClient.notifyDeduction(any())).thenReturn(new QwDeductionNotifyResponse("qw-order-5"));

        notificationService.handleGrant(new LoanResultCallbackRequest(
                "req-5", "user-5", null, "ord-5", "ord-5", "loan-5",
                7003, "reason", 0L, null, 1711197240L, null, null, null));

        ArgumentCaptor<NotificationReceiveLog> captor = ArgumentCaptor.forClass(NotificationReceiveLog.class);
        verify(notificationReceiveLogRepository).insert(captor.capture());
        assertThat(captor.getValue().getNotifyType()).isEqualTo("GRANT_RESULT");
        assertThat(captor.getValue().getRequestId()).isEqualTo("req-5");
        verify(notificationReceiveLogRepository).updateById(captor.capture());
        assertThat(captor.getAllValues().get(1).getProcessStatus()).isEqualTo("PROCESSED");
        ArgumentCaptor<QwDeductionNotifyRequest> deductionCaptor = ArgumentCaptor.forClass(QwDeductionNotifyRequest.class);
        verify(qwBenefitClient).notifyDeduction(deductionCaptor.capture());
        assertThat(deductionCaptor.getValue().status()).isEqualTo(0);
        assertThat(deductionCaptor.getValue().serialNo()).isEqualTo("serial-5");
    }

    @Test
    void shouldMarkNotificationFailedWhenRepaymentOrderMissing() {
        when(idempotencyService.isProcessed("req-6")).thenReturn(false);
        when(benefitOrderRepository.selectById("ord-missing")).thenReturn(null);
        when(notificationReceiveLogRepository.selectOne(any())).thenReturn(null);

        notificationService.handleRepayment(new RepaymentResultCallbackRequest(
                "req-6", "user-6", null, "ord-missing", "ord-missing", "loan-missing", "swift-missing",
                8001, 1, "1", null, 100L, 1711197300L, null, null));

        ArgumentCaptor<NotificationReceiveLog> captor = ArgumentCaptor.forClass(NotificationReceiveLog.class);
        verify(notificationReceiveLogRepository).updateById(captor.capture());
        assertThat(captor.getValue().getProcessStatus()).isEqualTo("FAILED");
        verify(idempotencyService, never()).markProcessed(any(), any(), any(), any());
    }

    @Test
    void shouldKeepLoanOrderNoAndMarkRepaymentProcessingWithLatestStatus() {
        BenefitOrder order = new BenefitOrder();
        order.setBenefitOrderNo("ord-8");
        order.setLoanOrderNo("loan-old");
        when(idempotencyService.isProcessed("req-8")).thenReturn(false);
        when(benefitOrderRepository.selectById("ord-8")).thenReturn(order);
        when(notificationReceiveLogRepository.selectOne(any())).thenReturn(null);

        notificationService.handleRepayment(new RepaymentResultCallbackRequest(
                "req-8", "user-8", null, "ord-8", "ord-8", "loan-new", "swift-8",
                8004, 2, "1", "处理中", 100L, 1711197300L, 20L, 120L));

        verify(benefitOrderRepository).updateById(order);
        assertThat(order.getLoanOrderNo()).isEqualTo("loan-new");
        verify(idempotencyService).markProcessed("req-8", "REPAYMENT", "ord-8", "8004");
    }

    @Test
    void shouldKeepOrderPendingWhenLoanCallbackIsProcessing() {
        BenefitOrder order = new BenefitOrder();
        order.setBenefitOrderNo("ord-7");
        order.setOrderStatus("FIRST_DEDUCT_SUCCESS");
        order.setExternalUserId("user-7");
        when(idempotencyService.isProcessed("req-7")).thenReturn(false);
        when(benefitOrderRepository.selectById("ord-7")).thenReturn(order);
        when(notificationReceiveLogRepository.selectOne(any())).thenReturn(null);

        notificationService.handleGrant(new LoanResultCallbackRequest(
                "req-7", "user-7", null, "ord-7", "ord-7", "loan-7",
                7002, null, null, null, null, null, null, null));

        verify(benefitOrderRepository).updateById(order);
        assertThat(order.getGrantStatus()).isEqualTo("PROCESSING");
        verify(fallbackDeductService, never()).triggerFallback(any(), any());
        verify(qwBenefitClient, never()).notifyDeduction(any());
        verify(idempotencyService).markProcessed("req-7", "GRANT", "ord-7", "7002");
    }

    private PaymentRecord firstDeductRecord(String benefitOrderNo, String serialNo) {
        PaymentRecord record = new PaymentRecord();
        record.setPaymentNo("pay-" + serialNo);
        record.setBenefitOrderNo(benefitOrderNo);
        record.setPaymentType(PaymentTypeEnum.FIRST_DEDUCT.name());
        record.setPaymentStatus(PaymentStatusEnum.SUCCESS.name());
        record.setChannelTradeNo(serialNo);
        return record;
    }
}
