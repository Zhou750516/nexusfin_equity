package com.nexusfin.equity.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusfin.equity.entity.AsyncCompensationTask;
import com.nexusfin.equity.entity.BenefitOrder;
import com.nexusfin.equity.enums.BenefitOrderStatusEnum;
import com.nexusfin.equity.exception.BizException;
import com.nexusfin.equity.repository.BenefitOrderRepository;
import com.nexusfin.equity.service.AsyncCompensationExecutor;
import com.nexusfin.equity.thirdparty.qw.QwBenefitClient;
import com.nexusfin.equity.thirdparty.qw.QwMemberSyncRequest;
import com.nexusfin.equity.thirdparty.qw.QwMemberSyncResponse;
import org.springframework.stereotype.Component;

@Component
public class QwBenefitPurchaseCompensationExecutor implements AsyncCompensationExecutor {

    private final QwBenefitClient qwBenefitClient;
    private final BenefitOrderRepository benefitOrderRepository;
    private final ObjectMapper objectMapper;

    public QwBenefitPurchaseCompensationExecutor(
            QwBenefitClient qwBenefitClient,
            BenefitOrderRepository benefitOrderRepository,
            ObjectMapper objectMapper
    ) {
        this.qwBenefitClient = qwBenefitClient;
        this.benefitOrderRepository = benefitOrderRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public String taskType() {
        return "QW_BENEFIT_PURCHASE_RETRY";
    }

    @Override
    public ExecutionResult execute(AsyncCompensationTask task) {
        QwBenefitPurchasePayload payload = readPayload(task);
        BenefitOrder benefitOrder = benefitOrderRepository.selectById(payload.benefitOrderNo());
        if (benefitOrder != null && BenefitOrderStatusEnum.SYNC_SUCCESS.name().equals(benefitOrder.getSyncStatus())) {
            return new ExecutionResult("""
                    {"code":"SKIPPED_ALREADY_SYNCED","benefitOrderNo":"%s"}
                    """.formatted(payload.benefitOrderNo()).replace("\n", "").trim());
        }
        QwMemberSyncResponse response = qwBenefitClient.syncMemberOrder(new QwMemberSyncRequest(
                payload.externalUserId(),
                payload.benefitOrderNo(),
                payload.loanAmount(),
                payload.productCode(),
                payload.productCode(),
                null,
                null,
                null,
                null,
                0,
                null,
                null,
                null,
                null
        ));
        if (benefitOrder != null) {
            BenefitOrder update = new BenefitOrder();
            update.setBenefitOrderNo(payload.benefitOrderNo());
            update.setSyncStatus(BenefitOrderStatusEnum.SYNC_SUCCESS.name());
            benefitOrderRepository.updateById(update);
        }
        return new ExecutionResult(writeResponse(response));
    }

    private QwBenefitPurchasePayload readPayload(AsyncCompensationTask task) {
        try {
            return objectMapper.readValue(task.getRequestPayload(), QwBenefitPurchasePayload.class);
        } catch (JsonProcessingException exception) {
            throw new BizException("ASYNC_COMPENSATION_PAYLOAD_INVALID",
                    "Failed to parse QW compensation payload");
        }
    }

    private String writeResponse(QwMemberSyncResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException exception) {
            throw new BizException("ASYNC_COMPENSATION_RESPONSE_SERIALIZE_FAILED",
                    "Failed to serialize QW compensation response");
        }
    }

    private record QwBenefitPurchasePayload(
            String externalUserId,
            String benefitOrderNo,
            String productCode,
            Long loanAmount
    ) {
    }
}
