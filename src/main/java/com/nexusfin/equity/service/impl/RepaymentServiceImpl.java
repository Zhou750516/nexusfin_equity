package com.nexusfin.equity.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.nexusfin.equity.config.H5LoanProperties;
import com.nexusfin.equity.config.YunkaProperties;
import com.nexusfin.equity.dto.request.RepaymentSubmitRequest;
import com.nexusfin.equity.dto.response.BankAccountResponse;
import com.nexusfin.equity.dto.response.RepaymentInfoResponse;
import com.nexusfin.equity.dto.response.RepaymentResultResponse;
import com.nexusfin.equity.dto.response.RepaymentSubmitResponse;
import com.nexusfin.equity.exception.BizException;
import com.nexusfin.equity.util.TraceIdUtil;
import com.nexusfin.equity.service.H5I18nService;
import com.nexusfin.equity.service.RepaymentService;
import com.nexusfin.equity.thirdparty.yunka.YunkaGatewayClient;
import java.math.BigDecimal;
import java.math.RoundingMode;
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

    private final H5LoanProperties h5LoanProperties;
    private final YunkaProperties yunkaProperties;
    private final YunkaGatewayClient yunkaGatewayClient;
    private final H5I18nService h5I18nService;

    public RepaymentServiceImpl(
            H5LoanProperties h5LoanProperties,
            YunkaProperties yunkaProperties,
            YunkaGatewayClient yunkaGatewayClient,
            H5I18nService h5I18nService
    ) {
        this.h5LoanProperties = h5LoanProperties;
        this.yunkaProperties = yunkaProperties;
        this.yunkaGatewayClient = yunkaGatewayClient;
        this.h5I18nService = h5I18nService;
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
        return new RepaymentInfoResponse(
                loanId,
                centsToYuan(data.path("repayAmount").asLong()),
                h5I18nService.text("repayment.type.early", "提前还款"),
                bankAccount(),
                h5I18nService.text(
                        "repayment.tip.info",
                        "还款后将立即生效，剩余期数对应的利息将不再收取。请确认银行卡余额充足。"
                )
        );
    }

    @Override
    public RepaymentSubmitResponse submit(String uid, RepaymentSubmitRequest request) {
        String requestId = "RS-" + newCompactUuid();
        YunkaGatewayClient.YunkaGatewayRequest gatewayRequest = new YunkaGatewayClient.YunkaGatewayRequest(
                requestId,
                yunkaProperties.paths().repayApply(),
                request.loanId(),
                new RepayApplyForwardData(
                        uid,
                        request.loanId(),
                        mapRepayType(request.repaymentType()),
                        List.of(),
                        request.bankCardId(),
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
        String swiftNumber = data.path("swiftNumber").asText();
        return new RepaymentSubmitResponse(
                swiftNumber,
                mapSubmitStatus(data.path("status").asText()),
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
        return new RepaymentResultResponse(
                repaymentId,
                mapResultStatus(data.path("status").asText()),
                centsToYuan(data.path("amount").asLong()),
                data.path("successTime").asText(),
                bankAccount(),
                BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY),
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

    private BankAccountResponse bankAccount() {
        H5LoanProperties.ReceivingAccount account = h5LoanProperties.receivingAccount();
        return new BankAccountResponse(
                h5I18nService.text("loan.receivingAccount.bankName", account.bankName()),
                account.lastFour(),
                account.accountId()
        );
    }

    private String mapRepayType(String repaymentType) {
        if ("scheduled".equalsIgnoreCase(repaymentType)) {
            return "SCHEDULED";
        }
        return DEFAULT_REPAY_TYPE;
    }

    private String mapSubmitStatus(String status) {
        if ("FAILED".equalsIgnoreCase(status) || "FAIL".equalsIgnoreCase(status)) {
            return "failed";
        }
        return "processing";
    }

    private String mapResultStatus(String status) {
        if ("SUCCESS".equalsIgnoreCase(status) || "9001".equals(status)) {
            return "success";
        }
        return "failed";
    }

    private String readRemark(JsonNode data, String fallback) {
        String remark = data.path("remark").asText();
        return remark.isBlank() ? fallback : remark;
    }

    private List<String> repaymentTips() {
        return List.of(
                h5I18nService.text("repayment.tip.0", "还款金额已从您的银行卡扣除，请注意查收银行通知"),
                h5I18nService.text("repayment.tip.1", "提前还款后，您的信用额度将即时恢复"),
                h5I18nService.text("repayment.tip.2", "如需查看还款记录，可前往\"我的\"-\"账单明细\""),
                h5I18nService.text("repayment.tip.3", "若有任何疑问，请联系客服：400-888-8888")
        );
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
}
