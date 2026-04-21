package com.nexusfin.equity.service.impl;

import com.nexusfin.equity.dto.request.BankCardSignApplyRequest;
import com.nexusfin.equity.dto.request.BankCardSignConfirmRequest;
import com.nexusfin.equity.dto.response.BankCardSignApplyResponse;
import com.nexusfin.equity.dto.response.BankCardSignConfirmResponse;
import com.nexusfin.equity.dto.response.BankCardSignStatusResponse;
import com.nexusfin.equity.entity.MemberChannel;
import com.nexusfin.equity.entity.MemberInfo;
import com.nexusfin.equity.exception.BizException;
import com.nexusfin.equity.repository.MemberChannelRepository;
import com.nexusfin.equity.repository.MemberInfoRepository;
import com.nexusfin.equity.service.BankCardSignService;
import com.nexusfin.equity.service.PaymentProtocolService;
import com.nexusfin.equity.thirdparty.qw.QwBenefitClient;
import com.nexusfin.equity.thirdparty.qw.QwSignApplyRequest;
import com.nexusfin.equity.thirdparty.qw.QwSignApplyResponse;
import com.nexusfin.equity.thirdparty.qw.QwSignConfirmRequest;
import com.nexusfin.equity.thirdparty.qw.QwSignConfirmResponse;
import com.nexusfin.equity.thirdparty.qw.QwSignStatusRequest;
import com.nexusfin.equity.thirdparty.qw.QwSignStatusResponse;
import com.nexusfin.equity.util.RequestIdUtil;
import com.nexusfin.equity.util.SensitiveDataCipher;
import com.nexusfin.equity.util.TraceIdUtil;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class BankCardSignServiceImpl implements BankCardSignService {

    private static final Logger log = LoggerFactory.getLogger(BankCardSignServiceImpl.class);
    private static final String STATUS_SIGNED = "SIGNED";
    private static final String STATUS_UNSIGNED = "UNSIGNED";
    private static final String STATUS_SMS_SENT = "SMS_SENT";
    private static final String PROVIDER_QW_SIGN = "QW_SIGN";
    private static final String PROTOCOL_STATUS_ACTIVE = "ACTIVE";
    private static final String QW_SIGN_UPSTREAM_FAILED = "QW_SIGN_UPSTREAM_FAILED";

    private final MemberInfoRepository memberInfoRepository;
    private final MemberChannelRepository memberChannelRepository;
    private final SensitiveDataCipher sensitiveDataCipher;
    private final QwBenefitClient qwBenefitClient;
    private final PaymentProtocolService paymentProtocolService;

    public BankCardSignServiceImpl(
            MemberInfoRepository memberInfoRepository,
            MemberChannelRepository memberChannelRepository,
            SensitiveDataCipher sensitiveDataCipher,
            QwBenefitClient qwBenefitClient,
            PaymentProtocolService paymentProtocolService
    ) {
        this.memberInfoRepository = memberInfoRepository;
        this.memberChannelRepository = memberChannelRepository;
        this.sensitiveDataCipher = sensitiveDataCipher;
        this.qwBenefitClient = qwBenefitClient;
        this.paymentProtocolService = paymentProtocolService;
    }

    @Override
    public BankCardSignStatusResponse getSignStatus(String memberId, String accountNo) {
        String requestId = RequestIdUtil.nextId("qwsignstatus");
        String bizOrderNo = bankCardBizOrderNo(accountNo);
        long startNanos = System.nanoTime();
        log.info("traceId={} bizOrderNo={} requestId={} memberId={} bank-card sign status qw request begin",
                TraceIdUtil.getTraceId(), bizOrderNo, requestId, memberId);
        try {
            MemberInfo memberInfo = resolveMember(memberId);
            QwSignStatusResponse response = qwBenefitClient.querySignStatus(new QwSignStatusRequest(
                    decryptRequired(memberInfo.getMobileEncrypted(), "MEMBER_MOBILE_MISSING", "Member mobile is missing"),
                    decryptRequired(memberInfo.getRealNameEncrypted(), "MEMBER_NAME_MISSING", "Member real name is missing"),
                    accountNo
            ));
            boolean signed = response != null && Integer.valueOf(1).equals(response.status());
            String status = signed ? STATUS_SIGNED : STATUS_UNSIGNED;
            log.info("traceId={} bizOrderNo={} requestId={} memberId={} elapsedMs={} status={} bank-card sign status qw request success",
                    TraceIdUtil.getTraceId(), bizOrderNo, requestId, memberId, elapsedMs(startNanos), status);
            return new BankCardSignStatusResponse(accountNo, signed, status);
        } catch (RuntimeException exception) {
            logFailure("bank-card sign status qw request failed", bizOrderNo, requestId, memberId, startNanos, exception);
            throw exception;
        }
    }

    @Override
    public BankCardSignApplyResponse applySign(String memberId, BankCardSignApplyRequest request) {
        String requestId = RequestIdUtil.nextId("qwsignapply");
        String bizOrderNo = bankCardBizOrderNo(request.accountNo());
        long startNanos = System.nanoTime();
        log.info("traceId={} bizOrderNo={} requestId={} memberId={} bank-card sign apply qw request begin",
                TraceIdUtil.getTraceId(), bizOrderNo, requestId, memberId);
        try {
            MemberInfo memberInfo = resolveMember(memberId);
            QwSignApplyResponse response = qwBenefitClient.applySign(new QwSignApplyRequest(
                    decryptRequired(memberInfo.getMobileEncrypted(), "MEMBER_MOBILE_MISSING", "Member mobile is missing"),
                    decryptRequired(memberInfo.getRealNameEncrypted(), "MEMBER_NAME_MISSING", "Member real name is missing"),
                    request.accountNo(),
                    decryptRequired(memberInfo.getIdCardEncrypted(), "MEMBER_ID_CARD_MISSING", "Member id card is missing")
            ));
            String requestNo = firstNonBlank(response == null ? null : response.requestNo(), requestId);
            log.info("traceId={} bizOrderNo={} requestId={} requestNo={} memberId={} elapsedMs={} status={} bank-card sign apply qw request success",
                    TraceIdUtil.getTraceId(), bizOrderNo, requestId, requestNo, memberId, elapsedMs(startNanos), STATUS_SMS_SENT);
            return new BankCardSignApplyResponse(requestNo, STATUS_SMS_SENT);
        } catch (RuntimeException exception) {
            logFailure("bank-card sign apply qw request failed", bizOrderNo, requestId, memberId, startNanos, exception);
            throw exception;
        }
    }

    @Override
    public BankCardSignConfirmResponse confirmSign(String memberId, BankCardSignConfirmRequest request) {
        String requestId = RequestIdUtil.nextId("qwsignconfirm");
        String bizOrderNo = bankCardBizOrderNo(request.accountNo());
        long startNanos = System.nanoTime();
        log.info("traceId={} bizOrderNo={} requestId={} memberId={} bank-card sign confirm qw request begin",
                TraceIdUtil.getTraceId(), bizOrderNo, requestId, memberId);
        try {
            MemberInfo memberInfo = resolveMember(memberId);
            QwSignConfirmResponse response = qwBenefitClient.confirmSign(new QwSignConfirmRequest(
                    decryptRequired(memberInfo.getMobileEncrypted(), "MEMBER_MOBILE_MISSING", "Member mobile is missing"),
                    decryptRequired(memberInfo.getRealNameEncrypted(), "MEMBER_NAME_MISSING", "Member real name is missing"),
                    request.accountNo(),
                    decryptRequired(memberInfo.getIdCardEncrypted(), "MEMBER_ID_CARD_MISSING", "Member id card is missing"),
                    request.verificationCode()
            ));
            String requestNo = firstNonBlank(response == null ? null : response.requestNo(), requestId);
            String protocolStatus = firstNonBlank(response == null ? null : response.protocolStatus(), PROTOCOL_STATUS_ACTIVE);
            if (!PROTOCOL_STATUS_ACTIVE.equalsIgnoreCase(protocolStatus)) {
                throw new BizException("QW_SIGN_CONFIRM_FAILED", "QW sign confirm failed");
            }
            MemberChannel memberChannel = memberChannelRepository.selectLatestByMemberId(memberId);
            paymentProtocolService.saveActiveProtocol(new PaymentProtocolService.SavePaymentProtocolCommand(
                    memberInfo.getMemberId(),
                    firstNonBlank(memberInfo.getExternalUserId(), memberInfo.getTechPlatformUserId()),
                    PROVIDER_QW_SIGN,
                    "QW-SIGN-" + requestNo,
                    requestNo,
                    memberChannel == null ? null : memberChannel.getChannelCode(),
                    LocalDateTime.now()
            ));
            log.info("traceId={} bizOrderNo={} requestId={} requestNo={} memberId={} elapsedMs={} status={} bank-card sign confirm qw request success",
                    TraceIdUtil.getTraceId(), bizOrderNo, requestId, requestNo, memberId, elapsedMs(startNanos), STATUS_SIGNED);
            return new BankCardSignConfirmResponse(requestNo, true, STATUS_SIGNED);
        } catch (RuntimeException exception) {
            logFailure("bank-card sign confirm qw request failed", bizOrderNo, requestId, memberId, startNanos, exception);
            throw exception;
        }
    }

    private MemberInfo resolveMember(String memberId) {
        MemberInfo memberInfo = memberInfoRepository.selectById(memberId);
        if (memberInfo == null) {
            throw new BizException("MEMBER_NOT_FOUND", "Member not found");
        }
        return memberInfo;
    }

    private String decryptRequired(String ciphertext, String code, String message) {
        if (ciphertext == null || ciphertext.isBlank()) {
            throw new BizException(code, message);
        }
        String plaintext = sensitiveDataCipher.decrypt(ciphertext);
        if (plaintext == null || plaintext.isBlank()) {
            throw new BizException(code, message);
        }
        return plaintext;
    }

    private String firstNonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private void logFailure(
            String message,
            String bizOrderNo,
            String requestId,
            String memberId,
            long startNanos,
            RuntimeException exception
    ) {
        log.warn("traceId={} bizOrderNo={} requestId={} memberId={} elapsedMs={} errorNo={} errorMsg={} {}",
                TraceIdUtil.getTraceId(),
                bizOrderNo,
                requestId,
                memberId,
                elapsedMs(startNanos),
                errorNo(exception),
                errorMsg(exception),
                message);
    }

    private String errorNo(RuntimeException exception) {
        if (exception instanceof BizException bizException) {
            return bizException.getErrorNo();
        }
        return QW_SIGN_UPSTREAM_FAILED;
    }

    private String errorMsg(RuntimeException exception) {
        if (exception instanceof BizException bizException) {
            return bizException.getErrorMsg();
        }
        return firstNonBlank(exception.getMessage(), "QW sign upstream failed");
    }

    private static String bankCardBizOrderNo(String accountNo) {
        return "bank-card-" + lastFour(accountNo);
    }

    private static String lastFour(String accountNo) {
        if (accountNo == null || accountNo.isBlank()) {
            return "UNKNOWN";
        }
        return accountNo.length() <= 4 ? accountNo : accountNo.substring(accountNo.length() - 4);
    }

    private static long elapsedMs(long startNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
    }
}
