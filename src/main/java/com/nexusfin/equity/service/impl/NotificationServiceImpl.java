package com.nexusfin.equity.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nexusfin.equity.dto.request.ExerciseCallbackRequest;
import com.nexusfin.equity.dto.request.LoanResultCallbackRequest;
import com.nexusfin.equity.dto.request.RepaymentResultCallbackRequest;
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
import com.nexusfin.equity.thirdparty.qw.QwBenefitClient;
import com.nexusfin.equity.thirdparty.qw.QwLendingNotifyRequest;
import com.nexusfin.equity.util.OrderStateMachine;
import com.nexusfin.equity.util.RequestIdUtil;
import com.nexusfin.equity.util.TraceIdUtil;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final BenefitOrderRepository benefitOrderRepository;
    private final NotificationReceiveLogRepository notificationReceiveLogRepository;
    private final FallbackDeductService fallbackDeductService;
    private final IdempotencyService idempotencyService;
    private final QwBenefitClient qwBenefitClient;

    public NotificationServiceImpl(
            BenefitOrderRepository benefitOrderRepository,
            NotificationReceiveLogRepository notificationReceiveLogRepository,
            FallbackDeductService fallbackDeductService,
            IdempotencyService idempotencyService,
            QwBenefitClient qwBenefitClient
    ) {
        this.benefitOrderRepository = benefitOrderRepository;
        this.notificationReceiveLogRepository = notificationReceiveLogRepository;
        this.fallbackDeductService = fallbackDeductService;
        this.idempotencyService = idempotencyService;
        this.qwBenefitClient = qwBenefitClient;
    }

    @Override
    @Transactional
    public void handleGrant(LoanResultCallbackRequest request) {
        String bizOrderNo = request.bizOrderNo();
        if (idempotencyService.isProcessed(request.requestId())) {
            log.info("traceId={} bizOrderNo={} requestId={} loan result callback duplicated, ignored",
                    TraceIdUtil.getTraceId(), bizOrderNo, request.requestId());
            return;
        }
        log.info("traceId={} bizOrderNo={} requestId={} loan result callback processing status={}",
                TraceIdUtil.getTraceId(), bizOrderNo, request.requestId(), request.idempotencyStatus());
        // 先记收到通知的事实，再根据实际处理结果更新状态。
        NotificationReceiveLog notificationLog = logNotificationReceived(
                request.requestId(),
                bizOrderNo,
                NotificationTypeEnum.GRANT_RESULT,
                request.toString()
        );
        BenefitOrder order = findBenefitOrder(request.resolvedBenefitOrderNo(), request.loanOrderNo());
        if (order == null) {
            log.warn("traceId={} bizOrderNo={} requestId={} loan result callback order missing",
                    TraceIdUtil.getTraceId(), bizOrderNo, request.requestId());
            markNotification(notificationLog, NotificationProcessStatusEnum.FAILED);
            return;
        }
        try {
            if (request.isSuccess()) {
                OrderStateMachine.applyGrantResult(order, true, request.loanOrderNo());
            } else if (request.isProcessing()) {
                order.setGrantStatus(request.normalizedGrantStatus());
                order.setLoanOrderNo(request.loanOrderNo());
            } else {
                OrderStateMachine.applyGrantResult(order, false, request.loanOrderNo());
            }
            if (request.isSuccess() && BenefitOrderStatusEnum.FIRST_DEDUCT_FAIL.name().equals(order.getOrderStatus())) {
                // 只有“首扣失败且已放款成功”的订单，才进入自动兜底代扣。
                fallbackDeductService.triggerFallback(order, request);
            }
            order.setUpdatedTs(LocalDateTime.now());
            benefitOrderRepository.updateById(order);
            if (!request.isProcessing()) {
                qwBenefitClient.notifyLending(new QwLendingNotifyRequest(
                        order.getExternalUserId(),
                        order.getBenefitOrderNo(),
                        request.isSuccess() ? 1 : 0
                ));
            }
            markNotification(notificationLog, NotificationProcessStatusEnum.PROCESSED);
            idempotencyService.markProcessed(request.requestId(), "GRANT", order.getBenefitOrderNo(), request.idempotencyStatus());
            log.info("traceId={} bizOrderNo={} requestId={} loan result callback processed localStatus={}",
                    TraceIdUtil.getTraceId(), order.getBenefitOrderNo(), request.requestId(), order.getGrantStatus());
        } catch (RuntimeException ex) {
            log.error("traceId={} bizOrderNo={} requestId={} loan result callback failed errorMsg={}",
                    TraceIdUtil.getTraceId(), bizOrderNo, request.requestId(), ex.getMessage());
            markNotification(notificationLog, NotificationProcessStatusEnum.FAILED);
            throw ex;
        }
    }

    @Override
    @Transactional
    public void handleRepayment(RepaymentResultCallbackRequest request) {
        String bizOrderNo = request.bizOrderNo();
        if (idempotencyService.isProcessed(request.requestId())) {
            log.info("traceId={} bizOrderNo={} requestId={} repayment result callback duplicated, ignored",
                    TraceIdUtil.getTraceId(), bizOrderNo, request.requestId());
            return;
        }
        log.info("traceId={} bizOrderNo={} requestId={} repayment result callback processing status={}",
                TraceIdUtil.getTraceId(), bizOrderNo, request.requestId(), request.idempotencyStatus());
        NotificationReceiveLog notificationLog = logNotificationReceived(
                request.requestId(),
                bizOrderNo,
                NotificationTypeEnum.REPAYMENT_STATUS,
                request.toString()
        );
        BenefitOrder order = findBenefitOrder(request.resolvedBenefitOrderNo(), request.loanOrderNo());
        if (order == null) {
            log.warn("traceId={} bizOrderNo={} requestId={} repayment result callback order missing",
                    TraceIdUtil.getTraceId(), bizOrderNo, request.requestId());
            markNotification(notificationLog, NotificationProcessStatusEnum.FAILED);
            return;
        }
        if (request.loanOrderNo() != null && !request.loanOrderNo().isBlank()) {
            order.setLoanOrderNo(request.loanOrderNo());
            order.setUpdatedTs(LocalDateTime.now());
            benefitOrderRepository.updateById(order);
        }
        markNotification(notificationLog, NotificationProcessStatusEnum.PROCESSED);
        idempotencyService.markProcessed(request.requestId(), "REPAYMENT", order.getBenefitOrderNo(), request.idempotencyStatus());
        log.info("traceId={} bizOrderNo={} requestId={} repayment result callback processed swiftNumber={}",
                TraceIdUtil.getTraceId(), order.getBenefitOrderNo(), request.requestId(), request.swiftNumber());
    }

    @Override
    @Transactional
    public void handleExercise(ExerciseCallbackRequest request) {
        if (idempotencyService.isProcessed(request.requestId())) {
            return;
        }
        NotificationReceiveLog notificationLog = logNotificationReceived(
                request.requestId(),
                request.benefitOrderNo(),
                NotificationTypeEnum.EXERCISE_RESULT,
                request.toString()
        );
        BenefitOrder order = benefitOrderRepository.selectById(request.benefitOrderNo());
        if (order == null) {
            markNotification(notificationLog, NotificationProcessStatusEnum.FAILED);
            return;
        }
        try {
            OrderStateMachine.applyExerciseResult(order, "SUCCESS".equalsIgnoreCase(request.exerciseStatus()));
            order.setUpdatedTs(LocalDateTime.now());
            benefitOrderRepository.updateById(order);
            markNotification(notificationLog, NotificationProcessStatusEnum.PROCESSED);
            idempotencyService.markProcessed(request.requestId(), "EXERCISE", request.benefitOrderNo(), request.exerciseStatus());
        } catch (RuntimeException ex) {
            markNotification(notificationLog, NotificationProcessStatusEnum.FAILED);
            throw ex;
        }
    }

    @Override
    @Transactional
    public void handleRefund(RefundCallbackRequest request) {
        if (idempotencyService.isProcessed(request.requestId())) {
            return;
        }
        NotificationReceiveLog notificationLog = logNotificationReceived(
                request.requestId(),
                request.benefitOrderNo(),
                NotificationTypeEnum.REFUND_RESULT,
                request.toString()
        );
        BenefitOrder order = benefitOrderRepository.selectById(request.benefitOrderNo());
        if (order == null) {
            markNotification(notificationLog, NotificationProcessStatusEnum.FAILED);
            return;
        }
        try {
            OrderStateMachine.applyRefundResult(order, "SUCCESS".equalsIgnoreCase(request.refundStatus()));
            order.setUpdatedTs(LocalDateTime.now());
            benefitOrderRepository.updateById(order);
            markNotification(notificationLog, NotificationProcessStatusEnum.PROCESSED);
            idempotencyService.markProcessed(request.requestId(), "REFUND", request.benefitOrderNo(), request.refundStatus());
        } catch (RuntimeException ex) {
            markNotification(notificationLog, NotificationProcessStatusEnum.FAILED);
            throw ex;
        }
    }

    private NotificationReceiveLog logNotificationReceived(
            String requestId,
            String benefitOrderNo,
            NotificationTypeEnum type,
            String payload
    ) {
        NotificationReceiveLog existing = notificationReceiveLogRepository.selectOne(Wrappers.<NotificationReceiveLog>lambdaQuery()
                .eq(NotificationReceiveLog::getRequestId, requestId)
                .last("limit 1"));
        if (existing != null) {
            return existing;
        }
        // 回调日志是对账和人工排障的基础数据，统一记录通知类型、请求号、原始载荷和处理时间。
        NotificationReceiveLog notificationReceiveLog = new NotificationReceiveLog();
        notificationReceiveLog.setNotifyNo(RequestIdUtil.nextId("ntf"));
        notificationReceiveLog.setBenefitOrderNo(benefitOrderNo);
        notificationReceiveLog.setNotifyType(type.name());
        notificationReceiveLog.setRequestId(requestId);
        notificationReceiveLog.setProcessStatus(NotificationProcessStatusEnum.RECEIVED.name());
        notificationReceiveLog.setPayload(payload);
        notificationReceiveLog.setRetryCount(0);
        notificationReceiveLog.setReceivedTs(LocalDateTime.now());
        notificationReceiveLogRepository.insert(notificationReceiveLog);
        return notificationReceiveLog;
    }

    private BenefitOrder findBenefitOrder(String benefitOrderNo, String loanOrderNo) {
        if (benefitOrderNo != null && !benefitOrderNo.isBlank()) {
            BenefitOrder order = benefitOrderRepository.selectById(benefitOrderNo);
            if (order != null) {
                return order;
            }
        }
        if (loanOrderNo == null || loanOrderNo.isBlank()) {
            return null;
        }
        return benefitOrderRepository.selectOne(Wrappers.<BenefitOrder>lambdaQuery()
                .eq(BenefitOrder::getLoanOrderNo, loanOrderNo)
                .last("limit 1"));
    }

    private void markNotification(NotificationReceiveLog notificationReceiveLog, NotificationProcessStatusEnum processStatus) {
        notificationReceiveLog.setProcessStatus(processStatus.name());
        notificationReceiveLog.setProcessedTs(LocalDateTime.now());
        notificationReceiveLogRepository.updateById(notificationReceiveLog);
    }
}
