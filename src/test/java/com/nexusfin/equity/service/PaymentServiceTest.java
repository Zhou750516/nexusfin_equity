package com.nexusfin.equity.service;

import com.nexusfin.equity.dto.request.DeductionCallbackRequest;
import com.nexusfin.equity.dto.response.PaymentStatusResponse;
import com.nexusfin.equity.entity.BenefitOrder;
import com.nexusfin.equity.entity.PaymentRecord;
import com.nexusfin.equity.exception.BizException;
import com.nexusfin.equity.repository.BenefitOrderRepository;
import com.nexusfin.equity.repository.PaymentRecordRepository;
import com.nexusfin.equity.service.impl.PaymentServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private BenefitOrderRepository benefitOrderRepository;

    @Mock
    private PaymentRecordRepository paymentRecordRepository;

    @Mock
    private DownstreamSyncService downstreamSyncService;

    @Mock
    private IdempotencyService idempotencyService;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @Test
    void shouldReturnExistingPaymentForDuplicateCallback() {
        PaymentRecord existing = new PaymentRecord();
        existing.setPaymentNo("pay-1");
        existing.setBenefitOrderNo("ord-1");
        existing.setPaymentType("FIRST_DEDUCT");
        existing.setPaymentStatus("SUCCESS");
        when(paymentRecordRepository.selectOne(any())).thenReturn(existing);

        PaymentStatusResponse response = paymentService.handleFirstDeductCallback(request("req-1", "ord-1", "SUCCESS"));

        assertThat(response.paymentNo()).isEqualTo("pay-1");
        verify(benefitOrderRepository, never()).selectById(any());
    }

    @Test
    void shouldPersistSuccessfulFirstDeductAndSyncOrder() {
        BenefitOrder order = new BenefitOrder();
        order.setBenefitOrderNo("ord-2");
        when(paymentRecordRepository.selectOne(any())).thenReturn(null);
        when(idempotencyService.isProcessed("req-2")).thenReturn(false);
        when(benefitOrderRepository.selectById("ord-2")).thenReturn(order);

        paymentService.handleFirstDeductCallback(request("req-2", "ord-2", "SUCCESS"));

        ArgumentCaptor<PaymentRecord> captor = ArgumentCaptor.forClass(PaymentRecord.class);
        verify(paymentRecordRepository).insert(captor.capture());
        verify(downstreamSyncService).syncOrder(order);
        verify(benefitOrderRepository).updateById(order);
        assertThat(captor.getValue().getPaymentStatus()).isEqualTo("SUCCESS");
        assertThat(order.getOrderStatus()).isEqualTo("FIRST_DEDUCT_SUCCESS");
    }

    @Test
    void shouldRejectCallbackWhenOrderMissing() {
        when(paymentRecordRepository.selectOne(any())).thenReturn(null);
        when(idempotencyService.isProcessed("req-3")).thenReturn(false);
        when(benefitOrderRepository.selectById("ord-3")).thenReturn(null);

        assertThatThrownBy(() -> paymentService.handleFallbackDeductCallback(request("req-3", "ord-3", "FAIL")))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("ORDER_NOT_FOUND");
    }

    private DeductionCallbackRequest request(String requestId, String benefitOrderNo, String status) {
        return new DeductionCallbackRequest(requestId, benefitOrderNo, "qw-" + requestId, status, 680000L, "reason", "2026-03-23T20:30:00");
    }
}
