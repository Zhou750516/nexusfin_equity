package com.nexusfin.equity.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.nexusfin.equity.config.H5BenefitsProperties;
import com.nexusfin.equity.config.YunkaProperties;
import com.nexusfin.equity.dto.response.LoanApprovalResultResponse;
import com.nexusfin.equity.dto.response.LoanApprovalStatusResponse;
import com.nexusfin.equity.entity.LoanApplicationMapping;
import com.nexusfin.equity.exception.BizException;
import com.nexusfin.equity.repository.LoanApplicationMappingRepository;
import com.nexusfin.equity.service.H5I18nService;
import com.nexusfin.equity.service.LoanApprovalQueryService;
import com.nexusfin.equity.service.XiaohuaGatewayService;
import com.nexusfin.equity.service.support.YunkaCallTemplate;
import com.nexusfin.equity.thirdparty.yunka.LoanRepayPlanRequest;
import com.nexusfin.equity.util.TraceIdUtil;
import java.math.BigDecimal;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import static com.nexusfin.equity.util.BizIds.next;
import static com.nexusfin.equity.util.MoneyUnits.centsToYuan;

@Service
public class LoanApprovalQueryServiceImpl implements LoanApprovalQueryService {

    private static final Logger log = LoggerFactory.getLogger(LoanApprovalQueryServiceImpl.class);
    private static final String LOAN_STATUS_SUCCESS = "7001";
    private static final String LOAN_STATUS_PROCESSING = "7002";
    private static final String LOAN_STATUS_FAILURE = "7003";

    private final H5BenefitsProperties h5BenefitsProperties;
    private final YunkaProperties yunkaProperties;
    private final LoanApplicationMappingRepository loanApplicationMappingRepository;
    private final H5I18nService h5I18nService;
    private final XiaohuaGatewayService xiaohuaGatewayService;
    private final YunkaCallTemplate yunkaCallTemplate;

    public LoanApprovalQueryServiceImpl(
            H5BenefitsProperties h5BenefitsProperties,
            YunkaProperties yunkaProperties,
            LoanApplicationMappingRepository loanApplicationMappingRepository,
            H5I18nService h5I18nService,
            XiaohuaGatewayService xiaohuaGatewayService,
            YunkaCallTemplate yunkaCallTemplate
    ) {
        this.h5BenefitsProperties = h5BenefitsProperties;
        this.yunkaProperties = yunkaProperties;
        this.loanApplicationMappingRepository = loanApplicationMappingRepository;
        this.h5I18nService = h5I18nService;
        this.xiaohuaGatewayService = xiaohuaGatewayService;
        this.yunkaCallTemplate = yunkaCallTemplate;
    }

    @Override
    public LoanApprovalStatusResponse getApprovalStatus(String memberId, String applicationId) {
        LoanApplicationMapping mapping = findMapping(memberId, applicationId);
        JsonNode data = queryLoan(mapping);
        String h5Status = mapApprovalStatus(data.path("status").asText());
        return new LoanApprovalStatusResponse(
                applicationId,
                h5Status,
                mapping.getPurpose(),
                buildApprovalStatusSteps(h5Status),
                buildBenefitsCardPreview()
        );
    }

    @Override
    public LoanApprovalResultResponse getApprovalResult(String memberId, String applicationId) {
        LoanApplicationMapping mapping = findMapping(memberId, applicationId);
        JsonNode data = queryLoan(mapping);
        String h5Status = mapApprovalStatus(data.path("status").asText());
        boolean approved = "approved".equals(h5Status);
        boolean reviewing = "reviewing".equals(h5Status);
        return new LoanApprovalResultResponse(
                applicationId,
                h5Status,
                mapping.getPurpose(),
                approved ? centsToYuan(data.path("loanAmount").asLong(0L)) : BigDecimal.ZERO,
                approved || reviewing ? h5I18nService.text("loan.approval.arrivalTime", "30分钟") : "--",
                buildApprovalStatusSteps(h5Status),
                true,
                resolveApprovalResultTip(data, h5Status),
                approved ? mapping.getUpstreamQueryValue() : null,
                approved ? queryRepayPlan(mapping) : List.of()
        );
    }

