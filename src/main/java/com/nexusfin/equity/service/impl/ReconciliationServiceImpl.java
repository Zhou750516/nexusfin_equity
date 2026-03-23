package com.nexusfin.equity.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nexusfin.equity.entity.BenefitOrder;
import com.nexusfin.equity.entity.NotificationReceiveLog;
import com.nexusfin.equity.entity.PaymentRecord;
import com.nexusfin.equity.repository.BenefitOrderRepository;
import com.nexusfin.equity.repository.NotificationReceiveLogRepository;
import com.nexusfin.equity.repository.PaymentRecordRepository;
import com.nexusfin.equity.service.ReconciliationService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ReconciliationServiceImpl implements ReconciliationService {

    private final BenefitOrderRepository benefitOrderRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final NotificationReceiveLogRepository notificationReceiveLogRepository;

    public ReconciliationServiceImpl(
            BenefitOrderRepository benefitOrderRepository,
            PaymentRecordRepository paymentRecordRepository,
            NotificationReceiveLogRepository notificationReceiveLogRepository
    ) {
        this.benefitOrderRepository = benefitOrderRepository;
        this.paymentRecordRepository = paymentRecordRepository;
        this.notificationReceiveLogRepository = notificationReceiveLogRepository;
    }

    @Override
    public BenefitOrder queryOrderByBenefitOrderNo(String benefitOrderNo) {
        return benefitOrderRepository.selectById(benefitOrderNo);
    }

    @Override
    public List<BenefitOrder> queryOrdersByMemberId(String memberId) {
        return benefitOrderRepository.selectList(Wrappers.<BenefitOrder>lambdaQuery()
                .eq(BenefitOrder::getMemberId, memberId)
                .orderByDesc(BenefitOrder::getCreatedTs));
    }

    @Override
    public PaymentRecord queryPaymentByPaymentNo(String paymentNo) {
        return paymentRecordRepository.selectById(paymentNo);
    }

    @Override
    public List<NotificationReceiveLog> queryByBenefitOrderNo(String benefitOrderNo) {
        return notificationReceiveLogRepository.selectByBenefitOrderNo(benefitOrderNo);
    }

    @Override
    public List<NotificationReceiveLog> queryByRequestId(String requestId) {
        return notificationReceiveLogRepository.selectByRequestId(requestId);
    }
}
