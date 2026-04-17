package com.nexusfin.equity.service.impl;

import com.nexusfin.equity.entity.BenefitStatusPushLog;
import com.nexusfin.equity.repository.BenefitStatusPushLogRepository;
import com.nexusfin.equity.service.BenefitStatusPushService;
import com.nexusfin.equity.service.TechPlatformBenefitStatusClient;
import com.nexusfin.equity.util.RequestIdUtil;
import com.nexusfin.equity.util.TraceIdUtil;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class BenefitStatusPushServiceImpl implements BenefitStatusPushService {

    private static final Logger log = LoggerFactory.getLogger(BenefitStatusPushServiceImpl.class);

    private final BenefitStatusPushLogRepository benefitStatusPushLogRepository;
    private final TechPlatformBenefitStatusClient techPlatformBenefitStatusClient;

    public BenefitStatusPushServiceImpl(
            BenefitStatusPushLogRepository benefitStatusPushLogRepository,
            TechPlatformBenefitStatusClient techPlatformBenefitStatusClient
    ) {
        this.benefitStatusPushLogRepository = benefitStatusPushLogRepository;
        this.techPlatformBenefitStatusClient = techPlatformBenefitStatusClient;
    }

    @Override
    public void pushStatus(BenefitStatusPushCommand command) {
        String eventId = RequestIdUtil.nextId("evt");
        LocalDateTime now = LocalDateTime.now();
        BenefitStatusPushLog pushLog = new BenefitStatusPushLog();
        pushLog.setEventId(eventId);
        pushLog.setBenefitOrderNo(command.benefitOrderNo());
        pushLog.setEventType(command.eventType());
        pushLog.setStatusBefore(command.statusBefore());
        pushLog.setStatusAfter(command.statusAfter());
        pushLog.setPushStatus("INIT");
        pushLog.setRetryCount(0);
        pushLog.setRequestPayload(command.eventType() + ":" + command.statusAfter());
        pushLog.setCreatedTs(now);
        pushLog.setUpdatedTs(now);
        benefitStatusPushLogRepository.insert(pushLog);

        log.info("traceId={} bizOrderNo={} benefit status push requested eventType={}",
                TraceIdUtil.getTraceId(), command.benefitOrderNo(), command.eventType());

        techPlatformBenefitStatusClient.push(new TechPlatformBenefitStatusClient.BenefitStatusPushPayload(
                eventId,
                command.benefitOrderNo(),
                command.eventType(),
                command.statusAfter(),
                command.externalUserId(),
                command.statusBefore()
        ));

        BenefitStatusPushLog successLog = new BenefitStatusPushLog();
        successLog.setEventId(eventId);
        successLog.setBenefitOrderNo(command.benefitOrderNo());
        successLog.setEventType(command.eventType());
        successLog.setStatusBefore(command.statusBefore());
        successLog.setStatusAfter(command.statusAfter());
        successLog.setPushStatus("SUCCESS");
        successLog.setRetryCount(0);
        successLog.setRequestPayload(command.eventType() + ":" + command.statusAfter());
        successLog.setResponsePayload("accepted");
        successLog.setCreatedTs(now);
        successLog.setUpdatedTs(LocalDateTime.now());
        benefitStatusPushLogRepository.updateById(successLog);
    }
}