    private LoanApplicationMapping findMapping(String memberId, String applicationId) {
        LoanApplicationMapping mapping = loanApplicationMappingRepository.selectOne(
                Wrappers.<LoanApplicationMapping>lambdaQuery()
                        .eq(LoanApplicationMapping::getApplicationId, applicationId)
                        .eq(LoanApplicationMapping::getMemberId, memberId)
                        .in(LoanApplicationMapping::getMappingStatus, "ACTIVE", "PENDING_REVIEW")
                        .last("limit 1")
        );
        if (mapping == null) {
            throw new BizException(404, "application mapping not found");
        }
        return mapping;
    }

    private JsonNode queryLoan(LoanApplicationMapping mapping) {
        String requestId = next("LQ");
        return yunkaCallTemplate.executeForData(
                YunkaCallTemplate.YunkaCall.of(
                        "loan query",
                        requestId,
                        yunkaProperties.paths().loanQuery(),
                        mapping.getApplicationId(),
                        new LoanQueryForwardData(mapping.getExternalUserId(), mapping.getUpstreamQueryValue())
                ).withMemberId(mapping.getMemberId())
        );
    }

    private LoanApprovalStatusResponse.BenefitsCardPreview buildBenefitsCardPreview() {
        List<String> features = java.util.stream.IntStream.range(0, h5BenefitsProperties.detail().features().size())
                .mapToObj(index -> {
                    H5BenefitsProperties.Feature feature = h5BenefitsProperties.detail().features().get(index);
                    return h5I18nService.text("benefits.feature." + index + ".title", feature.title());
                })
                .limit(3)
                .toList();
        return new LoanApprovalStatusResponse.BenefitsCardPreview(
                true,
                h5BenefitsProperties.detail().price(),
                features
        );
    }

    private List<LoanApprovalStatusResponse.ApprovalStep> buildApprovalStatusSteps(String status) {
        if ("approved".equals(status)) {
            return buildApprovalResultSteps(true);
        }
        if ("rejected".equals(status)) {
            return buildApprovalResultSteps(false);
        }
        return List.of(
                new LoanApprovalStatusResponse.ApprovalStep(
                        h5I18nService.text("loan.approval.submit.name", "提交申请"),
                        "completed",
                        h5I18nService.text("loan.approval.submit.description", "申请已提交成功")
                ),
                new LoanApprovalStatusResponse.ApprovalStep(
                        h5I18nService.text("loan.approval.reviewing.name", "审批中"),
                        "in_progress",
                        h5I18nService.text("loan.approval.reviewing.description", "正在进行资质审核...")
                ),
                new LoanApprovalStatusResponse.ApprovalStep(
                        h5I18nService.text("loan.approval.waiting.name", "等待放款"),
                        "pending",
                        h5I18nService.text("loan.approval.waiting.description", "审批通过后即可放款")
                )
        );
    }

    private List<LoanApprovalStatusResponse.ApprovalStep> buildApprovalResultSteps(boolean approved) {
        if (approved) {
            return List.of(
                    new LoanApprovalStatusResponse.ApprovalStep(
                            h5I18nService.text("loan.approval.submit.name", "提交申请"),
                            "completed",
                            h5I18nService.text("loan.approval.submit.description", "申请已提交成功")
                    ),
                    new LoanApprovalStatusResponse.ApprovalStep(
                            h5I18nService.text("loan.approval.approved.name", "审批完成"),
                            "completed",
                            h5I18nService.text("loan.approval.approved.description", "资质审核已通过")
                    ),
                    new LoanApprovalStatusResponse.ApprovalStep(
                            h5I18nService.text("loan.approval.disburse.name", "准备放款"),
                            "completed",
                            h5I18nService.text("loan.approval.disburse.description", "资金将在30分钟内到账")
                    )
            );
        }
        return List.of(
                new LoanApprovalStatusResponse.ApprovalStep(
                        h5I18nService.text("loan.approval.submit.name", "提交申请"),
                        "completed",
                        h5I18nService.text("loan.approval.submit.description", "申请已提交成功")
                ),
                new LoanApprovalStatusResponse.ApprovalStep(
                        h5I18nService.text("loan.approval.rejected.name", "审批完成"),
                        "completed",
                        h5I18nService.text("loan.approval.rejected.description", "暂未通过本次审核")
                ),
                new LoanApprovalStatusResponse.ApprovalStep(
                        h5I18nService.text("loan.approval.rejected.disburse.name", "准备放款"),
                        "pending",
                        h5I18nService.text("loan.approval.rejected.disburse.description", "审核未通过，无法放款")
                )
        );
    }

