package com.nexusfin.equity.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusfin.equity.entity.AsyncCompensationTask;
import com.nexusfin.equity.entity.BenefitOrder;
import com.nexusfin.equity.enums.BenefitOrderStatusEnum;
import com.nexusfin.equity.exception.BizException;
import com.nexusfin.equity.repository.BenefitOrderRepository;
import com.nexusfin.equity.service.impl.QwBenefitPurchaseCompensationExecutor;
import com.nexusfin.equity.thirdparty.qw.QwBenefitClient;
import com.nexusfin.equity.thirdparty.qw.QwMemberSyncRequest;
import com.nexusfin.equity.thirdparty.qw.QwMemberSyncResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QwBenefitPurchaseCompensationExecutorTest {

    @Mock
    private QwBenefitClient qwBenefitClient;

    @Mock
    private BenefitOrderRepository benefitOrderRepository;

    @Test
    void shouldBuildQwRequestFromTaskPayload() {
        QwBenefitPurchaseCompensationExecutor executor =
                new QwBenefitPurchaseCompensationExecutor(qwBenefitClient, benefitOrderRepository, new ObjectMapper());
        AsyncCompensationTask task = new AsyncCompensationTask();
        task.setTaskId("task-qw-1");
        task.setTaskType("QW_BENEFIT_PURCHASE_RETRY");
        task.setRequestPayload("""
                {
                  "externalUserId": "user-001",
                  "benefitOrderNo": "ord-001",
                  "productCode": "HUXUAN_CARD",
                  "loanAmount": 300000
                }
                """);
        BenefitOrder benefitOrder = new BenefitOrder();
        benefitOrder.setBenefitOrderNo("ord-001");
        benefitOrder.setSyncStatus(BenefitOrderStatusEnum.SYNC_FAIL.name());
        when(benefitOrderRepository.selectById("ord-001")).thenReturn(benefitOrder);
        when(qwBenefitClient.syncMemberOrder(any())).thenReturn(new QwMemberSyncResponse(
                "qw-order-001",
                "card-001",
                "1710000000000",
                0,
                "HUXUAN_CARD",
                "惠选卡",
                "independence",
                "2026-04-18 12:00:00",
                "2027-04-18 12:00:00"
        ));

        AsyncCompensationExecutor.ExecutionResult result = executor.execute(task);

        ArgumentCaptor<QwMemberSyncRequest> requestCaptor = ArgumentCaptor.forClass(QwMemberSyncRequest.class);
        verify(qwBenefitClient).syncMemberOrder(requestCaptor.capture());
        assertThat(requestCaptor.getValue().uniqueId()).isEqualTo("user-001");
        assertThat(requestCaptor.getValue().partnerOrderNo()).isEqualTo("ord-001");
        assertThat(requestCaptor.getValue().productCode()).isEqualTo("HUXUAN_CARD");
        assertThat(requestCaptor.getValue().payAmount()).isEqualTo(300000L);
        assertThat(result.responsePayload()).contains("qw-order-001");
        ArgumentCaptor<BenefitOrder> orderCaptor = ArgumentCaptor.forClass(BenefitOrder.class);
        verify(benefitOrderRepository).updateById(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getBenefitOrderNo()).isEqualTo("ord-001");
        assertThat(orderCaptor.getValue().getSyncStatus()).isEqualTo(BenefitOrderStatusEnum.SYNC_SUCCESS.name());
    }

    @Test
    void shouldSkipRemoteCallWhenBenefitOrderAlreadySynced() {
        QwBenefitPurchaseCompensationExecutor executor =
                new QwBenefitPurchaseCompensationExecutor(qwBenefitClient, benefitOrderRepository, new ObjectMapper());
        AsyncCompensationTask task = new AsyncCompensationTask();
        task.setTaskId("task-qw-skip");
        task.setTaskType("QW_BENEFIT_PURCHASE_RETRY");
        task.setRequestPayload("""
                {
                  "externalUserId": "user-001",
                  "benefitOrderNo": "ord-synced",
                  "productCode": "HUXUAN_CARD",
                  "loanAmount": 300000
                }
                """);
        BenefitOrder benefitOrder = new BenefitOrder();
        benefitOrder.setBenefitOrderNo("ord-synced");
        benefitOrder.setSyncStatus(BenefitOrderStatusEnum.SYNC_SUCCESS.name());
        when(benefitOrderRepository.selectById("ord-synced")).thenReturn(benefitOrder);

        AsyncCompensationExecutor.ExecutionResult result = executor.execute(task);

        assertThat(result.responsePayload()).contains("SKIPPED_ALREADY_SYNCED");
        org.mockito.Mockito.verifyNoInteractions(qwBenefitClient);
    }

    @Test
    void shouldRejectInvalidPayload() {
        QwBenefitPurchaseCompensationExecutor executor =
                new QwBenefitPurchaseCompensationExecutor(qwBenefitClient, benefitOrderRepository, new ObjectMapper());
        AsyncCompensationTask task = new AsyncCompensationTask();
        task.setTaskId("task-qw-invalid");
        task.setTaskType("QW_BENEFIT_PURCHASE_RETRY");
        task.setRequestPayload("{\"externalUserId\":}");

        assertThatThrownBy(() -> executor.execute(task))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("ASYNC_COMPENSATION_PAYLOAD_INVALID");
    }
}
