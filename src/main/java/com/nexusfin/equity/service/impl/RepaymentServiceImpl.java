package com.nexusfin.equity.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.nexusfin.equity.entity.IdempotencyRecord;
import com.nexusfin.equity.config.H5LoanProperties;
import com.nexusfin.equity.config.H5RepaymentProperties;
import com.nexusfin.equity.config.YunkaProperties;
import com.nexusfin.equity.dto.request.RepaymentSmsConfirmRequest;
import com.nexusfin.equity.dto.request.RepaymentSmsSendRequest;
import com.nexusfin.equity.dto.request.RepaymentSubmitRequest;
import com.nexusfin.equity.dto.response.BankAccountResponse;
import com.nexusfin.equity.dto.response.RepaymentInfoResponse;
import com.nexusfin.equity.dto.response.RepaymentResultResponse;
import com.nexusfin.equity.dto.response.RepaymentSmsConfirmResponse;
import com.nexusfin.equity.dto.response.RepaymentSmsSendResponse;
import com.nexusfin.equity.dto.response.RepaymentSubmitResponse;
import com.nexusfin.equity.entity.LoanApplicationMapping;
import com.nexusfin.equity.entity.MemberInfo;
import com.nexusfin.equity.exception.BizException;
import com.nexusfin.equity.exception.ErrorCodes;
import com.nexusfin.equity.exception.UpstreamTimeoutException;
import com.nexusfin.equity.repository.IdempotencyRecordRepository;
import com.nexusfin.equity.repository.LoanApplicationMappingRepository;
import com.nexusfin.equity.repository.MemberInfoRepository;
import com.nexusfin.equity.service.H5I18nService;
import com.nexusfin.equity.service.MemberReceivingAccountService;
import com.nexusfin.equity.service.RepaymentService;
import com.nexusfin.equity.service.XiaohuaGatewayService;
import com.nexusfin.equity.service.support.YunkaCallTemplate;
import com.nexusfin.equity.thirdparty.yunka.CardSmsConfirmRequest;
import com.nexusfin.equity.thirdparty.yunka.CardSmsSendRequest;
import com.nexusfin.equity.thirdparty.yunka.LoanRepayPlanItem;
import com.nexusfin.equity.thirdparty.yunka.LoanRepayPlanRequest;
import com.nexusfin.equity.thirdparty.yunka.UserCardListRequest;
import com.nexusfin.equity.thirdparty.yunka.UserCardSummary;
import com.nexusfin.equity.thirdparty.yunka.YunkaGatewayClient;
import static com.nexusfin.equity.util.BizIds.next;
import static com.nexusfin.equity.util.JsonNodes.readDecimal;
import static com.nexusfin.equity.util.JsonNodes.readLong;
import static com.nexusfin.equity.util.JsonNodes.readRemark;
import static com.nexusfin.equity.util.JsonNodes.readText;
import static com.nexusfin.equity.util.MoneyUnits.yuanToCent;
import com.nexusfin.equity.util.SensitiveDataUtil;
import com.nexusfin.equity.util.SensitiveDataCipher;
import com.nexusfin.equity.util.TraceIdUtil;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
public class RepaymentServiceImpl implements RepaymentService {

    private static final Logger log = LoggerFactory.getLogger(RepaymentServiceImpl.class);
    private static final int REPAY_TYPE_CURRENT = 2;
    private static final int REPAY_TYPE_EARLY_SETTLE = 5;
    private static final int REPAYMENT_SMS_TYPE = 2;
    private static final int REPAY_PLAN_STATUS_UNPAID = 1;
    private static final int REPAY_PLAN_STATUS_OVERDUE = 3;
    private static final long MAX_REASONABLE_EPOCH_SECONDS = 9_999_999_999L;
    private static final String REPAYMENT_REPAY_PLAN_UNAVAILABLE = "REPAYMENT_REPAY_PLAN_UNAVAILABLE";
    private static final String REPAYMENT_AMOUNT_EXCEEDED = "REPAYMENT_AMOUNT_EXCEEDED";
    private static final String REPAYMENT_SUBMIT_DUPLICATED = "REPAYMENT_SUBMIT_DUPLICATED";
    private static final String REPAYMENT_SUBMIT_BIZ_TYPE = "REPAYMENT_SUBMIT";
    private static final String REPAYMENT_RESULT_BIZ_TYPE = "REPAYMENT_RESULT";
    private static final int REPAYMENT_SUBMIT_DUPLICATE_WINDOW_SECONDS = 5;

