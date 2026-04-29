package com.nexusfin.equity.service.impl;

import com.nexusfin.equity.entity.BenefitOrder;
import com.nexusfin.equity.entity.MemberPaymentProtocol;
import com.nexusfin.equity.exception.BizException;
import com.nexusfin.equity.repository.MemberPaymentProtocolRepository;
import com.nexusfin.equity.service.PaymentProtocolService;
import com.nexusfin.equity.util.TraceIdUtil;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentProtocolServiceImpl implements PaymentProtocolService {

    private static final Logger log = LoggerFactory.getLogger(PaymentProtocolServiceImpl.class);
    private static final String PROVIDER_QW_SIGN = "QW_SIGN";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String SOURCE_QW_SIGN = "QW_SIGN";

    private final MemberPaymentProtocolRepository memberPaymentProtocolRepository;

    public PaymentProtocolServiceImpl(MemberPaymentProtocolRepository memberPaymentProtocolRepository) {
        this.memberPaymentProtocolRepository = memberPaymentProtocolRepository;
    }

    @Override
    @Transactional
    public void saveActiveProtocol(SavePaymentProtocolCommand command) {
        MemberPaymentProtocol existing = memberPaymentProtocolRepository.selectActiveByMemberId(
                command.memberId(),
                command.providerCode()
        );
        LocalDateTime now = LocalDateTime.now();
        if (existing == null) {
            MemberPaymentProtocol protocol = new MemberPaymentProtocol();
            protocol.setMemberId(command.memberId());
            protocol.setExternalUserId(command.externalUserId());
            protocol.setProviderCode(command.providerCode());
            protocol.setProtocolNo(command.protocolNo());
            protocol.setProtocolStatus(STATUS_ACTIVE);
            protocol.setSignRequestNo(command.signRequestNo());
            protocol.setChannelCode(command.channelCode());
            protocol.setSignedTs(command.signedTs());
            protocol.setLastVerifiedTs(now);
            protocol.setCreatedTs(now);
            protocol.setUpdatedTs(now);
            memberPaymentProtocolRepository.insert(protocol);
            return;
        }
        existing.setExternalUserId(command.externalUserId());
        existing.setProviderCode(command.providerCode());
        existing.setProtocolNo(command.protocolNo());
        existing.setProtocolStatus(STATUS_ACTIVE);
        existing.setSignRequestNo(command.signRequestNo());
        existing.setChannelCode(command.channelCode());
        existing.setSignedTs(command.signedTs());
        existing.setLastVerifiedTs(now);
        existing.setUpdatedTs(now);
        memberPaymentProtocolRepository.updateById(existing);
    }

    @Override
    public ResolvedPaymentProtocol resolveForBenefitOrder(BenefitOrder order) {
        MemberPaymentProtocol activeProtocol = memberPaymentProtocolRepository.selectActiveByMemberId(
                order.getMemberId(),
                PROVIDER_QW_SIGN
        );
        if (activeProtocol == null && order.getExternalUserId() != null && !order.getExternalUserId().isBlank()) {
            activeProtocol = memberPaymentProtocolRepository.selectActiveByExternalUserId(
                    order.getExternalUserId(),
                    PROVIDER_QW_SIGN
            );
        }
        if (activeProtocol != null
                && activeProtocol.getProtocolNo() != null
                && !activeProtocol.getProtocolNo().isBlank()
                && activeProtocol.getSignRequestNo() != null
                && !activeProtocol.getSignRequestNo().isBlank()) {
            log.info("traceId={} bizOrderNo={} resolved active qw sign reference from {}",
                    TraceIdUtil.getTraceId(),
                    order.getBenefitOrderNo(),
                    SOURCE_QW_SIGN);
            return new ResolvedPaymentProtocol(
                    activeProtocol.getProtocolNo(),
                    activeProtocol.getSignRequestNo(),
                    SOURCE_QW_SIGN
            );
        }
        log.warn("traceId={} bizOrderNo={} active qw sign reference missing",
                TraceIdUtil.getTraceId(),
                order.getBenefitOrderNo());
        throw new BizException("QW_SIGN_REQUIRED", "QW sign confirmation required before benefit order");
    }
}
