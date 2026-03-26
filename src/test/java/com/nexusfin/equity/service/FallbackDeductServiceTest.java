package com.nexusfin.equity.service;

import com.nexusfin.equity.config.BusinessProperties;
import com.nexusfin.equity.dto.request.GrantForwardCallbackRequest;
import com.nexusfin.equity.dto.response.PaymentStatusResponse;
import com.nexusfin.equity.entity.BenefitOrder;
import com.nexusfin.equity.entity.PaymentRecord;
import com.nexusfin.equity.repository.BenefitOrderRepository;
import com.nexusfin.equity.repository.PaymentRecordRepository;
import com.nexusfin.equity.service.impl.FallbackDeductServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FallbackDeductServiceTest {

    @Mock
    private PaymentRecordRepository paymentRecordRepository;

    @Mock
    private BenefitOrderRepository benefitOrderRepository;

    private FallbackDeductServiceImpl fallbackDeductService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        fallbackDeductService = new FallbackDeductServiceImpl(
                paymentRecordRepository,
                benefitOrderRepository,
                new BusinessProperties()
        );
    }

    @Test
    void shouldReturnExistingFallbackRecordWhenAlreadyTriggered() {
        PaymentRecord existing = new PaymentRecord();
        existing.setPaymentNo("pay-1");
        existing.setBenefitOrderNo("ord-1");
        existing.setPaymentType("FALLBACK_DEDUCT");
        existing.setPaymentStatus("PENDING");
        when(paymentRecordRepository.selectOne(any())).thenReturn(existing);

        PaymentStatusResponse response = fallbackDeductService.triggerFallback(order("ord-1"), request("req-1"));

        assertThat(response.paymentNo()).isEqualTo("pay-1");
    }

    @Test
    void shouldCreatePendingFallbackPaymentWhenEligible() {
        when(paymentRecordRepository.selectOne(any())).thenReturn(null);
        BenefitOrder order = order("ord-2");

        fallbackDeductService.triggerFallback(order, request("req-2"));

        ArgumentCaptor<PaymentRecord> captor = ArgumentCaptor.forClass(PaymentRecord.class);
        verify(paymentRecordRepository).insert(captor.capture());
        verify(benefitOrderRepository).updateById(order);
        assertThat(captor.getValue().getPaymentType()).isEqualTo("FALLBACK_DEDUCT");
        assertThat(order.getOrderStatus()).isEqualTo("FALLBACK_DEDUCT_PENDING");
    }

    private BenefitOrder order(String benefitOrderNo) {
        BenefitOrder order = new BenefitOrder();
        order.setBenefitOrderNo(benefitOrderNo);
        order.setOrderStatus("FIRST_DEDUCT_FAIL");
        order.setFallbackDeductStatus("NONE");
        return order;
    }

    private GrantForwardCallbackRequest request(String requestId) {
        return new GrantForwardCallbackRequest(requestId, "ord", "SUCCESS", 680000L, "loan-1", null, "2026-03-23T20:31:00", 1711197060L);
    }
}