    private final H5LoanProperties h5LoanProperties;
    private final H5RepaymentProperties h5RepaymentProperties;
    private final YunkaProperties yunkaProperties;
    private final YunkaGatewayClient yunkaGatewayClient;
    private final H5I18nService h5I18nService;
    private final XiaohuaGatewayService xiaohuaGatewayService;
    private final MemberInfoRepository memberInfoRepository;
    private final LoanApplicationMappingRepository loanApplicationMappingRepository;
    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final SensitiveDataCipher sensitiveDataCipher;
    private final MemberReceivingAccountService memberReceivingAccountService;
    private final YunkaCallTemplate yunkaCallTemplate;

    public RepaymentServiceImpl(
            H5LoanProperties h5LoanProperties,
            H5RepaymentProperties h5RepaymentProperties,
            YunkaProperties yunkaProperties,
            YunkaGatewayClient yunkaGatewayClient,
            H5I18nService h5I18nService,
            XiaohuaGatewayService xiaohuaGatewayService,
            MemberInfoRepository memberInfoRepository,
            LoanApplicationMappingRepository loanApplicationMappingRepository,
            IdempotencyRecordRepository idempotencyRecordRepository,
            SensitiveDataCipher sensitiveDataCipher,
            MemberReceivingAccountService memberReceivingAccountService,
            YunkaCallTemplate yunkaCallTemplate
    ) {
        this.h5LoanProperties = h5LoanProperties;
        this.h5RepaymentProperties = h5RepaymentProperties;
        this.yunkaProperties = yunkaProperties;
        this.yunkaGatewayClient = yunkaGatewayClient;
        this.h5I18nService = h5I18nService;
        this.xiaohuaGatewayService = xiaohuaGatewayService;
        this.memberInfoRepository = memberInfoRepository;
        this.loanApplicationMappingRepository = loanApplicationMappingRepository;
        this.idempotencyRecordRepository = idempotencyRecordRepository;
        this.sensitiveDataCipher = sensitiveDataCipher;
        this.memberReceivingAccountService = memberReceivingAccountService;
        this.yunkaCallTemplate = yunkaCallTemplate;
    }

    @Override
    public RepaymentInfoResponse getInfo(String memberId, Integer loanId) {
        LoanApplicationMapping mapping = validateKnownLoanId(memberId, loanId);
        String periods = resolveCurrentDuePeriods(mapping);
        String requestId = next("RT");
        JsonNode data = yunkaCallTemplate.executeForData(
                YunkaCallTemplate.YunkaCall.of(
                        "repayment info",
                        requestId,
                        yunkaProperties.paths().repayTrial(),
                        String.valueOf(loanId),
                        new RepayTrialForwardData(memberId, loanId, REPAY_TYPE_CURRENT, periods)
                )
        );
        List<BankAccountResponse> bankCards = queryRepaymentCards(memberId, String.valueOf(loanId));
        BankAccountResponse selectedCard = bankCards.stream().findFirst().orElseGet(() -> fallbackBankAccount(memberId));
        String defaultTip = h5I18nService.text(
                "repayment.tip.info",
                "还款后将立即生效，剩余期数对应的利息将不再收取。请确认银行卡余额充足。"
        );
        String remark = readText(data, "remark", "");
        return new RepaymentInfoResponse(
                loanId,
                readDecimal(data, "repayAmount", "amount"),
                h5I18nService.text("repayment.type.current", "当前应还"),
                selectedCard,
                bankCards,
                h5RepaymentProperties.smsRequired(),
                remark,
                trialFeeDetails(data),
                defaultText(remark, defaultTip)
        );
    }

