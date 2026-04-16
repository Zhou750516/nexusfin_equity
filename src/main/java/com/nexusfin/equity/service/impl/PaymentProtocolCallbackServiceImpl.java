package com.nexusfin.equity.service.impl;

import com.nexusfin.equity.entity.MemberInfo;
import com.nexusfin.equity.exception.BizException;
import com.nexusfin.equity.repository.MemberInfoRepository;
import com.nexusfin.equity.service.IdempotencyService;
import com.nexusfin.equity.service.PaymentProtocolCallbackService;
import com.nexusfin.equity.service.PaymentProtocolService;
import com.nexusfin.equity.util.TraceIdUtil;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentProtocolCallbackServiceImpl implements PaymentProtocolCallbackService {

    private static final Logger log = LoggerFactory.getLogger(PaymentProtocolCallbackServiceImpl.class);
    private static final String DEFAULT_PROVIDER_CODE = "ALLINPAY";
    private static final String ACTIVE_STATUS = "ACTIVE";
    private static final DateTimeFormatter LEGACY_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final MemberInfoRepository memberInfoRepository;
    private final PaymentProtocolService paymentProtocolService;
    private final IdempotencyService idempotencyService;

    public PaymentProtocolCallbackServiceImpl(
            MemberInfoRepository memberInfoRepository,
            PaymentProtocolService paymentProtocolService,
            IdempotencyService idempotencyService
    ) {
        this.memberInfoRepository = memberInfoRepository;
        this.paymentProtocolService = paymentProtocolService;
        this.idempotencyService = idempotencyService;
    }

    @Override
    @Transactional
    public void handleCallback(PaymentProtocolCallbackCommand command) {
        if (idempotencyService.isProcessed(command.requestId())) {
            log.info("traceId={} bizOrderNo={} payment protocol callback duplicated, ignored",
                    TraceIdUtil.getTraceId(),
                    command.requestId());
            return;
        }
        MemberInfo memberInfo = resolveMember(command.memberId(), command.externalUserId());
        String protocolStatus = normalizeUpper(command.protocolStatus(), ACTIVE_STATUS);
        if (!ACTIVE_STATUS.equals(protocolStatus)) {
            throw new BizException("PAY_PROTOCOL_STATUS_UNSUPPORTED", "Only ACTIVE payment protocol callback is supported");
        }
        paymentProtocolService.saveActiveProtocol(new PaymentProtocolService.SavePaymentProtocolCommand(
                memberInfo.getMemberId(),
                firstNonBlank(command.externalUserId(), memberInfo.getExternalUserId()),
                normalizeUpper(command.providerCode(), DEFAULT_PROVIDER_CODE),
                command.protocolNo(),
                firstNonBlank(command.signRequestNo(), command.requestId()),
                command.channelCode(),
                parseSignedTs(command.signedTs())
        ));
        idempotencyService.markProcessed(
                command.requestId(),
                "PAYMENT_PROTOCOL_SYNC",
                command.protocolNo(),
                protocolStatus
        );
        log.info("traceId={} bizOrderNo={} payment protocol callback processed protocolNo={}",
                TraceIdUtil.getTraceId(),
                command.requestId(),
                command.protocolNo());
    }

    private MemberInfo resolveMember(String memberId, String externalUserId) {
        if ((memberId == null || memberId.isBlank()) && (externalUserId == null || externalUserId.isBlank())) {
            throw new BizException("MEMBER_IDENTIFIER_REQUIRED", "Member identifier is required");
        }
        if (memberId != null && !memberId.isBlank()) {
            MemberInfo memberInfo = memberInfoRepository.selectById(memberId);
            if (memberInfo == null) {
                throw new BizException("MEMBER_NOT_FOUND", "Member not found");
            }
            return memberInfo;
        }
        MemberInfo memberInfo = memberInfoRepository.selectByExternalUserId(externalUserId);
        if (memberInfo == null) {
            throw new BizException("MEMBER_NOT_FOUND", "Member not found");
        }
        return memberInfo;
    }

    private String normalizeUpper(String value, String fallback) {
        return firstNonBlank(value, fallback).toUpperCase(Locale.ROOT);
    }

    private String firstNonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private LocalDateTime parseSignedTs(String signedTs) {
        if (signedTs == null || signedTs.isBlank()) {
            return LocalDateTime.now();
        }
        try {
            return LocalDateTime.parse(signedTs);
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDateTime.parse(signedTs, LEGACY_DATE_TIME);
            } catch (DateTimeParseException exception) {
                throw new BizException("PAY_PROTOCOL_SIGNED_TS_INVALID", "Signed timestamp format is invalid");
            }
        }
    }
}
