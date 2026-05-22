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

import static com.nexusfin.equity.util.MoneyUnits.centsToYuan;

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
                                resolveOutboundUserId(mapping, payload),
                                payload.benefitOrderNo(),
                                payload.applyId(),
                                payload.applyId(),
                                payload.loanId(),
                                centsToYuan(payload.loanAmount()),
                                payload.loanPeriod(),
                                payload.bankCardNo(),
                                payload.bankCardNo(),
                                payload.phone(),
                                payload.idno(),
                                payload.name(),
                                payload.loanReason(),
                                payload.basicInfo(),
                                payload.idInfo(),
                                payload.contactInfo(),
                                payload.supplementInfo(),
                                payload.optionInfo(),
                                payload.imageInfo()
                        )
                ),
                gatewayResponse -> {
                    YunkaGatewayClient.YunkaGatewayResponse presentResponse =
                            yunkaCallTemplate.requirePresentResponse(gatewayResponse);
                    if (!yunkaCallTemplate.isSuccessful(presentResponse)) {
                        throw new BizException(ErrorCodes.YUNKA_UPSTREAM_REJECTED, presentResponse.message());
                    }
                    return presentResponse;
                }
        );
        markMappingActive(payload.applyId(), response);
        return new ExecutionResult(writeResponse(response));
    }

    private void markMappingActive(String applicationId, YunkaGatewayClient.YunkaGatewayResponse response) {
        LoanApplicationMapping update = new LoanApplicationMapping();
        update.setApplicationId(applicationId);
        update.setMappingStatus("ACTIVE");
        if (response != null && response.data() != null && !response.data().isMissingNode() && !response.data().isNull()) {
            Integer loanId = readLoanId(response.data().path("loanId"));
            if (loanId != null) {
                update.setPlatformLoanId(loanId);
            }
        }
        loanApplicationMappingRepository.updateById(update);
    }

    private YunkaGatewayClient.YunkaGatewayResponse queryExisting(
            LoanApplicationMapping mapping,
            YunkaLoanApplyPayload payload
    ) {
        String outboundUserId = resolveOutboundUserId(mapping, payload);
        Integer platformLoanId = mapping.getPlatformLoanId() == null || mapping.getPlatformLoanId() <= 0
                ? payload.loanId()
                : mapping.getPlatformLoanId();
        if (platformLoanId == null || platformLoanId <= 0) {
            throw new BizException("LOAN_ID_MISSING", "loanId is required for loan query");
        }
        ObjectNode queryData = objectMapper.createObjectNode();
        queryData.put("userId", outboundUserId);
        queryData.put("loanId", platformLoanId);
        try {
            return yunkaCallTemplate.execute(
                    YunkaCallTemplate.YunkaCall.of(
                            "yunka loan apply retry query",
                            payload.requestId(),
                            yunkaProperties.paths().loanQuery(),
                            payload.applyId(),
                            queryData
                    )
            );
        } catch (BizException exception) {
            if (ErrorCodes.YUNKA_RESPONSE_EMPTY.equals(exception.getErrorNo())) {
                return null;
            }
            throw exception;
        }
    }

    private String resolveOutboundUserId(LoanApplicationMapping mapping, YunkaLoanApplyPayload payload) {
        String memberId = mapping != null ? mapping.getMemberId() : null;
        if (memberId == null || memberId.isBlank()) {
            memberId = payload.memberId();
        }
        if (memberId == null || memberId.isBlank()) {
            throw new BizException("ASYNC_COMPENSATION_MEMBER_ID_MISSING",
                    "Yunka loan apply retry requires ABS memberId for outbound userId");
        }
        return memberId;
    }

    private YunkaLoanApplyPayload readPayload(AsyncCompensationTask task) {
        try {
            return objectMapper.readValue(task.getRequestPayload(), YunkaLoanApplyPayload.class);
        } catch (JsonProcessingException exception) {
            throw new BizException("ASYNC_COMPENSATION_PAYLOAD_INVALID",
                    "Failed to parse Yunka compensation payload");
        }
    }

    private Integer readLoanId(com.fasterxml.jackson.databind.JsonNode loanIdNode) {
        if (loanIdNode == null || loanIdNode.isMissingNode() || loanIdNode.isNull()) {
            return null;
        }
        if (loanIdNode.isInt() || loanIdNode.isLong()) {
            int loanId = loanIdNode.asInt();
            return loanId > 0 ? loanId : null;
        }
        String text = loanIdNode.asText("");
        if (text.isBlank()) {
            return null;
        }
        try {
            int loanId = Integer.parseInt(text);
            return loanId > 0 ? loanId : null;
        } catch (NumberFormatException exception) {
            throw new BizException("YUNKA_RESPONSE_INVALID", "Yunka loan apply response loanId is invalid");
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
            String memberId,
            String uid,
            String benefitOrderNo,
            String applyId,
            Integer loanId,
            Long loanAmount,
            Integer loanPeriod,
            String bankCardNo,
            String phone,
            String idno,
            String name,
            String loanReason,
            com.fasterxml.jackson.databind.JsonNode basicInfo,
            com.fasterxml.jackson.databind.JsonNode idInfo,
            com.fasterxml.jackson.databind.JsonNode contactInfo,
            com.fasterxml.jackson.databind.JsonNode supplementInfo,
            com.fasterxml.jackson.databind.JsonNode optionInfo,
            com.fasterxml.jackson.databind.JsonNode imageInfo
    ) {
    }

    private record YunkaLoanApplyForwardData(
            String userId,
            String benefitOrderNo,
            String platformBenefitOrderNo,
            String applyId,
            Integer loanId,
            java.math.BigDecimal loanAmount,
            Integer loanPeriod,
            String bankCardNo,
            String bankCardNum,
            String phone,
            String idno,
            String name,
            String loanReason,
            com.fasterxml.jackson.databind.JsonNode basicInfo,
            com.fasterxml.jackson.databind.JsonNode idInfo,
            com.fasterxml.jackson.databind.JsonNode contactInfo,
            com.fasterxml.jackson.databind.JsonNode supplementInfo,
            com.fasterxml.jackson.databind.JsonNode optionInfo,
            com.fasterxml.jackson.databind.JsonNode imageInfo
    ) {
    }
}