    @Override
    public RepaymentSmsSendResponse sendSms(String memberId, RepaymentSmsSendRequest request) {
        MemberProfile memberProfile = loadMemberProfile(memberId);
        String bankCardNum = resolveBankCardNumber(memberId, request.loanId(), request.bankCardId());
        String requestId = next("RSS");
        var response = xiaohuaGatewayService.sendCardSms(
                requestId,
                String.valueOf(request.loanId()),
                new CardSmsSendRequest(
                        memberId,
                        request.loanId(),
                        REPAYMENT_SMS_TYPE,
                        bankCardNum,
                        memberProfile.mobile(),
                        memberProfile.idCardNo(),
                        memberProfile.realName()
                )
        );
        return new RepaymentSmsSendResponse(
                response.smsSeq(),
                mapSmsSendStatus(response.status()),
                defaultText(response.message(), "验证码已发送")
        );
    }

    @Override
    public RepaymentSmsConfirmResponse confirmSms(String memberId, RepaymentSmsConfirmRequest request) {
        MemberProfile memberProfile = loadMemberProfile(memberId);
        String requestId = next("RSC");
        var response = xiaohuaGatewayService.confirmCardSms(
                requestId,
                String.valueOf(request.loanId()),
                new CardSmsConfirmRequest(
                        memberId,
                        memberProfile.mobile(),
                        REPAYMENT_SMS_TYPE,
                        request.loanId(),
                        request.captcha()
                )
        );
        return new RepaymentSmsConfirmResponse(
                mapSmsConfirmStatus(response.status()),
                defaultText(response.message(), "验证码校验成功")
        );
    }

    @Override
    public RepaymentSubmitResponse submit(String memberId, RepaymentSubmitRequest request) {
        String requestId = next("RS");
        LoanApplicationMapping mapping = validateKnownLoanId(memberId, request.loanId());
        String periods = resolveCurrentDuePeriods(mapping);
        long requestedRepayAmount = yuanToCent(request.amount());
        JsonNode data;
        try {
            validateRepaymentAmount(memberId, request.loanId(), request.repaymentType(), periods, requestedRepayAmount);
            reserveRepaymentSubmit(memberId, request.loanId(), requestedRepayAmount);
            String bankCardNum = resolveBankCardNumber(memberId, request.loanId(), request.bankCardId());
            MemberProfile memberProfile = loadMemberProfile(memberId);
            data = yunkaCallTemplate.executeForData(
                    YunkaCallTemplate.YunkaCall.of(
                            "repayment submit",
                            requestId,
                            yunkaProperties.paths().repayApply(),
                            String.valueOf(request.loanId()),
                            new RepayApplyForwardData(
                                    memberId,
                                    request.loanId(),
                                    mapRepayType(request.repaymentType()),
                                    periods,
                                    bankCardNum,
                                    memberProfile.mobile(),
                                    defaultText(mapping.getExternalUserId(), memberId),
                                    memberProfile.idCardNo(),
                                    memberProfile.realName(),
                                    request.amount().setScale(2)
                            )
                    )
            );
        } catch (UpstreamTimeoutException exception) {
            throw new BizException(ErrorCodes.YUNKA_UPSTREAM_TIMEOUT, "Repayment submit temporarily unavailable");
        }
        String swiftNumber = readText(data, "swiftNumber", String.valueOf(request.loanId()));
        persistRepaymentResultReference(memberId, request.loanId(), swiftNumber);
        String status = mapSubmitStatus(readText(data, "status", ""));
        if ("failed".equals(status)) {
            log.warn("traceId={} bizOrderNo={} requestId={} errorNo={} errorMsg={}",
                    TraceIdUtil.getTraceId(),
                    request.loanId(),
                    requestId,
                    "REPAYMENT_SUBMIT_FAILED",
                    readRemark(data, "Repayment submit failed"));
        }
        return new RepaymentSubmitResponse(
                swiftNumber,
                status,
                readRemark(data, "还款请求已提交，正在处理中")
        );
    }

