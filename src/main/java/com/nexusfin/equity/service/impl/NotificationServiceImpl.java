package com.nexusfin.equity.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nexusfin.equity.dto.request.ExerciseCallbackRequest;
import com.nexusfin.equity.dto.request.GrantForwardCallbackRequest;
import com.nexusfin.equity.dto.request.RepaymentForwardCallbackRequest;
import com.nexusfin.equity.dto.request.RefundCallbackRequest;
import com.nexusfin.equity.entity.BenefitOrder;
import com.nexusfin.equity.entity.NotificationReceiveLog;
import com.nexusfin.equity.enums.BenefitOrderStatusEnum;
import com.nexusfin.equity.enums.NotificationProcessStatusEnum;
import com.nexusfin.equity.enums.NotificationTypeEnum;
import com.nexusfin.equity.repository.BenefitOrderRepository;
import com.nexusfin.equity.repository.NotificationReceiveLogRepository;
import com.nexusfin.equity.service.FallbackDeductService;
import com.nexusfin.equity.service.IdempotencyService;
import com.nexusfin.equity.service.NotificationService;
import com.nexusfin.equity.util.OrderStateMachine;
import com.nexusfin.equity.util.RequestIdUtil;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationServiceImpl implements NotificationService {

    private final BenefitOrderRepository benefitOrderRepository;
    private final NotificationReceiveLogRepository notificationReceiveLogRepository;
    private final FallbackDeductService fallbackDeductService;
    private final IdempotencyService idempotencyService;

    public NotificationServiceImpl(
            BenefitOrderRepository benefitOrderRepository,
            NotificationReceiveLogRepository notificationReceiveLogRepository,
            FallbackDeductService fallbackDeductService,
            IdempotencyService idempotencyService
    ) {
        this.benefitOrderRepository = benefitOrderRepository;
        this.notificationReceiveLogRepository = notificationReceiveLogRepository;
        this.fallbackDeductService = fallbackDeductService;
        this.idempotencyService = idempotencyService;
    }

    @Override
    @Transactional
    public void handleGrant(GrantForwardCallbackRequest request) {
        if (idempotencyService.isProcessed(request.requestId())) {
            return;
        }
        // 先记通知日志，再更新订单状态，这样即使中途失败也能知道通知曾经到达过。
        BenefitOrder order = benefitOrderRepository.selectById(request.benefitOrderNo());
        logNotification(request.requestId(), request.benefitOrderNo(), NotificationTypeEnum.GRANT_RESULT, request.toString());
        if (order == null) {
            return;
        }
        boolean success = "SUCCESS".equalsIgnoreCase(request.grantStatus());
        OrderStateMachine.applyGrantResult(order, success, request.loanOrderNo());
        if (success && BenefitOrderStatusEnum.FIRST_DEDUCT_FAIL.name().equals(order.getOrderStatus())) {
            // 只有“首扣失败且已放款成功”的订单，才进入自动兜底代扣。
            fallbackDeductService.triggerFallback(order, request);
        }
        order.setUpdatedTs(LocalDateTime.now());
        benefitOrderRepository.updateById(order);
        idempotencyService.markProcessed(request.requestId(), "GRANT", request.benefitOrderNo(), request.grantStatus());
    }

    @Override
    @Transactional
    public void handleRepayment(RepaymentForwardCallbackRequest request) {
        if (idempotencyService.isProcessed(request.requestId())) {
            return;
        }
        logNotification(request.requestId(), request.benefitOrderNo(), NotificationTypeEnum.REPAYMENT_STATUS, request.toString());
        idempotencyService.markProcessed(request.requestId(), "REPAYMENT", request.benefitOrderNo(), request.repaymentStatus());
    }

    @Override
    @Transactional
    public void handleExercise(ExerciseCallbackRequest request) {
        if (idempotencyService.isProcessed(request.requestId())) {
            return;
        }
        BenefitOrder order = benefitOrderRepository.selectById(request.benefitOrderNo());
        logNotification(request.requestId(), request.benefitOrderNo(), NotificationTypeEnum.EXERCISE_RESULT, request.toString());
        if (order != null) {
            OrderStateMachine.applyExerciseResult(order, "SUCCESS".equalsIgnoreCase(request.exerciseStatus()));
            order.setUpdatedTs(LocalDateTime.now());
            benefitOrderRepository.updateById(order);
        }
        idempotencyService.markProcessed(request.requestId(), "EXERCISE", request.benefitOrderNo(), request.exerciseStatus());
    }

    @Override
    @Transactional
    public void handleRefund(RefundCallbackRequest request) {
        if (idempotencyService.isProcessed(request.requestId())) {
            return;
        }
        BenefitOrder order = benefitOrderRepository.selectById(request.benefitOrderNo());
        logNotification(request.requestId(), request.benefitOrderNo(), NotificationTypeEnum.REFUND_RESULT, request.toString());
        if (order != null) {
            OrderStateMachine.applyRefundResult(order, "SUCCESS".equalsIgnoreCase(request.refundStatus()));
            order.setUpdatedTs(LocalDateTime.now());
            benefitOrderRepository.updateById(order);
        }
        idempotencyService.markProcessed(request.requestId(), "REFUND", request.benefitOrderNo(), request.refundStatus());
    }

    private void logNotification(String requestId, String benefitOrderNo, NotificationTypeEnum type, String payload) {
        NotificationReceiveLog existing = notificationReceiveLogRepository.selectOne(Wrappers.<NotificationReceiveLog>lambdaQuery()
                .eq(NotificationReceiveLog::getRequestId, requestId)
                .last("limit 1"));
        if (existing != null) {
            return;
        }
        // 回调日志是对账和人工排障的基础数据，统一记录通知类型、请求号、原始载荷和处理时间。
        NotificationReceiveLog notificationReceiveLog = new NotificationReceiveLog();
        notificationReceiveLog.setNotifyNo(RequestIdUtil.nextId("ntf"));
        notificationReceiveLog.setBenefitOrderNo(benefitOrderNo);
        notificationReceiveLog.setNotifyType(type.name());
        notificationReceiveLog.setRequestId(requestId);
        notificationReceiveLog.setProcessStatus(NotificationProcessStatusEnum.PROCESSED.name());
        notificationReceiveLog.setPayload(payload);
        notificationReceiveLog.setRetryCount(0);
        notificationReceiveLog.setReceivedTs(LocalDateTime.now());
        notificationReceiveLog.setProcessedTs(LocalDateTime.now());
        notificationReceiveLogRepository.insert(notificationReceiveLog);
    }
}
