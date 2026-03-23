package com.nexusfin.equity.service;

import com.nexusfin.equity.entity.BenefitOrder;
import com.nexusfin.equity.entity.NotificationReceiveLog;
import com.nexusfin.equity.entity.PaymentRecord;
import com.nexusfin.equity.repository.BenefitOrderRepository;
import com.nexusfin.equity.repository.NotificationReceiveLogRepository;
import com.nexusfin.equity.repository.PaymentRecordRepository;
import com.nexusfin.equity.service.impl.ReconciliationServiceImpl;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReconciliationServiceTest {

    @Mock
    private BenefitOrderRepository benefitOrderRepository;

    @Mock
    private PaymentRecordRepository paymentRecordRepository;

    @Mock
    private NotificationReceiveLogRepository notificationReceiveLogRepository;

    @InjectMocks
    private ReconciliationServiceImpl reconciliationService;

    @Test
    void shouldQueryAcrossCoreIdentifiers() {
        BenefitOrder order = new BenefitOrder();
        order.setBenefitOrderNo("ord-rec-1");
        order.setMemberId("mem-rec-1");
        PaymentRecord paymentRecord = new PaymentRecord();
        paymentRecord.setPaymentNo("pay-rec-1");
        NotificationReceiveLog notification = new NotificationReceiveLog();
        notification.setRequestId("req-rec-1");

        when(benefitOrderRepository.selectById("ord-rec-1")).thenReturn(order);
        when(benefitOrderRepository.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(order));
        when(paymentRecordRepository.selectById("pay-rec-1")).thenReturn(paymentRecord);
        when(notificationReceiveLogRepository.selectByBenefitOrderNo("ord-rec-1")).thenReturn(List.of(notification));
        when(notificationReceiveLogRepository.selectByRequestId("req-rec-1")).thenReturn(List.of(notification));

        assertThat(reconciliationService.queryOrderByBenefitOrderNo("ord-rec-1")).isSameAs(order);
        assertThat(reconciliationService.queryOrdersByMemberId("mem-rec-1")).containsExactly(order);
        assertThat(reconciliationService.queryPaymentByPaymentNo("pay-rec-1")).isSameAs(paymentRecord);
        assertThat(reconciliationService.queryByBenefitOrderNo("ord-rec-1")).containsExactly(notification);
        assertThat(reconciliationService.queryByRequestId("req-rec-1")).containsExactly(notification);

        verify(notificationReceiveLogRepository).selectByBenefitOrderNo("ord-rec-1");
        verify(notificationReceiveLogRepository).selectByRequestId("req-rec-1");
    }
}