    private void validateRepaymentAmount(
            String memberId,
            Integer loanId,
            String repaymentType,
            String periods,
            long requestedRepayAmount
    ) {
        JsonNode trialData = yunkaCallTemplate.executeForData(
                YunkaCallTemplate.YunkaCall.of(
                        "repayment submit amount validation",
                        next("RTS"),
                        yunkaProperties.paths().repayTrial(),
                        String.valueOf(loanId),
                        new RepayTrialForwardData(memberId, loanId, mapRepayType(repaymentType), periods)
                )
        );
        long repayableAmount = yuanToCent(readDecimal(trialData, "repayAmount", "amount"));
        if (requestedRepayAmount <= repayableAmount) {
            return;
        }
        log.warn("traceId={} bizOrderNo={} errorNo={} requestedRepayAmount={} repayableAmount={} errorMsg={}",
                TraceIdUtil.getTraceId(),
                loanId,
                REPAYMENT_AMOUNT_EXCEEDED,
                requestedRepayAmount,
                repayableAmount,
                "Repayment amount exceeds current repayable amount");
        throw new BizException(REPAYMENT_AMOUNT_EXCEEDED, "Repayment amount exceeds current repayable amount");
    }

    private void reserveRepaymentSubmit(String memberId, Integer loanId, long requestedRepayAmount) {
        LocalDateTime now = LocalDateTime.now();
        String bizKey = repaymentSubmitBizKey(memberId, loanId, requestedRepayAmount);
        IdempotencyRecord existing = idempotencyRecordRepository.selectOne(
                Wrappers.<IdempotencyRecord>lambdaQuery()
                        .eq(IdempotencyRecord::getBizType, REPAYMENT_SUBMIT_BIZ_TYPE)
                        .eq(IdempotencyRecord::getBizKey, bizKey)
                        .ge(IdempotencyRecord::getProcessedTs, now.minusSeconds(REPAYMENT_SUBMIT_DUPLICATE_WINDOW_SECONDS))
                        .orderByDesc(IdempotencyRecord::getProcessedTs)
                        .last("limit 1")
        );
        if (existing != null) {
            log.warn("traceId={} bizOrderNo={} requestId={} errorNo={} errorMsg={}",
                    TraceIdUtil.getTraceId(),
                    loanId,
                    existing.getRequestId(),
                    REPAYMENT_SUBMIT_DUPLICATED,
                    "Repayment request is duplicated");
            throw new BizException(REPAYMENT_SUBMIT_DUPLICATED, "Repayment request is duplicated");
        }
        IdempotencyRecord guardRecord = new IdempotencyRecord();
        guardRecord.setRequestId(repaymentSubmitGuardRequestId(bizKey, now));
        guardRecord.setBizType(REPAYMENT_SUBMIT_BIZ_TYPE);
        guardRecord.setBizKey(bizKey);
        guardRecord.setResponseBody("IN_FLIGHT");
        guardRecord.setProcessedTs(now);
        try {
            idempotencyRecordRepository.insert(guardRecord);
        } catch (DuplicateKeyException exception) {
            log.warn("traceId={} bizOrderNo={} requestId={} errorNo={} errorMsg={}",
                    TraceIdUtil.getTraceId(),
                    loanId,
                    guardRecord.getRequestId(),
                    REPAYMENT_SUBMIT_DUPLICATED,
                    "Repayment request is duplicated");
            throw new BizException(REPAYMENT_SUBMIT_DUPLICATED, "Repayment request is duplicated");
        }
    }

    private String repaymentSubmitBizKey(String memberId, Integer loanId, long requestedRepayAmount) {
        return memberId + ":" + loanId + ":" + requestedRepayAmount;
    }

    private String repaymentSubmitGuardRequestId(String bizKey, LocalDateTime now) {
        long bucket = now.atZone(ZoneOffset.UTC).toEpochSecond() / REPAYMENT_SUBMIT_DUPLICATE_WINDOW_SECONDS;
        return "repay-submit-" + bucket + "-" + SensitiveDataUtil.sha256(bizKey).substring(0, 24);
    }

