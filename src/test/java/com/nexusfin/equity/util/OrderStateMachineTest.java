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

        boolean transitioned = OrderStateMachine.applyFirstDeductResult(order, true);

        assertThat(transitioned).isTrue();
        assertThat(order.getOrderStatus()).isEqualTo("FIRST_DEDUCT_SUCCESS");
        assertThat(order.getFirstDeductStatus()).isEqualTo("SUCCESS");
        assertThat(order.getSyncStatus()).isEqualTo("SYNC_SUCCESS");
    }

    @Test
    void shouldKeepFirstDeductSuccessTerminalWhenLaterFailureArrives() {
        BenefitOrder order = new BenefitOrder();
        order.setOrderStatus("FIRST_DEDUCT_SUCCESS");
        order.setFirstDeductStatus("SUCCESS");
        order.setSyncStatus("SYNC_SUCCESS");

        boolean transitioned = OrderStateMachine.applyFirstDeductResult(order, false);

        assertThat(transitioned).isFalse();
        assertThat(order.getOrderStatus()).isEqualTo("FIRST_DEDUCT_SUCCESS");
        assertThat(order.getFirstDeductStatus()).isEqualTo("SUCCESS");
        assertThat(order.getSyncStatus()).isEqualTo("SYNC_SUCCESS");
    }

    @Test
    void shouldRejectFallbackWhenOrderNotInFirstDeductFail() {
        BenefitOrder order = new BenefitOrder();
        order.setOrderStatus("FIRST_DEDUCT_PENDING");
        order.setFallbackDeductStatus("NONE");

        assertThatThrownBy(() -> OrderStateMachine.ensureCanTriggerFallback(order))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("ILLEGAL_STATE");
    }

    @Test
    void shouldApplyGrantExerciseAndRefundStates() {
        BenefitOrder order = new BenefitOrder();
        order.setOrderStatus("FIRST_DEDUCT_SUCCESS");

        OrderStateMachine.applyGrantResult(order, true, "20260501");
        assertThat(order.getOrderStatus()).isEqualTo("EXERCISE_PENDING");
        assertThat(order.getGrantStatus()).isEqualTo("SUCCESS");
        assertThat(order.getLoanOrderNo()).isEqualTo("20260501");

        OrderStateMachine.applyExerciseResult(order, true);
        assertThat(order.getOrderStatus()).isEqualTo("EXERCISE_SUCCESS");
        assertThat(order.getExerciseStatus()).isEqualTo("SUCCESS");

        OrderStateMachine.applyRefundResult(order, false);
        assertThat(order.getOrderStatus()).isEqualTo("REFUND_FAIL");
        assertThat(order.getRefundStatus()).isEqualTo("FAIL");
    }
}