    private String mapApprovalStatus(String upstreamStatus) {
        return switch (upstreamStatus) {
            case LOAN_STATUS_SUCCESS -> "approved";
            case LOAN_STATUS_FAILURE, "7004", "7008", "7009" -> "rejected";
            case LOAN_STATUS_PROCESSING -> "reviewing";
            default -> "reviewing";
        };
    }

    private String resolveApprovalResultTip(JsonNode data, String h5Status) {
        String fallback = switch (h5Status) {
            case "approved" -> h5I18nService.text("loan.approval.result.tip.approved", "审批通过，预计30分钟内到账");
            case "reviewing" -> h5I18nService.text("loan.approval.reviewing.description", "正在进行资质审核...");
            default -> h5I18nService.text("loan.approval.tip.rejected", "借款申请未通过，权益已购买成功。");
        };
        String remark = readText(data, "remark", "");
        if (remark.isBlank()) {
            return fallback;
        }
        if ("approved".equals(h5Status) && isGenericApprovedRemark(remark)) {
            return fallback;
        }
        return remark;
    }

    private List<LoanApprovalResultResponse.RepaymentPlanItem> queryRepayPlan(LoanApplicationMapping mapping) {
        String requestId = next("LRP");
        try {
            var response = xiaohuaGatewayService.queryLoanRepayPlan(
                    requestId,
                    mapping.getApplicationId(),
                    new LoanRepayPlanRequest(mapping.getExternalUserId(), mapping.getUpstreamQueryValue())
            );
            if (response == null || response.repayPlan() == null) {
                return List.of();
            }
            return response.repayPlan().stream()
                    .map(item -> new LoanApprovalResultResponse.RepaymentPlanItem(
                            item.termNo(),
                            item.repayDate(),
                            centsToYuan(defaultLong(item.repayPrincipal())),
                            centsToYuan(defaultLong(item.repayInterest())),
                            centsToYuan(defaultLong(item.repayAmount()))
                    ))
                    .toList();
        } catch (BizException exception) {
            log.warn("traceId={} bizOrderNo={} requestId={} loan repay plan query failed errorNo={} errorMsg={}",
                    TraceIdUtil.getTraceId(),
                    mapping.getApplicationId(),
                    requestId,
                    exception.getErrorNo(),
                    exception.getErrorMsg());
            return List.of();
        }
    }

    private long defaultLong(Long value) {
        return value == null ? 0L : value;
    }

    private boolean isGenericApprovedRemark(String remark) {
        return "放款成功".equals(remark)
                || "审批通过，预计30分钟内到账".equals(remark)
                || "審批通過，預計30分鐘內到帳".equals(remark)
                || "Approved. Funds are expected to arrive within 30 minutes.".equals(remark)
                || "Đã phê duyệt, dự kiến tiền sẽ đến trong vòng 30 phút.".equals(remark);
    }

    private String readText(JsonNode data, String fieldName, String fallback) {
        if (data == null || data.isNull()) {
            return fallback;
        }
        String value = data.path(fieldName).asText();
        return value.isBlank() ? fallback : value;
    }

    private record LoanQueryForwardData(
            String uid,
            String loanId
    ) {
    }
}
