package com.nexusfin.equity.util;

import com.nexusfin.equity.entity.BenefitOrder;
import com.nexusfin.equity.enums.BenefitOrderStatusEnum;
import com.nexusfin.equity.enums.PaymentStatusEnum;
import com.nexusfin.equity.exception.BizException;

public final class OrderStateMachine {

    private OrderStateMachine() {
    }

    public static void ensureCanCreateOrder(boolean agreementsReady) {
        // 业务要求先完成协议签署再进入支付阶段，这里统一做前置兜底校验。
        if (!agreementsReady) {
            throw new BizException("AGREEMENT_REQUIRED", "Agreements must be signed before order creation");
        }
    }

    public static void applyFirstDeductResult(BenefitOrder order, boolean success) {
        // 首扣结果直接决定订单走直连路径还是兜底路径，因此需要一次性更新订单与支付视图状态。
        if (success) {
            order.setOrderStatus(BenefitOrderStatusEnum.FIRST_DEDUCT_SUCCESS.name());
            order.setQwFirstDeductStatus(PaymentStatusEnum.SUCCESS.name());
            order.setSyncStatus(BenefitOrderStatusEnum.SYNC_SUCCESS.name());
        } else {
            order.setOrderStatus(BenefitOrderStatusEnum.FIRST_DEDUCT_FAIL.name());
            order.setQwFirstDeductStatus(PaymentStatusEnum.FAIL.name());
            order.setSyncStatus(BenefitOrderStatusEnum.SYNC_PENDING.name());
        }
    }

    public static void ensureCanTriggerFallback(BenefitOrder order) {
        // 只有“首扣失败”的订单才允许触发兜底代扣，且同一笔订单只允许发起一次有效兜底流程。
        if (!BenefitOrderStatusEnum.FIRST_DEDUCT_FAIL.name().equals(order.getOrderStatus())) {
            throw new BizException("ILLEGAL_STATE", "Fallback deduct requires a first deduct failure");
        }
        if (PaymentStatusEnum.PENDING.name().equals(order.getQwFallbackDeductStatus())
                || PaymentStatusEnum.SUCCESS.name().equals(order.getQwFallbackDeductStatus())) {
            throw new BizException("DUPLICATE_FALLBACK", "Fallback deduct already triggered");
        }
    }

    public static void applyFallbackResult(BenefitOrder order, boolean success) {
        if (success) {
            order.setOrderStatus(BenefitOrderStatusEnum.FALLBACK_DEDUCT_SUCCESS.name());
            order.setQwFallbackDeductStatus(PaymentStatusEnum.SUCCESS.name());
        } else {
            order.setOrderStatus(BenefitOrderStatusEnum.FALLBACK_DEDUCT_FAIL.name());
            order.setQwFallbackDeductStatus(PaymentStatusEnum.FAIL.name());
        }
    }

    public static void applyGrantResult(BenefitOrder order, boolean success, String loanOrderNo) {
        // 放款通知是 ABS 侧感知下游结果的关键节点，同时补齐 loanOrderNo 方便后续对账和客服查询。
        order.setGrantStatus(success ? "SUCCESS" : "FAIL");
        order.setLoanOrderNo(loanOrderNo);
        if (success && BenefitOrderStatusEnum.FIRST_DEDUCT_SUCCESS.name().equals(order.getOrderStatus())) {
            order.setOrderStatus(BenefitOrderStatusEnum.EXERCISE_PENDING.name());
        }
    }

    public static void applyExerciseResult(BenefitOrder order, boolean success) {
        order.setQwExerciseStatus(success ? "SUCCESS" : "FAIL");
        order.setOrderStatus(success
                ? BenefitOrderStatusEnum.EXERCISE_SUCCESS.name()
                : BenefitOrderStatusEnum.EXERCISE_FAIL.name());
    }

    public static void applyRefundResult(BenefitOrder order, boolean success) {
        order.setRefundStatus(success ? "SUCCESS" : "FAIL");
        order.setOrderStatus(success
                ? BenefitOrderStatusEnum.REFUND_SUCCESS.name()
                : BenefitOrderStatusEnum.REFUND_FAIL.name());
    }
}