    private void persistRepaymentResultReference(String memberId, Integer loanId, String swiftNumber) {
        if (swiftNumber == null || swiftNumber.isBlank() || loanId == null) {
            return;
        }
        IdempotencyRecord record = new IdempotencyRecord();
        record.setRequestId(repaymentResultReferenceRequestId(memberId, swiftNumber));
        record.setBizType(REPAYMENT_RESULT_BIZ_TYPE);
        record.setBizKey(repaymentResultBizKey(memberId, swiftNumber));
        record.setResponseBody(String.valueOf(loanId));
        record.setProcessedTs(LocalDateTime.now());
        try {
            idempotencyRecordRepository.insert(record);
        } catch (DuplicateKeyException exception) {
            log.info("traceId={} bizOrderNo={} requestId={} repayment result reference already exists",
                    TraceIdUtil.getTraceId(),
                    loanId,
                    record.getRequestId());
        }
    }

    private String repaymentResultReferenceRequestId(String memberId, String swiftNumber) {
        return "repay-result-" + SensitiveDataUtil.sha256(repaymentResultBizKey(memberId, swiftNumber)).substring(0, 32);
    }

    private String repaymentResultBizKey(String memberId, String swiftNumber) {
        return memberId + ":" + swiftNumber;
    }

    @Override
    public RepaymentResultResponse getResult(String memberId, String repaymentId) {
        Integer loanId = validateAndResolveLoanId(memberId, repaymentId);
        String requestId = next("RQ");
        JsonNode data = yunkaCallTemplate.executeForData(
                YunkaCallTemplate.YunkaCall.of(
                        "repayment result",
                        requestId,
                        yunkaProperties.paths().repayQuery(),
                        repaymentId,
                        new RepayQueryForwardData(memberId, loanId, repaymentId)
                )
        );
        String swiftNumber = readText(data, "swiftNumber", repaymentId);
        BankAccountResponse bankCard = resolveResultBankCard(memberId, swiftNumber, data);
        return new RepaymentResultResponse(
                repaymentId,
                swiftNumber,
                mapResultStatus(readText(data, "status", "")),
                readDecimal(data, "amount", "repayAmount"),
                resolveRepaymentTime(data),
                bankCard,
                readDecimal(data, "discount"),
                readRemark(data, ""),
                repaymentTips()
        );
    }

    private List<BankAccountResponse> queryRepaymentCards(String memberId, String bizOrderNo) {
        try {
            var response = xiaohuaGatewayService.queryUserCards(
                    next("RUC"),
                    bizOrderNo,
                    new UserCardListRequest(memberId)
            );
            if (response == null || response.cards() == null || response.cards().isEmpty()) {
                return List.of(fallbackBankAccount(memberId));
            }
            return response.cards().stream()
                    .sorted((left, right) -> Integer.compare(defaultInt(right.isDefault()), defaultInt(left.isDefault())))
                    .map(this::toBankAccount)
                    .toList();
        } catch (BizException exception) {
            log.warn("traceId={} bizOrderNo={} repayment card query failed errorNo={} errorMsg={}",
                    TraceIdUtil.getTraceId(),
                    bizOrderNo,
                    exception.getErrorNo(),
                    exception.getErrorMsg());
            return List.of(fallbackBankAccount(memberId));
        }
    }

    private BankAccountResponse resolveResultBankCard(String memberId, String bizOrderNo, JsonNode data) {
        String bankCardNum = readText(data, "bankCardNum", "");
        List<BankAccountResponse> bankCards = queryRepaymentCards(memberId, bizOrderNo);
        if (!bankCardNum.isBlank()) {
            return bankCards.stream()
                    .filter(card -> bankCardNum.equals(card.accountId()))
                    .findFirst()
                    .orElse(new BankAccountResponse(
                            h5I18nService.text("loan.receivingAccount.bankName", "银行卡"),
                            lastFour(bankCardNum),
                            bankCardNum
                    ));
        }
        return bankCards.stream().findFirst().orElseGet(() -> fallbackBankAccount(memberId));
    }

    private BankAccountResponse toBankAccount(UserCardSummary card) {
        return new BankAccountResponse(
                defaultText(card.bankName(), h5I18nService.text("loan.receivingAccount.bankName", "银行卡")),
                defaultText(card.cardLastFour(), lastFour(card.cardId())),
                card.cardId()
        );
    }

