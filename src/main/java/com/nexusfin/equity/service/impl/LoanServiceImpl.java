package com.nexusfin.equity.service.impl;

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
import com.nexusfin.equity.service.LoanApprovalQueryService;
import com.nexusfin.equity.service.LoanCalculatorService;
import com.nexusfin.equity.service.LoanService;
import com.nexusfin.equity.service.XiaohuaGatewayService;
import com.nexusfin.equity.service.support.YunkaCallTemplate;
import com.nexusfin.equity.thirdparty.yunka.YunkaGatewayClient;
import static com.nexusfin.equity.util.BizIds.next;
import static com.nexusfin.equity.util.MoneyUnits.yuanToCent;
import com.nexusfin.equity.util.TraceIdUtil;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoanServiceImpl implements LoanService {

    private static final Logger log = LoggerFactory.getLogger(LoanServiceImpl.class);
    private static final String DEFAULT_CHANNEL_CODE = "KJ";

    private final H5LoanProperties h5LoanProperties;
    private final H5BenefitsProperties h5BenefitsProperties;
    private final YunkaProperties yunkaProperties;
    private final YunkaGatewayClient yunkaGatewayClient;
    private final LoanApplicationMappingRepository loanApplicationMappingRepository;
    private final BenefitOrderService benefitOrderService;
    private final H5I18nService h5I18nService;
    private final AsyncCompensationEnqueueService asyncCompensationEnqueueService;
    private final YunkaCallTemplate yunkaCallTemplate;
    private final LoanApprovalQueryService loanApprovalQueryService;
    private final LoanCalculatorService loanCalculatorService;

    public LoanServiceImpl(
            H5LoanProperties h5LoanProperties,
            H5BenefitsProperties h5BenefitsProperties,
            YunkaProperties yunkaProperties,
            YunkaGatewayClient yunkaGatewayClient,
            LoanApplicationMappingRepository loanApplicationMappingRepository,
            BenefitOrderService benefitOrderService,
            H5I18nService h5I18nService,
            AsyncCompensationEnqueueService asyncCompensationEnqueueService,
            XiaohuaGatewayService xiaohuaGatewayService,
            YunkaCallTemplate yunkaCallTemplate,
            LoanApprovalQueryService loanApprovalQueryService,
            LoanCalculatorService loanCalculatorService
    ) {
        this.h5LoanProperties = h5LoanProperties;
        this.h5BenefitsProperties = h5BenefitsProperties;
        this.yunkaProperties = yunkaProperties;
        this.yunkaGatewayClient = yunkaGatewayClient;
        this.loanApplicationMappingRepository = loanApplicationMappingRepository;
        this.benefitOrderService = benefitOrderService;
        this.h5I18nService = h5I18nService;
        this.asyncCompensationEnqueueService = asyncCompensationEnqueueService;
        this.yunkaCallTemplate = yunkaCallTemplate;
        this.loanApprovalQueryService = loanApprovalQueryService;
        this.loanCalculatorService = loanCalculatorService;
    }

    @Override
    public LoanCalculatorConfigResponse getCalculatorConfig() {
        return loanCalculatorService.getCalculatorConfig();
    }

    @Override
    public LoanCalculateResponse calculate(String memberId, String uid, LoanCalculateRequest request) {
        return loanCalculatorService.calculate(memberId, uid, request);
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
        try {
            response = yunkaCallTemplate.execute(
                    YunkaCallTemplate.YunkaCall.of(
                            "loan apply",
                            requestId,
                            yunkaProperties.paths().loanApply(),
                            applicationId,
                            forwardData
                    ).withMemberId(memberId).withBenefitOrderNo(benefitOrder.benefitOrderNo()),
                    gatewayResponse -> {
                        YunkaGatewayClient.YunkaGatewayResponse presentResponse =
                                yunkaCallTemplate.requirePresentResponse(gatewayResponse);
                        if (!yunkaCallTemplate.isSuccessful(presentResponse)) {
                            throw new BizException(ErrorCodes.YUNKA_UPSTREAM_REJECTED, presentResponse.message());
                        }
                        return presentResponse;
                    }
            );
        } catch (UpstreamTimeoutException exception) {
            log.warn("traceId={} bizOrderNo={} requestId={} memberId={} benefitOrderNo={} path={} elapsedMs={} errorNo={} errorMsg={}",
                    TraceIdUtil.getTraceId(),
                    applicationId,
                    requestId,
                    memberId,
                    benefitOrder.benefitOrderNo(),
                    yunkaProperties.paths().loanApply(),
                    java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos),
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
        } catch (BizException exception) {
            log.warn("traceId={} bizOrderNo={} requestId={} memberId={} benefitOrderNo={} path={} elapsedMs={} errorNo={} errorMsg={}",
                    TraceIdUtil.getTraceId(),
                    applicationId,
                    requestId,
                    memberId,
                    benefitOrder.benefitOrderNo(),
                    yunkaProperties.paths().loanApply(),
                    java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos),
                    exception.getErrorNo(),
                    exception.getErrorMsg());
            return buildLoanFailedResponse(
                    applicationId,
                    benefitOrder.benefitOrderNo(),
                    (ErrorCodes.YUNKA_RESPONSE_EMPTY.equals(exception.getErrorNo())
                            || ErrorCodes.YUNKA_UPSTREAM_REJECTED.equals(exception.getErrorNo()))
                            ? exception.getErrorMsg()
                            : exception.getMessage()
            );
        } catch (RuntimeException exception) {
            log.warn("traceId={} bizOrderNo={} requestId={} memberId={} benefitOrderNo={} path={} elapsedMs={} errorNo={} errorMsg={}",
                    TraceIdUtil.getTraceId(),
                    applicationId,
                    requestId,
                    memberId,
                    benefitOrder.benefitOrderNo(),
                    yunkaProperties.paths().loanApply(),
                    java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos),
                    exception instanceof BizException bizException
                            ? bizException.getErrorNo()
                            : ErrorCodes.YUNKA_UPSTREAM_FAILED,
                    exception instanceof BizException bizException
                            ? bizException.getErrorMsg()
                            : exception.getMessage());
            return buildLoanFailedResponse(applicationId, benefitOrder.benefitOrderNo(), exception.getMessage());
        }
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
        return loanApprovalQueryService.getApprovalStatus(memberId, applicationId);
    }

    @Override
    public LoanApprovalResultResponse getApprovalResult(String memberId, String applicationId) {
        return loanApprovalQueryService.getApprovalResult(memberId, applicationId);
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

    private String readRemark(JsonNode data) {
        return readRemark(data, "借款申请未通过，权益已购买成功。");
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
}
