package com.nexusfin.equity.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.nexusfin.equity.config.H5BenefitsProperties;
import com.nexusfin.equity.config.H5LoanProperties;
import com.nexusfin.equity.config.YunkaProperties;
import com.nexusfin.equity.dto.request.CreateBenefitOrderRequest;
import com.nexusfin.equity.dto.request.LoanApplyRequest;
import com.nexusfin.equity.dto.request.LoanCalculateRequest;
import com.nexusfin.equity.dto.response.CreateBenefitOrderResponse;
import com.nexusfin.equity.dto.response.LoanApprovalResultResponse;
import com.nexusfin.equity.dto.response.LoanApprovalStatusResponse;
import com.nexusfin.equity.dto.response.LoanApplyResponse;
import com.nexusfin.equity.dto.response.LoanCalculateResponse;
import com.nexusfin.equity.dto.response.LoanCalculatorConfigResponse;
import com.nexusfin.equity.entity.LoanApplicationMapping;
import com.nexusfin.equity.exception.BenefitPurchaseSyncTimeoutCompensationException;
import com.nexusfin.equity.exception.BizException;
import com.nexusfin.equity.exception.ErrorCodes;
import com.nexusfin.equity.exception.UpstreamTimeoutException;
import com.nexusfin.equity.repository.LoanApplicationMappingRepository;
import com.nexusfin.equity.service.AsyncCompensationEnqueueService;
import com.nexusfin.equity.service.BenefitOrderService;
import com.nexusfin.equity.service.H5I18nService;
import com.nexusfin.equity.service.LoanService;
import com.nexusfin.equity.service.XiaohuaGatewayService;
import com.nexusfin.equity.thirdparty.yunka.LoanRepayPlanRequest;
import com.nexusfin.equity.thirdparty.yunka.YunkaGatewayClient;
import static com.nexusfin.equity.util.BizIds.next;
import static com.nexusfin.equity.util.MoneyUnits.centsToYuan;
import static com.nexusfin.equity.util.MoneyUnits.yuanToCent;
import com.nexusfin.equity.util.TraceIdUtil;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoanServiceImpl implements LoanService {

    private static final Logger log = LoggerFactory.getLogger(LoanServiceImpl.class);
    private static final String LOAN_STATUS_SUCCESS = "7001";
    private static final String LOAN_STATUS_PROCESSING = "7002";
    private static final String LOAN_STATUS_FAILURE = "7003";
    private static final String DEFAULT_CHANNEL_CODE = "KJ";

    private final H5LoanProperties h5LoanProperties;
    private final H5BenefitsProperties h5BenefitsProperties;
    private final YunkaProperties yunkaProperties;
    private final YunkaGatewayClient yunkaGatewayClient;
    private final LoanApplicationMappingRepository loanApplicationMappingRepository;
    private final BenefitOrderService benefitOrderService;
    private final H5I18nService h5I18nService;
    private final AsyncCompensationEnqueueService asyncCompensationEnqueueService;
    private final XiaohuaGatewayService xiaohuaGatewayService;

    public LoanServiceImpl(
            H5LoanProperties h5LoanProperties,
            H5BenefitsProperties h5BenefitsProperties,
            YunkaProperties yunkaProperties,
            YunkaGatewayClient yunkaGatewayClient,
            LoanApplicationMappingRepository loanApplicationMappingRepository,
            BenefitOrderService benefitOrderService,
            H5I18nService h5I18nService,
            AsyncCompensationEnqueueService asyncCompensationEnqueueService,
            XiaohuaGatewayService xiaohuaGatewayService
    ) {
        this.h5LoanProperties = h5LoanProperties;
        this.h5BenefitsProperties = h5BenefitsProperties;
        this.yunkaProperties = yunkaProperties;
        this.yunkaGatewayClient = yunkaGatewayClient;
        this.loanApplicationMappingRepository = loanApplicationMappingRepository;
        this.benefitOrderService = benefitOrderService;
        this.h5I18nService = h5I18nService;
        this.asyncCompensationEnqueueService = asyncCompensationEnqueueService;
        this.xiaohuaGatewayService = xiaohuaGatewayService;
    }

    @Override
    public LoanCalculatorConfigResponse getCalculatorConfig() {
        H5LoanProperties.ReceivingAccount receivingAccount = h5LoanProperties.receivingAccount();
        return new LoanCalculatorConfigResponse(
                new LoanCalculatorConfigResponse.AmountRange(
                        h5LoanProperties.amountRange().min(),
                        h5LoanProperties.amountRange().max(),
                        h5LoanProperties.amountRange().step(),
                        h5LoanProperties.amountRange().defaultAmount()
                ),
                mapTermOptions(h5LoanProperties.termOptions()),
                h5LoanProperties.annualRate(),
                h5I18nService.text("loan.lender", h5LoanProperties.lender()),
                new LoanCalculatorConfigResponse.ReceivingAccount(
                        h5I18nService.text("loan.receivingAccount.bankName", receivingAccount.bankName()),
                        receivingAccount.lastFour(),
                        receivingAccount.accountId()
                )
        );
    }

    @Override
    public LoanCalculateResponse calculate(String memberId, String uid, LoanCalculateRequest request) {
        validateCalculateRequest(request);
        String requestId = next("LC");
        long startNanos = System.nanoTime();
        YunkaGatewayClient.YunkaGatewayRequest gatewayRequest = new YunkaGatewayClient.YunkaGatewayRequest(
                requestId,
                yunkaProperties.paths().loanCalculate(),
                requestId,
                new LoanTrailForwardData(uid, requestId, yuanToCent(request.amount()), request.term())
        );
        log.info("traceId={} bizOrderNo={} requestId={} memberId={} path={} loan calculate yunka request begin",
                TraceIdUtil.getTraceId(),
                requestId,
                requestId,
                memberId,
                gatewayRequest.path());
        JsonNode data;
        try {
            data = requireSuccessfulYunkaData(yunkaGatewayClient.proxy(gatewayRequest));
        } catch (BizException exception) {
            log.warn("traceId={} bizOrderNo={} requestId={} memberId={} path={} elapsedMs={} errorNo={} errorMsg={}",
                    TraceIdUtil.getTraceId(),
                    requestId,
                    requestId,
                    memberId,
                    gatewayRequest.path(),
                    elapsedMs(startNanos),
                    exception.getErrorNo(),
                    exception.getErrorMsg());
            throw exception;
        }
        log.info("traceId={} bizOrderNo={} requestId={} memberId={} path={} elapsedMs={} loan calculate yunka request success",
                TraceIdUtil.getTraceId(),
                requestId,
                requestId,
                memberId,
                gatewayRequest.path(),
                elapsedMs(startNanos));
        long receiveAmount = data.path("receiveAmount").asLong(yuanToCent(request.amount()));
        long repayAmount = data.path("repayAmount").asLong(receiveAmount);
        return new LoanCalculateResponse(
                centsToYuan(repayAmount - receiveAmount),
                readAnnualRate(data),
                mapRepaymentPlan(data.path("repayPlan"))
        );
    }

    @Override
    @Transactional(noRollbackFor = BenefitPurchaseSyncTimeoutCompensationException.class)
    public LoanApplyResponse apply(String memberId, String uid, LoanApplyRequest request) {
        validateApplyRequest(request);
        String applicationId = next("APP");
        String loanId = next("LN");
        CreateBenefitOrderResponse benefitOrder = benefitOrderService.createOrder(
                memberId,
                new CreateBenefitOrderRequest(
                        "loan-apply-" + applicationId,
                        h5BenefitsProperties.productCode(),
                        yuanToCent(request.amount()),
                        Boolean.TRUE
                )
        );
        YunkaGatewayClient.YunkaGatewayResponse response;
        String requestId = next("LA");
        String upstreamBankCardNum = resolveBankCardNum(request);
        String platformBenefitOrderNo = resolvePlatformBenefitOrderNo(
                benefitOrder.benefitOrderNo(),
                request.platformBenefitOrderNo()
        );
        LoanApplyForwardData forwardData = new LoanApplyForwardData(
                uid,
                benefitOrder.benefitOrderNo(),
                platformBenefitOrderNo,
                applicationId,
                loanId,
                yuanToCent(request.amount()),
                request.term(),
                upstreamBankCardNum,
                upstreamBankCardNum,
                request.purpose(),
                request.loanReason(),
                request.basicInfo(),
                request.idInfo(),
                request.contactInfo(),
                request.supplementInfo(),
                request.optionInfo(),
                request.imageInfo()
        );
        long startNanos = System.nanoTime();
        log.info("traceId={} bizOrderNo={} requestId={} memberId={} benefitOrderNo={} path={} loan apply yunka request begin",
                TraceIdUtil.getTraceId(),
                applicationId,
                requestId,
                memberId,
                benefitOrder.benefitOrderNo(),
                yunkaProperties.paths().loanApply());
        try {
            response = yunkaGatewayClient.proxy(new YunkaGatewayClient.YunkaGatewayRequest(
                    requestId,
                    yunkaProperties.paths().loanApply(),
                    applicationId,
                    forwardData
            ));
        } catch (UpstreamTimeoutException exception) {
            log.warn("traceId={} bizOrderNo={} requestId={} memberId={} benefitOrderNo={} path={} elapsedMs={} errorNo={} errorMsg={}",
                    TraceIdUtil.getTraceId(),
                    applicationId,
                    requestId,
                    memberId,
                    benefitOrder.benefitOrderNo(),
                    yunkaProperties.paths().loanApply(),
                    elapsedMs(startNanos),
                    ErrorCodes.YUNKA_UPSTREAM_TIMEOUT,
                    "Yunka loan apply timeout, async compensation enqueued");
            saveApplicationMapping(memberId, uid, applicationId, benefitOrder.benefitOrderNo(), loanId, request.purpose(), "PENDING_REVIEW");
            asyncCompensationEnqueueService.enqueue(new AsyncCompensationEnqueueService.EnqueueCommand(
                    "YUNKA_LOAN_APPLY_RETRY",
                    "LOAN_APPLY:" + applicationId,
                    applicationId,
                    "YUNKA",
                    yunkaProperties.gatewayPath(),
                    "POST",
                    null,
                    """
                    {"requestId":"%s","path":"%s","bizOrderNo":"%s","uid":"%s","benefitOrderNo":"%s","applyId":"%s","loanId":"%s","loanAmount":%d,"loanPeriod":%d,"bankCardNo":"%s"}
                    """.formatted(
                            requestId,
                            yunkaProperties.paths().loanApply(),
                            applicationId,
                            uid,
                            benefitOrder.benefitOrderNo(),
                            applicationId,
                            loanId,
                            yuanToCent(request.amount()),
                            request.term(),
                            upstreamBankCardNum
                    ).replace("\n", "").trim()
            ));
            return new LoanApplyResponse(
                    applicationId,
                    "pending",
                    h5I18nService.text("loan.approval.arrivalTime", "30分钟"),
                    true,
                    benefitOrder.benefitOrderNo(),
                    h5I18nService.text("loan.apply.pendingReview", "借款申请已提交，正在审核中")
            );
        } catch (RuntimeException exception) {
            log.warn("traceId={} bizOrderNo={} requestId={} memberId={} benefitOrderNo={} path={} elapsedMs={} errorNo={} errorMsg={}",
                    TraceIdUtil.getTraceId(),
                    applicationId,
                    requestId,
                    memberId,
                    benefitOrder.benefitOrderNo(),
                    yunkaProperties.paths().loanApply(),
                    elapsedMs(startNanos),
                    exception instanceof BizException bizException
                            ? bizException.getErrorNo()
                            : ErrorCodes.YUNKA_UPSTREAM_FAILED,
                    exception instanceof BizException bizException
                            ? bizException.getErrorMsg()
                            : exception.getMessage());
            return buildLoanFailedResponse(applicationId, benefitOrder.benefitOrderNo(), exception.getMessage());
        }
        if (response == null || response.code() != 0) {
            String message = response == null ? "Yunka gateway response is empty" : response.message();
            log.warn("traceId={} bizOrderNo={} requestId={} memberId={} benefitOrderNo={} path={} elapsedMs={} errorNo={} errorMsg={}",
                    TraceIdUtil.getTraceId(),
                    applicationId,
                    requestId,
                    memberId,
                    benefitOrder.benefitOrderNo(),
                    yunkaProperties.paths().loanApply(),
                    elapsedMs(startNanos),
                    response == null ? ErrorCodes.YUNKA_RESPONSE_EMPTY : ErrorCodes.YUNKA_UPSTREAM_REJECTED,
                    message);
            return buildLoanFailedResponse(applicationId, benefitOrder.benefitOrderNo(), message);
        }
        log.info("traceId={} bizOrderNo={} requestId={} memberId={} benefitOrderNo={} path={} elapsedMs={} loan apply yunka request success",
                TraceIdUtil.getTraceId(),
                applicationId,
                requestId,
                memberId,
                benefitOrder.benefitOrderNo(),
                yunkaProperties.paths().loanApply(),
                elapsedMs(startNanos));
        String upstreamLoanId = readText(response.data(), "loanId", loanId);
        saveApplicationMapping(memberId, uid, applicationId, benefitOrder.benefitOrderNo(), upstreamLoanId, request.purpose(), "ACTIVE");
        return new LoanApplyResponse(
                applicationId,
                "pending",
                h5I18nService.text("loan.approval.arrivalTime", "30分钟"),
                true,
                benefitOrder.benefitOrderNo(),
                readRemark(response.data(), "借款申请已提交，正在处理中")
        );
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

    private List<LoanCalculatorConfigResponse.TermOption> mapTermOptions(List<H5LoanProperties.TermOption> termOptions) {
        return termOptions.stream()
                .map(termOption -> new LoanCalculatorConfigResponse.TermOption(
                        h5I18nService.text("loan.term." + termOption.value(), termOption.label()),
                        termOption.value()
                ))
                .toList();
    }

    private void validateCalculateRequest(LoanCalculateRequest request) {
        validateAmountAndTerm(request.amount(), request.term());
    }

    private void validateApplyRequest(LoanApplyRequest request) {
        validateAmountAndTerm(request.amount(), request.term());
        String accountId = h5LoanProperties.receivingAccount().accountId();
        if (!accountId.equals(request.receivingAccountId())) {
            throw new BizException(400, "receiving account is unsupported");
        }
    }

    private void validateAmountAndTerm(Long amount, Integer term) {
        H5LoanProperties.AmountRange amountRange = h5LoanProperties.amountRange();
        if (amount < amountRange.min() || amount > amountRange.max()) {
            throw new BizException(400, "amount is out of range");
        }
        if ((amount - amountRange.min()) % amountRange.step() != 0) {
            throw new BizException(400, "amount step is invalid");
        }
        boolean supportedTerm = h5LoanProperties.termOptions().stream()
                .anyMatch(termOption -> termOption.value().equals(term));
        if (!supportedTerm) {
            throw new BizException(400, "term is unsupported");
        }
    }

    private LoanApplyResponse buildLoanFailedResponse(String applicationId, String benefitOrderNo, String reason) {
        String safeReason = reason == null || reason.isBlank() ? "Yunka gateway response is empty" : reason;
        return new LoanApplyResponse(
                null,
                "loan_failed",
                null,
                true,
                benefitOrderNo,
                h5I18nService.text("loan.apply.failurePrefix", "权益购买成功，借款申请失败：") + safeReason
        );
    }

    private void saveApplicationMapping(
            String memberId,
            String uid,
            String applicationId,
            String benefitOrderNo,
            String loanId,
            String purpose,
            String mappingStatus
    ) {
        LoanApplicationMapping mapping = new LoanApplicationMapping();
        mapping.setApplicationId(applicationId);
        mapping.setMemberId(memberId);
        mapping.setBenefitOrderNo(benefitOrderNo);
        mapping.setChannelCode(DEFAULT_CHANNEL_CODE);
        mapping.setExternalUserId(uid);
        mapping.setUpstreamQueryType("loanId");
        mapping.setUpstreamQueryValue(loanId);
        mapping.setPurpose(purpose);
        mapping.setMappingStatus(mappingStatus);
        mapping.setCreatedTs(LocalDateTime.now());
        mapping.setUpdatedTs(LocalDateTime.now());
        loanApplicationMappingRepository.insert(mapping);
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
        YunkaGatewayClient.YunkaGatewayRequest gatewayRequest = new YunkaGatewayClient.YunkaGatewayRequest(
                requestId,
                yunkaProperties.paths().loanQuery(),
                mapping.getApplicationId(),
                new LoanQueryForwardData(mapping.getExternalUserId(), mapping.getUpstreamQueryValue())
        );
        long startNanos = System.nanoTime();
        log.info("traceId={} bizOrderNo={} requestId={} memberId={} path={} loan query yunka request begin",
                TraceIdUtil.getTraceId(),
                mapping.getApplicationId(),
                requestId,
                mapping.getMemberId(),
                gatewayRequest.path());
        try {
            JsonNode data = requireSuccessfulYunkaData(yunkaGatewayClient.proxy(gatewayRequest));
            log.info("traceId={} bizOrderNo={} requestId={} memberId={} path={} elapsedMs={} loan query yunka request success",
                    TraceIdUtil.getTraceId(),
                    mapping.getApplicationId(),
                    requestId,
                    mapping.getMemberId(),
                    gatewayRequest.path(),
                    elapsedMs(startNanos));
            return data;
        } catch (BizException exception) {
            log.warn("traceId={} bizOrderNo={} requestId={} memberId={} path={} elapsedMs={} errorNo={} errorMsg={}",
                    TraceIdUtil.getTraceId(),
                    mapping.getApplicationId(),
                    requestId,
                    mapping.getMemberId(),
                    gatewayRequest.path(),
                    elapsedMs(startNanos),
                    exception.getErrorNo(),
                    exception.getErrorMsg());
            throw exception;
        }
    }

    private JsonNode requireSuccessfulYunkaData(YunkaGatewayClient.YunkaGatewayResponse response) {
        if (response == null || response.code() != 0) {
            String message = response == null ? "Yunka gateway response is empty" : response.message();
            throw new BizException(502, message);
        }
        return response.data();
    }

    private List<LoanCalculateResponse.RepaymentPlanItem> mapRepaymentPlan(JsonNode repaymentPlan) {
        if (!repaymentPlan.isArray()) {
            return List.of();
        }
        return java.util.stream.StreamSupport.stream(repaymentPlan.spliterator(), false)
                .map(item -> new LoanCalculateResponse.RepaymentPlanItem(
                        item.path("period").asInt(),
                        item.path("date").asText(),
                        centsToYuan(item.path("principal").asLong()),
                        centsToYuan(item.path("interest").asLong()),
                        centsToYuan(item.path("total").asLong())
                ))
                .toList();
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

    private String readAnnualRate(JsonNode data) {
        JsonNode yearRate = data.path("yearRate");
        if (yearRate.isTextual()) {
            return yearRate.asText();
        }
        if (yearRate.isNumber()) {
            return yearRate.decimalValue().setScale(1, RoundingMode.HALF_UP) + "%";
        }
        return h5LoanProperties.annualRate().multiply(BigDecimal.valueOf(100L))
                .setScale(1, RoundingMode.HALF_UP) + "%";
    }

    private String readRemark(JsonNode data) {
        return readRemark(data, "借款申请未通过，权益已购买成功。");
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

    private String resolveBankCardNum(LoanApplyRequest request) {
        if (hasText(request.bankCardNum())) {
            return request.bankCardNum();
        }
        return request.receivingAccountId();
    }

    private String resolvePlatformBenefitOrderNo(String benefitOrderNo, String requestPlatformBenefitOrderNo) {
        if (hasText(requestPlatformBenefitOrderNo)) {
            return requestPlatformBenefitOrderNo;
        }
        return benefitOrderNo;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
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

    private String readRemark(JsonNode data, String fallback) {
        String remark = readText(data, "remark", fallback);
        return remark.isBlank() ? fallback : remark;
    }

    private String readText(JsonNode data, String fieldName, String fallback) {
        if (data == null || data.isNull()) {
            return fallback;
        }
        String value = data.path(fieldName).asText();
        return value.isBlank() ? fallback : value;
    }

    private static long elapsedMs(long startNanos) {
        return java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
    }

    private record LoanTrailForwardData(
            String uid,
            String applyId,
            Long loanAmount,
            Integer loanPeriod
    ) {
    }

    private record LoanApplyForwardData(
            String uid,
            String benefitOrderNo,
            String platformBenefitOrderNo,
            String applyId,
            String loanId,
            Long loanAmount,
            Integer loanPeriod,
            String bankCardNo,
            String bankCardNum,
            String purpose,
            String loanReason,
            JsonNode basicInfo,
            JsonNode idInfo,
            JsonNode contactInfo,
            JsonNode supplementInfo,
            JsonNode optionInfo,
            JsonNode imageInfo
    ) {
    }

    private record LoanQueryForwardData(
            String uid,
            String loanId
    ) {
    }
}