    private BankAccountResponse fallbackBankAccount(String memberId) {
        MemberReceivingAccountService.ReceivingAccountDetails account =
                memberReceivingAccountService.getDefaultReceivingAccount(memberId);
        return new BankAccountResponse(
                account.bankName(),
                account.lastFour(),
                account.accountId()
        );
    }

    private String resolveBankCardNumber(String memberId, Integer loanId, String bankCardId) {
        return queryRepaymentCards(memberId, String.valueOf(loanId)).stream()
                .filter(card -> bankCardId.equals(card.accountId()))
                .findFirst()
                .map(BankAccountResponse::accountId)
                .orElse(bankCardId);
    }

    private LoanApplicationMapping validateKnownLoanId(String memberId, Integer loanId) {
        LoanApplicationMapping mapping = findLoanMapping(memberId, loanId);
        if (mapping == null) {
            throw new BizException(404, "repayment loan reference not found");
        }
        return mapping;
    }

    private String resolveCurrentDuePeriods(LoanApplicationMapping mapping) {
        Integer loanId = mapping.getPlatformLoanId();
        var response = xiaohuaGatewayService.queryLoanRepayPlan(
                next("LRP"),
                String.valueOf(loanId),
                new LoanRepayPlanRequest(mapping.getMemberId(), loanId)
        );
        List<LoanRepayPlanItem> repayPlan = response == null || response.repayPlan() == null
                ? List.of()
                : response.repayPlan();
        String period = repayPlan.stream()
                .filter(item -> Objects.equals(item.status(), REPAY_PLAN_STATUS_UNPAID)
                        || Objects.equals(item.status(), REPAY_PLAN_STATUS_OVERDUE))
                .map(LoanRepayPlanItem::periodNo)
                .filter(Objects::nonNull)
                .sorted(Comparator.naturalOrder())
                .map(String::valueOf)
                .findFirst()
                .orElse("");
        if (!period.isBlank()) {
            return period;
        }
        log.warn("traceId={} bizOrderNo={} errorNo={} errorMsg={}",
                TraceIdUtil.getTraceId(),
                loanId,
                REPAYMENT_REPAY_PLAN_UNAVAILABLE,
                "No current due periods found for repayment");
        throw new BizException(REPAYMENT_REPAY_PLAN_UNAVAILABLE, "No current due periods found for repayment");
    }

    private Integer validateAndResolveLoanId(String memberId, String repaymentId) {
        Integer storedLoanId = resolveStoredRepaymentLoanId(memberId, repaymentId);
        if (storedLoanId != null && findLoanMapping(memberId, storedLoanId) != null) {
            return storedLoanId;
        }
        String loanIdText = extractLoanId(repaymentId);
        if (loanIdText != null) {
            Integer numericLoanId = parseLoanId(loanIdText);
            if (numericLoanId != null && findLoanMapping(memberId, numericLoanId) != null) {
                return numericLoanId;
            }
        }
        throw new BizException(404, "repayment reference not found");
    }

    private Integer resolveStoredRepaymentLoanId(String memberId, String repaymentId) {
        if (repaymentId == null || repaymentId.isBlank()) {
            return null;
        }
        IdempotencyRecord record = idempotencyRecordRepository.selectById(repaymentResultReferenceRequestId(memberId, repaymentId));
        if (record == null) {
            return null;
        }
        return parseLoanId(record.getResponseBody());
    }

    private LoanApplicationMapping findLoanMapping(String memberId, Integer loanId) {
        return loanApplicationMappingRepository.selectOne(
                Wrappers.<LoanApplicationMapping>lambdaQuery()
                        .eq(LoanApplicationMapping::getMemberId, memberId)
                        .eq(LoanApplicationMapping::getPlatformLoanId, loanId)
                        .last("limit 1")
        );
    }

