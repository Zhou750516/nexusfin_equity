package com.nexusfin.equity.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.nexusfin.equity.config.H5LoanProperties;
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
import com.nexusfin.equity.entity.MemberInfo;
import com.nexusfin.equity.exception.BizException;
import com.nexusfin.equity.repository.MemberInfoRepository;
import com.nexusfin.equity.service.H5I18nService;
import com.nexusfin.equity.service.RepaymentService;
import com.nexusfin.equity.service.XiaohuaGatewayService;
import com.nexusfin.equity.thirdparty.yunka.CardSmsConfirmRequest;
import com.nexusfin.equity.thirdparty.yunka.CardSmsSendRequest;
import com.nexusfin.equity.thirdparty.yunka.UserCardListRequest;
import com.nexusfin.equity.thirdparty.yunka.UserCardSummary;
import com.nexusfin.equity.thirdparty.yunka.YunkaGatewayClient;
import com.nexusfin.equity.util.SensitiveDataCipher;
import com.nexusfin.equity.util.TraceIdUtil;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RepaymentServiceImpl implements RepaymentService {

    private static final Logger log = LoggerFactory.getLogger(RepaymentServiceImpl.class);
    private static final BigDecimal CENTS_PER_YUAN = BigDecimal.valueOf(100L);
    private static final String DEFAULT_REPAY_TYPE = "EARLY";
    private static final int REPAYMENT_SMS_TYPE = 2;

    private final H5LoanProperties h5LoanProperties;
    private final YunkaProperties yunkaProperties;
    private final YunkaGatewayClient yunkaGatewayClient;
    private final H5I18nService h5I18nService;
    private final XiaohuaGatewayService xiaohuaGatewayService;
    private final MemberInfoRepository memberInfoRepository;
    private final SensitiveDataCipher sensitiveDataCipher;

    public RepaymentServiceImpl(
            H5LoanProperties h5LoanProperties,
            YunkaProperties yunkaProperties,
            YunkaGatewayClient yunkaGatewayClient,
            H5I18nService h5I18nService,
            XiaohuaGatewayService xiaohuaGatewayService,
            MemberInfoRepository memberInfoRepository,
            SensitiveDataCipher sensitiveDataCipher
    ) {
        this.h5LoanProperties = h5LoanProperties;
        this.yunkaProperties = yunkaProperties;
        this.yunkaGatewayClient = yunkaGatewayClient;
        this.h5I18nService = h5I18nService;
        this.xiaohuaGatewayService = xiaohuaGatewayService;
        this.memberInfoRepository = memberInfoRepository;
        this.sensitiveDataCipher = sensitiveDataCipher;
    }

    @Override
    public RepaymentInfoResponse getInfo(String uid, String loanId) {
        String requestId = "RT-" + newCompactUuid();
        YunkaGatewayClient.YunkaGatewayRequest gatewayRequest = new YunkaGatewayClient.YunkaGatewayRequest(
                requestId,
                yunkaProperties.paths().repayTrial(),
                loanId,
                new RepayTrialForwardData(uid, loanId, DEFAULT_REPAY_TYPE, List.of())
        );
        long startNanos = System.nanoTime();
        log.info("traceId={} bizOrderNo={} requestId={} path={} repayment info yunka request begin",
                TraceIdUtil.getTraceId(),
                loanId,
                requestId,
                gatewayRequest.path());
        JsonNode data;
        try {
            data = requireSuccessfulYunkaData(yunkaGatewayClient.proxy(gatewayRequest));
        } catch (BizException exception) {
            log.warn("traceId={} bizOrderNo={} requestId={} path={} elapsedMs={} errorNo={} errorMsg={}",
                    TraceIdUtil.getTraceId(),
                    loanId,
                    requestId,
                    gatewayRequest.path(),
                    elapsedMs(startNanos),
                    exception.getErrorNo(),
                    exception.getErrorMsg());
            throw exception;
        }
        log.info("traceId={} bizOrderNo={} requestId={} path={} elapsedMs={} repayment info yunka request success",
                TraceIdUtil.getTraceId(),
                loanId,
                requestId,
                gatewayRequest.path(),
                elapsedMs(startNanos));
        List<BankAccountResponse> bankCards = queryRepaymentCards(uid, loanId);
        BankAccountResponse selectedCard = bankCards.stream().findFirst().orElseGet(this::fallbackBankAccount);
        return new RepaymentInfoResponse(
                loanId,
                centsToYuan(readLong(data, "repayAmount", "amount")),
                h5I18nService.text("repayment.type.early", "提前还款"),
                selectedCard,
                bankCards,
                true,
                h5I18nService.text(
                        "repayment.tip.info",
                        "还款后将立即生效，剩余期数对应的利息将不再收取。请确认银行卡余额充足。"
                )
        );
    }

    @Override
    public RepaymentSmsSendResponse sendSms(String uid, RepaymentSmsSendRequest request) {
        MemberProfile memberProfile = loadMemberProfile(uid);
        String bankCardNum = resolveBankCardNumber(uid, request.loanId(), request.bankCardId());
        String requestId = "RSS-" + newCompactUuid();
        var response = xiaohuaGatewayService.sendCardSms(
                requestId,
                request.loanId(),
                new CardSmsSendRequest(
                        uid,
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
    public RepaymentSmsConfirmResponse confirmSms(String uid, RepaymentSmsConfirmRequest request) {
        MemberProfile memberProfile = loadMemberProfile(uid);
        String requestId = "RSC-" + newCompactUuid();
        var response = xiaohuaGatewayService.confirmCardSms(
                requestId,
                request.loanId(),
                new CardSmsConfirmRequest(
                        uid,
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
    public RepaymentSubmitResponse submit(String uid, RepaymentSubmitRequest request) {
        String requestId = "RS-" + newCompactUuid();
        String bankCardNum = resolveBankCardNumber(uid, request.loanId(), request.bankCardId());
        YunkaGatewayClient.YunkaGatewayRequest gatewayRequest = new YunkaGatewayClient.YunkaGatewayRequest(
                requestId,
                yunkaProperties.paths().repayApply(),
                request.loanId(),
                new RepayApplyForwardData(
                        uid,
                        request.loanId(),
                        mapRepayType(request.repaymentType()),
                        List.of(),
                        bankCardNum,
                        yuanToCent(request.amount())
                )
        );
        long startNanos = System.nanoTime();
        log.info("traceId={} bizOrderNo={} requestId={} path={} repayment submit yunka request begin",
                TraceIdUtil.getTraceId(),
                request.loanId(),
                requestId,
                gatewayRequest.path());
        JsonNode data;
        try {
            data = requireSuccessfulYunkaData(yunkaGatewayClient.proxy(gatewayRequest));
        } catch (BizException exception) {
            log.warn("traceId={} bizOrderNo={} requestId={} path={} elapsedMs={} errorNo={} errorMsg={}",
                    TraceIdUtil.getTraceId(),
                    request.loanId(),
                    requestId,
                    gatewayRequest.path(),
                    elapsedMs(startNanos),
                    exception.getErrorNo(),
                    exception.getErrorMsg());
            throw exception;
        }
        log.info("traceId={} bizOrderNo={} requestId={} path={} elapsedMs={} repayment submit yunka request success",
                TraceIdUtil.getTraceId(),
                request.loanId(),
                requestId,
                gatewayRequest.path(),
                elapsedMs(startNanos));
        String swiftNumber = readText(data, "swiftNumber", request.loanId());
        return new RepaymentSubmitResponse(
                swiftNumber,
                mapSubmitStatus(readText(data, "status", "")),
                readRemark(data, "还款请求已提交，正在处理中")
        );
    }

    @Override
    public RepaymentResultResponse getResult(String uid, String repaymentId) {
        String requestId = "RQ-" + newCompactUuid();
        YunkaGatewayClient.YunkaGatewayRequest gatewayRequest = new YunkaGatewayClient.YunkaGatewayRequest(
                requestId,
                yunkaProperties.paths().repayQuery(),
                repaymentId,
                new RepayQueryForwardData(uid, repaymentId)
        );
        long startNanos = System.nanoTime();
        log.info("traceId={} bizOrderNo={} requestId={} path={} repayment result yunka request begin",
                TraceIdUtil.getTraceId(),
                repaymentId,
                requestId,
                gatewayRequest.path());
        JsonNode data;
        try {
            data = requireSuccessfulYunkaData(yunkaGatewayClient.proxy(gatewayRequest));
        } catch (BizException exception) {
            log.warn("traceId={} bizOrderNo={} requestId={} path={} elapsedMs={} errorNo={} errorMsg={}",
                    TraceIdUtil.getTraceId(),
                    repaymentId,
                    requestId,
                    gatewayRequest.path(),
                    elapsedMs(startNanos),
                    exception.getErrorNo(),
                    exception.getErrorMsg());
            throw exception;
        }
        log.info("traceId={} bizOrderNo={} requestId={} path={} elapsedMs={} repayment result yunka request success",
                TraceIdUtil.getTraceId(),
                repaymentId,
                requestId,
                gatewayRequest.path(),
                elapsedMs(startNanos));
        String swiftNumber = readText(data, "swiftNumber", repaymentId);
        BankAccountResponse bankCard = resolveResultBankCard(uid, swiftNumber, data);
        return new RepaymentResultResponse(
                repaymentId,
                swiftNumber,
                mapResultStatus(readText(data, "status", "")),
                centsToYuan(readLong(data, "amount", "repayAmount")),
                resolveRepaymentTime(data),
                bankCard,
                centsToYuan(readLong(data, "discount")),
                repaymentTips()
        );
    }

    private JsonNode requireSuccessfulYunkaData(YunkaGatewayClient.YunkaGatewayResponse response) {
        if (response == null || response.code() != 0) {
            String message = response == null ? "Yunka gateway response is empty" : response.message();
            throw new BizException(502, message);
        }
        return response.data();
    }

    private List<BankAccountResponse> queryRepaymentCards(String uid, String bizOrderNo) {
        try {
            var response = xiaohuaGatewayService.queryUserCards(
                    "RUC-" + newCompactUuid(),
                    bizOrderNo,
                    new UserCardListRequest(uid)
            );
            if (response == null || response.cards() == null || response.cards().isEmpty()) {
                return List.of(fallbackBankAccount());
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
            return List.of(fallbackBankAccount());
        }
    }

    private BankAccountResponse resolveResultBankCard(String uid, String bizOrderNo, JsonNode data) {
        String bankCardNum = readText(data, "bankCardNum", "");
        List<BankAccountResponse> bankCards = queryRepaymentCards(uid, bizOrderNo);
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
        return bankCards.stream().findFirst().orElseGet(this::fallbackBankAccount);
    }

    private BankAccountResponse toBankAccount(UserCardSummary card) {
        return new BankAccountResponse(
                defaultText(card.bankName(), h5I18nService.text("loan.receivingAccount.bankName", "银行卡")),
                defaultText(card.cardLastFour(), lastFour(card.cardId())),
                card.cardId()
        );
    }

    private BankAccountResponse fallbackBankAccount() {
        H5LoanProperties.ReceivingAccount account = h5LoanProperties.receivingAccount();
        return new BankAccountResponse(
                h5I18nService.text("loan.receivingAccount.bankName", account.bankName()),
                account.lastFour(),
                account.accountId()
        );
    }

    private String resolveBankCardNumber(String uid, String bizOrderNo, String bankCardId) {
        return queryRepaymentCards(uid, bizOrderNo).stream()
                .filter(card -> bankCardId.equals(card.accountId()))
                .findFirst()
                .map(BankAccountResponse::accountId)
                .orElse(bankCardId);
    }

    private MemberProfile loadMemberProfile(String uid) {
        MemberInfo memberInfo = memberInfoRepository.selectByTechPlatformUserId(uid);
        if (memberInfo == null) {
            throw new BizException(404, "member info not found");
        }
        return new MemberProfile(
                sensitiveDataCipher.decrypt(memberInfo.getMobileEncrypted()),
                sensitiveDataCipher.decrypt(memberInfo.getIdCardEncrypted()),
                sensitiveDataCipher.decrypt(memberInfo.getRealNameEncrypted())
        );
    }

    private String mapRepayType(String repaymentType) {
        if ("scheduled".equalsIgnoreCase(repaymentType)) {
            return "SCHEDULED";
        }
        return DEFAULT_REPAY_TYPE;
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
            long epochSeconds = successTime.asLong();
            if (epochSeconds <= 0) {
                return "";
            }
            return Instant.ofEpochSecond(epochSeconds)
                    .atOffset(ZoneOffset.ofHours(8))
                    .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        }
        return successTime.asText("");
    }

    private long readLong(JsonNode data, String... fields) {
        for (String field : fields) {
            JsonNode value = data.path(field);
            if (!value.isMissingNode() && !value.isNull()) {
                if (value.isNumber()) {
                    return value.asLong();
                }
                String text = value.asText();
                if (!text.isBlank()) {
                    try {
                        return new BigDecimal(text).setScale(0, RoundingMode.HALF_UP).longValue();
                    } catch (NumberFormatException ignored) {
                        return 0L;
                    }
                }
            }
        }
        return 0L;
    }

    private String readText(JsonNode data, String fieldName, String fallback) {
        String value = data.path(fieldName).asText();
        return value == null || value.isBlank() ? fallback : value;
    }

    private String readRemark(JsonNode data, String fallback) {
        return readText(data, "remark", fallback);
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

    private static String newCompactUuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private static Long yuanToCent(BigDecimal yuanAmount) {
        return yuanAmount.multiply(CENTS_PER_YUAN).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    private static BigDecimal centsToYuan(long cents) {
        return BigDecimal.valueOf(cents).divide(CENTS_PER_YUAN, 2, RoundingMode.UNNECESSARY);
    }

    private static long elapsedMs(long startNanos) {
        return java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
    }

    private record RepayTrialForwardData(
            String uid,
            String loanId,
            String repayType,
            List<Integer> periods
    ) {
    }

    private record RepayApplyForwardData(
            String uid,
            String loanId,
            String repayType,
            List<Integer> periods,
            String bankCardNo,
            Long repayAmount
    ) {
    }

    private record RepayQueryForwardData(
            String uid,
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
