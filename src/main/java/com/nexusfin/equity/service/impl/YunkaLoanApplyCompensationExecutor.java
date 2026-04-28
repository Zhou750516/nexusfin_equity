package com.nexusfin.equity.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nexusfin.equity.config.YunkaProperties;
import com.nexusfin.equity.entity.AsyncCompensationTask;
import com.nexusfin.equity.entity.LoanApplicationMapping;
import com.nexusfin.equity.exception.BizException;
import com.nexusfin.equity.exception.ErrorCodes;
import com.nexusfin.equity.repository.LoanApplicationMappingRepository;
import com.nexusfin.equity.service.AsyncCompensationExecutor;
import com.nexusfin.equity.service.support.YunkaCallTemplate;
import com.nexusfin.equity.thirdparty.yunka.YunkaGatewayClient;
import org.springframework.stereotype.Component;

@Component
public class YunkaLoanApplyCompensationExecutor implements AsyncCompensationExecutor {

    private final YunkaCallTemplate yunkaCallTemplate;
    private final LoanApplicationMappingRepository loanApplicationMappingRepository;
    private final YunkaProperties yunkaProperties;
    private final ObjectMapper objectMapper;

    public YunkaLoanApplyCompensationExecutor(
            YunkaCallTemplate yunkaCallTemplate,
            LoanApplicationMappingRepository loanApplicationMappingRepository,
            YunkaProperties yunkaProperties,
            ObjectMapper objectMapper
    ) {
        this.yunkaCallTemplate = yunkaCallTemplate;
        this.loanApplicationMappingRepository = loanApplicationMappingRepository;
        this.yunkaProperties = yunkaProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public String taskType() {
        return "YUNKA_LOAN_APPLY_RETRY";
    }

    @Override
    public ExecutionResult execute(AsyncCompensationTask task) {
        YunkaLoanApplyPayload payload = readPayload(task);
        LoanApplicationMapping mapping = loanApplicationMappingRepository.selectById(payload.applyId());
        if (mapping != null) {
            if ("ACTIVE".equals(mapping.getMappingStatus())) {
                return new ExecutionResult("""
                        {"code":"SKIPPED_ALREADY_ACTIVE","applicationId":"%s"}
                        """.formatted(payload.applyId()).replace("\n", "").trim());
            }
            if ("PENDING_REVIEW".equals(mapping.getMappingStatus())) {
                YunkaGatewayClient.YunkaGatewayResponse queryResponse = queryExisting(mapping, payload);
                if (yunkaCallTemplate.hasData(queryResponse)) {
                    markMappingActive(payload.applyId(), queryResponse);
                    return new ExecutionResult(writeResponse(queryResponse));
                }
            }
        }
        YunkaGatewayClient.YunkaGatewayResponse response = yunkaCallTemplate.execute(
                YunkaCallTemplate.YunkaCall.of(
                        "yunka loan apply retry apply",
                        payload.requestId(),
                        payload.path(),
                        payload.bizOrderNo(),
                        new YunkaLoanApplyForwardData(
                                payload.uid(),
                                payload.benefitOrderNo(),
                                payload.applyId(),
                                payload.loanId(),
                                payload.loanAmount(),
                                payload.loanPeriod(),
                                payload.bankCardNo()
                        )
                )
        );
        if (!yunkaCallTemplate.isSuccessful(response)) {
            throw new BizException(ErrorCodes.YUNKA_UPSTREAM_REJECTED, response.message());
        }
        markMappingActive(payload.applyId(), response);
        return new ExecutionResult(writeResponse(response));
    }

    private void markMappingActive(String applicationId, YunkaGatewayClient.YunkaGatewayResponse response) {
        LoanApplicationMapping update = new LoanApplicationMapping();
        update.setApplicationId(applicationId);
        update.setMappingStatus("ACTIVE");
        if (response != null && response.data() != null && !response.data().isMissingNode() && !response.data().isNull()) {
            String loanId = response.data().path("loanId").asText(null);
            if (loanId != null && !loanId.isBlank()) {
                update.setUpstreamQueryValue(loanId);
            }
        }
        loanApplicationMappingRepository.updateById(update);
    }

    private YunkaGatewayClient.YunkaGatewayResponse queryExisting(
            LoanApplicationMapping mapping,
            YunkaLoanApplyPayload payload
    ) {
        String externalUserId = mapping.getExternalUserId() == null || mapping.getExternalUserId().isBlank()
                ? payload.uid()
                : mapping.getExternalUserId();
        String upstreamQueryValue = mapping.getUpstreamQueryValue() == null || mapping.getUpstreamQueryValue().isBlank()
                ? payload.loanId()
                : mapping.getUpstreamQueryValue();
        ObjectNode queryData = objectMapper.createObjectNode();
        queryData.put("uid", externalUserId);
        queryData.put("loanId", upstreamQueryValue);
        return yunkaCallTemplate.execute(
                YunkaCallTemplate.YunkaCall.of(
                        "yunka loan apply retry query",
                        payload.requestId(),
                        yunkaProperties.paths().loanQuery(),
                        payload.applyId(),
                        queryData
                )
        );
    }

    private YunkaLoanApplyPayload readPayload(AsyncCompensationTask task) {
        try {
            return objectMapper.readValue(task.getRequestPayload(), YunkaLoanApplyPayload.class);
        } catch (JsonProcessingException exception) {
            throw new BizException("ASYNC_COMPENSATION_PAYLOAD_INVALID",
                    "Failed to parse Yunka compensation payload");
        }
    }

    private String writeResponse(YunkaGatewayClient.YunkaGatewayResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException exception) {
            throw new BizException("ASYNC_COMPENSATION_RESPONSE_SERIALIZE_FAILED",
                    "Failed to serialize Yunka compensation response");
        }
    }

    private record YunkaLoanApplyPayload(
            String requestId,
            String path,
            String bizOrderNo,
            String uid,
            String benefitOrderNo,
            String applyId,
            String loanId,
            Long loanAmount,
            Integer loanPeriod,
            String bankCardNo
    ) {
    }

    private record YunkaLoanApplyForwardData(
            String uid,
            String benefitOrderNo,
            String applyId,
            String loanId,
            Long loanAmount,
            Integer loanPeriod,
            String bankCardNo
    ) {
    }
}