    private Integer parseLoanId(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String extractLoanId(String repaymentId) {
        if (repaymentId == null || repaymentId.isBlank()) {
            return null;
        }
        if (repaymentId.startsWith("RP-") && repaymentId.length() > 3) {
            return repaymentId.substring(3);
        }
        return null;
    }

    private MemberProfile loadMemberProfile(String memberId) {
        MemberInfo memberInfo = memberInfoRepository.selectById(memberId);
        if (memberInfo == null) {
            throw new BizException(404, "member info not found");
        }
        return new MemberProfile(
                sensitiveDataCipher.decrypt(memberInfo.getMobileEncrypted()),
                sensitiveDataCipher.decrypt(memberInfo.getIdCardEncrypted()),
                sensitiveDataCipher.decrypt(memberInfo.getRealNameEncrypted())
        );
    }

    private int mapRepayType(String repaymentType) {
        if ("scheduled".equalsIgnoreCase(repaymentType)
                || "current".equalsIgnoreCase(repaymentType)
                || "current_due".equalsIgnoreCase(repaymentType)) {
            return REPAY_TYPE_CURRENT;
        }
        return REPAY_TYPE_EARLY_SETTLE;
    }

    private RepaymentInfoResponse.TrialFeeDetails trialFeeDetails(JsonNode data) {
        return new RepaymentInfoResponse.TrialFeeDetails(
                readDecimal(data, "repayPrincipal"),
                readDecimal(data, "repayInterest"),
                readDecimal(data, "repayPenaltyInt"),
                readDecimal(data, "repayBreakFee"),
                readDecimal(data, "repayOtherCharge"),
                readDecimal(data, "repaySvcFee"),
                readDecimal(data, "repayGuaranteeFee"),
                readDecimal(data, "discount"),
                readDecimal(data, "originalRepay")
        );
    }

    private String mapSubmitStatus(String status) {
        if ("5002".equals(status) || "FAILED".equalsIgnoreCase(status) || "FAIL".equalsIgnoreCase(status)) {
            return "failed";
        }
        return "processing";
    }

    private String mapResultStatus(String status) {
        return switch (status) {
            case "SUCCESS", "9001", "8001" -> "success";
            case "8004" -> "processing";
            case "8003", "8006", "FAILED", "FAIL" -> "failed";
            default -> "processing";
        };
    }

    private String mapSmsSendStatus(String status) {
        return "11001".equals(status) ? "sent" : "failed";
    }

    private String mapSmsConfirmStatus(String status) {
        return "11002".equals(status) ? "confirmed" : "failed";
    }

    private String resolveRepaymentTime(JsonNode data) {
        JsonNode successTime = data.path("successTime");
        if (successTime.isNumber()) {
            return formatSuccessTime(successTime.asLong());
        }
        String successTimeText = successTime.asText("");
        if (successTimeText.matches("-?\\d+")) {
            try {
                return formatSuccessTime(Long.parseLong(successTimeText));
            } catch (NumberFormatException exception) {
                return successTimeText;
            }
        }
        return successTimeText;
    }

    private String formatSuccessTime(long timestamp) {
        if (timestamp <= 0) {
            return "";
        }
        Instant instant = timestamp > MAX_REASONABLE_EPOCH_SECONDS
                ? Instant.ofEpochMilli(timestamp)
                : Instant.ofEpochSecond(timestamp);
        return instant.atOffset(ZoneOffset.ofHours(8))
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    private List<String> repaymentTips() {
        return List.of(
                h5I18nService.text("repayment.tip.0", "还款金额已从您的银行卡扣除，请注意查收银行通知"),
                h5I18nService.text("repayment.tip.1", "提前还款后，您的信用额度将即时恢复"),
                h5I18nService.text("repayment.tip.2", "如需查看还款记录，可前往\"我的\"-\"账单明细\""),
                h5I18nService.text("repayment.tip.3", "若有任何疑问，请联系客服：400-888-8888")
        );
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private String lastFour(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.length() <= 4 ? value : value.substring(value.length() - 4);
    }

    private record RepayTrialForwardData(
            String userId,
            Integer loanId,
            int repayType,
            String periods
    ) {
    }

    private record RepayApplyForwardData(
            String userId,
            Integer loanId,
            int repayType,
            String periods,
            String bankCardNum,
            String phone,
            String cid,
            String idno,
            String name,
            BigDecimal repayAmount
    ) {
    }

    private record RepayQueryForwardData(
            String userId,
            Integer loanId,
            String swiftNumber
    ) {
    }

    private record MemberProfile(
            String mobile,
            String idCardNo,
            String realName
    ) {
    }
}
