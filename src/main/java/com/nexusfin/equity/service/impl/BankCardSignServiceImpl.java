package com.nexusfin.equity.service.impl;

import com.nexusfin.equity.config.QwProperties;
import com.nexusfin.equity.dto.request.BankCardSignApplyRequest;
import com.nexusfin.equity.dto.request.BankCardSignConfirmRequest;
import com.nexusfin.equity.dto.response.BankCardSignApplyResponse;
import com.nexusfin.equity.dto.response.BankCardSignConfirmResponse;
import com.nexusfin.equity.dto.response.BankCardSignStatusResponse;
import com.nexusfin.equity.entity.MemberChannel;
import com.nexusfin.equity.entity.MemberInfo;
import com.nexusfin.equity.exception.BizException;
import com.nexusfin.equity.exception.UpstreamTimeoutException;
import com.nexusfin.equity.repository.MemberChannelRepository;
import com.nexusfin.equity.repository.MemberInfoRepository;
import com.nexusfin.equity.repository.MemberPaymentProtocolRepository;
import com.nexusfin.equity.service.BankCardSignService;
import com.nexusfin.equity.service.PaymentProtocolService;
import com.nexusfin.equity.thirdparty.qw.QwBenefitClient;
import com.nexusfin.equity.thirdparty.qw.QwSignApplyRequest;
import com.nexusfin.equity.thirdparty.qw.QwSignApplyResponse;
import com.nexusfin.equity.thirdparty.qw.QwSignConfirmRequest;
import com.nexusfin.equity.thirdparty.qw.QwSignConfirmResponse;
import com.nexusfin.equity.thirdparty.qw.QwSignStatusRequest;
import com.nexusfin.equity.thirdparty.qw.QwSignStatusResponse;
import com.nexusfin.equity.util.ErrorLogFields;
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
    private static final String QW_SIGN_UPSTREAM_FAILED = "QW_SIGN_UPSTREAM_FAILED";
    private static final String QW_SIGN_UPSTREAM_TIMEOUT = "QW_SIGN_UPSTREAM_TIMEOUT";
    private static final String QW_SIGN_MERCHANT_ID_MISSING = "QW_SIGN_MERCHANT_ID_MISSING";

    private final MemberInfoRepository memberInfoRepository;
    private final MemberChannelRepository memberChannelRepository;
    private final MemberPaymentProtocolRepository memberPaymentProtocolRepository;
    private final SensitiveDataCipher sensitiveDataCipher;
    private final QwBenefitClient qwBenefitClient;
    private final PaymentProtocolService paymentProtocolService;
    private final QwProperties qwProperties;

    public BankCardSignServiceImpl(
            MemberInfoRepository memberInfoRepository,
            MemberChannelRepository memberChannelRepository,
            MemberPaymentProtocolRepository memberPaymentProtocolRepository,
            SensitiveDataCipher sensitiveDataCipher,
            QwBenefitClient qwBenefitClient,
            PaymentProtocolService paymentProtocolService,
            QwProperties qwProperties
    ) {
        this.memberInfoRepository = memberInfoRepository;
        this.memberChannelRepository = memberChannelRepository;
        this.memberPaymentProtocolRepository = memberPaymentProtocolRepository;
        this.sensitiveDataCipher = sensitiveDataCipher;
        this.qwBenefitClient = qwBenefitClient;
        this.paymentProtocolService = paymentProtocolService;
        this.qwProperties = qwProperties;
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
                    resolveMerchantId(),
                    decryptRequired(memberInfo.getMobileEncrypted(), "MEMBER_MOBILE_MISSING", "Member mobile is missing"),
                    decryptRequired(memberInfo.getRealNameEncrypted(), "MEMBER_NAME_MISSING", "Member real name is missing"),
                    accountNo
            ));
            boolean signed = response != null && Integer.valueOf(1).equals(response.status());
            String status = signed ? STATUS_SIGNED : STATUS_UNSIGNED;
            log.info("traceId={} bizOrderNo={} requestId={} memberId={} elapsedMs={} status={} bank-card sign status qw request success",
                    TraceIdUtil.getTraceId(), bizOrderNo, requestId, memberId, elapsedMs(startNanos), status);
            return new BankCardSignStatusResponse(accountNo, signed, status);
        } catch (UpstreamTimeoutException exception) {
            logFailure("bank-card sign status qw request failed", bizOrderNo, requestId, memberId, startNanos, exception);
            throw new BizException(QW_SIGN_UPSTREAM_TIMEOUT, "QW sign status temporarily unavailable");
        } catch (BizException exception) {
            if (isQwUserUnsigned(exception)) {
                log.info("traceId={} bizOrderNo={} requestId={} memberId={} elapsedMs={} status={} bank-card sign status qw request unsigned",
                        TraceIdUtil.getTraceId(), bizOrderNo, requestId, memberId, elapsedMs(startNanos), STATUS_UNSIGNED);
                return new BankCardSignStatusResponse(accountNo, false, STATUS_UNSIGNED);
            }
            logFailure("bank-card sign status qw request failed", bizOrderNo, requestId, memberId, startNanos, exception);
            throw exception;
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
                    resolveMerchantId(),
                    decryptRequired(memberInfo.getMobileEncrypted(), "MEMBER_MOBILE_MISSING", "Member mobile is missing"),
                    decryptRequired(memberInfo.getRealNameEncrypted(), "MEMBER_NAME_MISSING", "Member real name is missing"),
                    request.accountNo(),
                    decryptRequired(memberInfo.getIdCardEncrypted(), "MEMBER_ID_CARD_MISSING", "Member id card is missing")
            ));
            Long userSignId = response == null ? null : response.userSignId();
            String applyTime = response == null ? null : response.applyTime();
            if (userSignId == null || applyTime == null || applyTime.isBlank()) {
                throw new BizException("QW_SIGN_APPLY_INVALID", "QW sign apply response is invalid");
            }
            log.info("traceId={} bizOrderNo={} requestId={} userSignId={} memberId={} elapsedMs={} status={} bank-card sign apply qw request success",
                    TraceIdUtil.getTraceId(), bizOrderNo, requestId, userSignId, memberId, elapsedMs(startNanos), STATUS_SMS_SENT);
            return new BankCardSignApplyResponse(userSignId, applyTime, STATUS_SMS_SENT);
        } catch (UpstreamTimeoutException exception) {
            logFailure("bank-card sign apply qw request failed", bizOrderNo, requestId, memberId, startNanos, exception);
            throw new BizException(QW_SIGN_UPSTREAM_TIMEOUT, "QW sign apply temporarily unavailable");
        } catch (RuntimeException exception) {
            logFailure("bank-card sign apply qw request failed", bizOrderNo, requestId, memberId, startNanos, exception);
            throw exception;
        }
    }

    @Override
    public BankCardSignConfirmResponse confirmSign(String memberId, BankCardSignConfirmRequest request) {
        String requestId = RequestIdUtil.nextId("qwsignconfirm");
        String bizOrderNo = bankCardSignBizOrderNo(request.userSignId());
        long startNanos = System.nanoTime();
        log.info("traceId={} bizOrderNo={} requestId={} memberId={} bank-card sign confirm qw request begin",
                TraceIdUtil.getTraceId(), bizOrderNo, requestId, memberId);
        try {
            MemberInfo memberInfo = resolveMember(memberId);
            var currentProtocol = memberPaymentProtocolRepository.selectActiveByMemberId(memberInfo.getMemberId(), PROVIDER_QW_SIGN);
            if (currentProtocol != null
                    && request.userSignId() != null
                    && String.valueOf(request.userSignId()).equals(currentProtocol.getSignRequestNo())
                    && currentProtocol.getProtocolNo() != null
                    && !currentProtocol.getProtocolNo().isBlank()) {
                log.info("traceId={} bizOrderNo={} requestId={} userSignId={} agreementNo={} memberId={} elapsedMs={} status={} bank-card sign confirm duplicated, reused local active protocol",
                        TraceIdUtil.getTraceId(),
                        bizOrderNo,
                        requestId,
                        request.userSignId(),
                        currentProtocol.getProtocolNo(),
                        memberId,
                        elapsedMs(startNanos),
                        STATUS_SIGNED);
                return new BankCardSignConfirmResponse(
                        request.userSignId(),
                        currentProtocol.getProtocolNo(),
                        true,
                        STATUS_SIGNED
                );
            }
            QwSignConfirmResponse response = qwBenefitClient.confirmSign(new QwSignConfirmRequest(
                    request.userSignId(),
                    request.verificationCode()
            ));
            Long userSignId = response == null ? null : response.userSignId();
            String agreementNo = response == null ? null : response.agreementNo();
            if (userSignId == null || agreementNo == null || agreementNo.isBlank()) {
                throw new BizException("QW_SIGN_CONFIRM_FAILED", "QW sign confirm failed");
            }
            MemberChannel memberChannel = memberChannelRepository.selectLatestByMemberId(memberId);
            paymentProtocolService.saveActiveProtocol(new PaymentProtocolService.SavePaymentProtocolCommand(
                    memberInfo.getMemberId(),
                    firstNonBlank(memberInfo.getExternalUserId(), memberInfo.getTechPlatformUserId()),
                    PROVIDER_QW_SIGN,
                    agreementNo,
                    String.valueOf(userSignId),
                    memberChannel == null ? null : memberChannel.getChannelCode(),
                    LocalDateTime.now()
            ));
            log.info("traceId={} bizOrderNo={} requestId={} userSignId={} agreementNo={} memberId={} elapsedMs={} status={} bank-card sign confirm qw request success",
                    TraceIdUtil.getTraceId(), bizOrderNo, requestId, userSignId, agreementNo, memberId, elapsedMs(startNanos), STATUS_SIGNED);
            return new BankCardSignConfirmResponse(userSignId, agreementNo, true, STATUS_SIGNED);
        } catch (UpstreamTimeoutException exception) {
            logFailure("bank-card sign confirm qw request failed", bizOrderNo, requestId, memberId, startNanos, exception);
            throw new BizException(QW_SIGN_UPSTREAM_TIMEOUT, "QW sign confirm temporarily unavailable");
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
                ErrorLogFields.errorNo(exception, QW_SIGN_UPSTREAM_FAILED),
                ErrorLogFields.errorMsg(exception, "QW sign upstream failed"),
                message);
    }

    private boolean isQwUserUnsigned(BizException exception) {
        return "QW_UPSTREAM_REJECTED".equals(exception.getErrorNo())
                && firstNonBlank(exception.getErrorMsg(), exception.getMessage()).contains("用户未签约");
    }

    private static String bankCardBizOrderNo(String accountNo) {
        return "bank-card-" + lastFour(accountNo);
    }

    private static String bankCardSignBizOrderNo(Long userSignId) {
        return userSignId == null ? "bank-card-sign-UNKNOWN" : "bank-card-sign-" + userSignId;
    }

    private String resolveMerchantId() {
        String merchantId = qwProperties.getDirect() == null ? null : qwProperties.getDirect().getMerchantId();
        if (qwProperties.getMode() != QwProperties.Mode.MOCK && (merchantId == null || merchantId.isBlank())) {
            throw new BizException(QW_SIGN_MERCHANT_ID_MISSING, "QW sign merchantId is required outside MOCK mode");
        }
        return merchantId;
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
