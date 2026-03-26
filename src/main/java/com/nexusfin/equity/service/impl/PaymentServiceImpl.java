package com.nexusfin.equity.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nexusfin.equity.config.BusinessProperties;
import com.nexusfin.equity.dto.request.DeductionCallbackRequest;
import com.nexusfin.equity.dto.response.PaymentStatusResponse;
import com.nexusfin.equity.entity.BenefitOrder;
import com.nexusfin.equity.entity.PaymentRecord;
import com.nexusfin.equity.enums.PaymentStatusEnum;
import com.nexusfin.equity.enums.PaymentTypeEnum;
import com.nexusfin.equity.exception.BizException;
import com.nexusfin.equity.repository.BenefitOrderRepository;
import com.nexusfin.equity.repository.PaymentRecordRepository;
import com.nexusfin.equity.service.DownstreamSyncService;
import com.nexusfin.equity.service.IdempotencyService;
import com.nexusfin.equity.service.PaymentService;
import com.nexusfin.equity.util.OrderStateMachine;
import com.nexusfin.equity.util.RequestIdUtil;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentServiceImpl implements PaymentService {

    private final BenefitOrderRepository benefitOrderRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final DownstreamSyncService downstreamSyncService;
    private final IdempotencyService idempotencyService;
    private final BusinessProperties businessProperties;

    public PaymentServiceImpl(
            BenefitOrderRepository benefitOrderRepository,
            PaymentRecordRepository paymentRecordRepository,
            DownstreamSyncService downstreamSyncService,
            IdempotencyService idempotencyService,
            BusinessProperties businessProperties
    ) {
        this.benefitOrderRepository = benefitOrderRepository;
        this.paymentRecordRepository = paymentRecordRepository;
        this.downstreamSyncService = downstreamSyncService;
        this.idempotencyService = idempotencyService;
        this.businessProperties = businessProperties;
    }

    @Override
    @Transactional
    public PaymentStatusResponse handleFirstDeductCallback(DeductionCallbackRequest request) {
        return handleCallback(request, PaymentTypeEnum.FIRST_DEDUCT);
    }

    @Override
    @Transactional
    public PaymentStatusResponse handleFallbackDeductCallback(DeductionCallbackRequest request) {
        return handleCallback(request, PaymentTypeEnum.FALLBACK_DEDUCT);
    }

    private PaymentStatusResponse handleCallback(DeductionCallbackRequest request, PaymentTypeEnum paymentType) {
        // 先按 requestId 去重，解决支付渠道回调重试、延迟、乱序带来的重复入账问题。
        PaymentRecord existing = paymentRecordRepository.selectOne(Wrappers.<PaymentRecord>lambdaQuery()
                .eq(PaymentRecord::getRequestId, request.requestId())
                .last("limit 1"));
        if (existing != null || idempotencyService.isProcessed(request.requestId())) {
            PaymentRecord duplicated = existing != null ? existing : paymentRecordRepository.selectOne(Wrappers.<PaymentRecord>lambdaQuery()
                    .eq(PaymentRecord::getBenefitOrderNo, request.benefitOrderNo())
                    .eq(PaymentRecord::getPaymentType, paymentType.name())
                    .orderByDesc(PaymentRecord::getCreatedTs)
                    .last("limit 1"));
            if (duplicated == null) {
                throw new BizException("PAYMENT_NOT_FOUND", "Payment callback already processed but payment record missing");
            }
            return new PaymentStatusResponse(
                    duplicated.getPaymentNo(),
                    duplicated.getBenefitOrderNo(),
                    duplicated.getPaymentType(),
                    duplicated.getPaymentStatus(),
                    duplicated.getFailReason()
            );
        }

        BenefitOrder order = benefitOrderRepository.selectById(request.benefitOrderNo());
        if (order == null) {
            throw new BizException("ORDER_NOT_FOUND", "Benefit order not found");
        }
        boolean success = PaymentStatusEnum.SUCCESS.name().equalsIgnoreCase(request.deductStatus());
        // 每次支付结果都单独落一条支付记录，方便后续对账和问题排查。
        PaymentRecord paymentRecord = new PaymentRecord();
        paymentRecord.setPaymentNo(RequestIdUtil.nextId("pay"));
        paymentRecord.setBenefitOrderNo(request.benefitOrderNo());
        paymentRecord.setPaymentType(paymentType.name());
        paymentRecord.setProviderCode(businessProperties.getDefaultProviderCode());
        paymentRecord.setChannelTradeNo(request.qwTradeNo());
        paymentRecord.setAmount(request.deductAmount());
        paymentRecord.setPaymentStatus(success ? PaymentStatusEnum.SUCCESS.name() : PaymentStatusEnum.FAIL.name());
        paymentRecord.setFailReason(request.failReason());
        paymentRecord.setRequestId(request.requestId());
        paymentRecord.setCreatedTs(LocalDateTime.now());
        paymentRecord.setUpdatedTs(LocalDateTime.now());
        paymentRecordRepository.insert(paymentRecord);
        if (paymentType == PaymentTypeEnum.FIRST_DEDUCT) {
            OrderStateMachine.applyFirstDeductResult(order, success);
            // 首扣成功或失败后都要向下游同步，失败场景会让订单进入“待兜底”路径。
            downstreamSyncService.syncOrder(order);
        } else {
            OrderStateMachine.applyFallbackResult(order, success);
        }
        order.setUpdatedTs(LocalDateTime.now());
        benefitOrderRepository.updateById(order);
        idempotencyService.markProcessed(request.requestId(), paymentType.name(), paymentRecord.getPaymentNo(), paymentRecord.getPaymentStatus());
        return new PaymentStatusResponse(
                paymentRecord.getPaymentNo(),
                paymentRecord.getBenefitOrderNo(),
                paymentRecord.getPaymentType(),
                paymentRecord.getPaymentStatus(),
                paymentRecord.getFailReason()
        );
    }
}
