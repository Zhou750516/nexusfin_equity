package com.nexusfin.equity.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nexusfin.equity.config.BusinessProperties;
import com.nexusfin.equity.dto.request.GrantForwardCallbackRequest;
import com.nexusfin.equity.dto.response.PaymentStatusResponse;
import com.nexusfin.equity.entity.BenefitOrder;
import com.nexusfin.equity.entity.PaymentRecord;
import com.nexusfin.equity.enums.BenefitOrderStatusEnum;
import com.nexusfin.equity.enums.PaymentStatusEnum;
import com.nexusfin.equity.enums.PaymentTypeEnum;
import com.nexusfin.equity.repository.BenefitOrderRepository;
import com.nexusfin.equity.repository.PaymentRecordRepository;
import com.nexusfin.equity.service.FallbackDeductService;
import com.nexusfin.equity.util.OrderStateMachine;
import com.nexusfin.equity.util.RequestIdUtil;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FallbackDeductServiceImpl implements FallbackDeductService {

    private final PaymentRecordRepository paymentRecordRepository;
    private final BenefitOrderRepository benefitOrderRepository;
    private final BusinessProperties businessProperties;

    public FallbackDeductServiceImpl(
            PaymentRecordRepository paymentRecordRepository,
            BenefitOrderRepository benefitOrderRepository,
            BusinessProperties businessProperties
    ) {
        this.paymentRecordRepository = paymentRecordRepository;
        this.benefitOrderRepository = benefitOrderRepository;
        this.businessProperties = businessProperties;
    }

    @Override
    @Transactional
    public PaymentStatusResponse triggerFallback(BenefitOrder order, GrantForwardCallbackRequest request) {
        // 兜底代扣只能被 grant success 事件推动一次，所以先查既有记录，避免重复触发。
        PaymentRecord existing = paymentRecordRepository.selectOne(Wrappers.<PaymentRecord>lambdaQuery()
                .eq(PaymentRecord::getBenefitOrderNo, order.getBenefitOrderNo())
                .eq(PaymentRecord::getPaymentType, PaymentTypeEnum.FALLBACK_DEDUCT.name())
                .last("limit 1"));
        if (existing != null) {
            return new PaymentStatusResponse(
                    existing.getPaymentNo(),
                    existing.getBenefitOrderNo(),
                    existing.getPaymentType(),
                    existing.getPaymentStatus(),
                    existing.getFailReason()
            );
        }
        OrderStateMachine.ensureCanTriggerFallback(order);
        // 基线阶段先把兜底代扣请求记录为待处理，真实出款渠道联调时再把“发起渠道调用”接入到这里。
        PaymentRecord paymentRecord = new PaymentRecord();
        paymentRecord.setPaymentNo(RequestIdUtil.nextId("pay"));
        paymentRecord.setBenefitOrderNo(order.getBenefitOrderNo());
        paymentRecord.setPaymentType(PaymentTypeEnum.FALLBACK_DEDUCT.name());
        paymentRecord.setProviderCode(businessProperties.getDefaultProviderCode());
        paymentRecord.setAmount(request.actualAmount());
        paymentRecord.setPaymentStatus(PaymentStatusEnum.PENDING.name());
        paymentRecord.setRequestId(request.requestId());
        paymentRecord.setCreatedTs(LocalDateTime.now());
        paymentRecord.setUpdatedTs(LocalDateTime.now());
        paymentRecordRepository.insert(paymentRecord);
        order.setOrderStatus(BenefitOrderStatusEnum.FALLBACK_DEDUCT_PENDING.name());
        order.setFallbackDeductStatus(PaymentStatusEnum.PENDING.name());
        order.setUpdatedTs(LocalDateTime.now());
        benefitOrderRepository.updateById(order);
        return new PaymentStatusResponse(
                paymentRecord.getPaymentNo(),
                paymentRecord.getBenefitOrderNo(),
                paymentRecord.getPaymentType(),
                paymentRecord.getPaymentStatus(),
                paymentRecord.getFailReason()
        );
    }
}
