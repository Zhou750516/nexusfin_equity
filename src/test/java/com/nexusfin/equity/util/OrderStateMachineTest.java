package com.nexusfin.equity.util;

import com.nexusfin.equity.entity.BenefitOrder;
import com.nexusfin.equity.exception.BizException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderStateMachineTest {

    @Test
    void shouldRejectOrderCreationWhenAgreementNotReady() {
        assertThatThrownBy(() -> OrderStateMachine.ensureCanCreateOrder(false))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("AGREEMENT_REQUIRED");
    }

    @Test
    void shouldApplyFirstDeductSuccessState() {
        BenefitOrder order = new BenefitOrder();

        OrderStateMachine.applyFirstDeductResult(order, true);

        assertThat(order.getOrderStatus()).isEqualTo("FIRST_DEDUCT_SUCCESS");
        assertThat(order.getQwFirstDeductStatus()).isEqualTo("SUCCESS");
        assertThat(order.getSyncStatus()).isEqualTo("SYNC_SUCCESS");
    }

    @Test
    void shouldRejectFallbackWhenOrderNotInFirstDeductFail() {
        BenefitOrder order = new BenefitOrder();
        order.setOrderStatus("FIRST_DEDUCT_PENDING");
        order.setQwFallbackDeductStatus("NONE");

        assertThatThrownBy(() -> OrderStateMachine.ensureCanTriggerFallback(order))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("ILLEGAL_STATE");
    }

    @Test
    void shouldApplyGrantExerciseAndRefundStates() {
        BenefitOrder order = new BenefitOrder();
        order.setOrderStatus("FIRST_DEDUCT_SUCCESS");

        OrderStateMachine.applyGrantResult(order, true, "loan-1");
        assertThat(order.getOrderStatus()).isEqualTo("EXERCISE_PENDING");
        assertThat(order.getGrantStatus()).isEqualTo("SUCCESS");
        assertThat(order.getLoanOrderNo()).isEqualTo("loan-1");

        OrderStateMachine.applyExerciseResult(order, true);
        assertThat(order.getOrderStatus()).isEqualTo("EXERCISE_SUCCESS");
        assertThat(order.getQwExerciseStatus()).isEqualTo("SUCCESS");

        OrderStateMachine.applyRefundResult(order, false);
        assertThat(order.getOrderStatus()).isEqualTo("REFUND_FAIL");
        assertThat(order.getRefundStatus()).isEqualTo("FAIL");
    }
}
