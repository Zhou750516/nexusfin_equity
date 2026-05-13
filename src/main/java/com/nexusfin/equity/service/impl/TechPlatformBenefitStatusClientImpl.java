package com.nexusfin.equity.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusfin.equity.config.TechPlatformProperties;
import com.nexusfin.equity.service.TechPlatformBenefitStatusClient;
import com.nexusfin.equity.thirdparty.techplatform.BenefitOrderNoticeRequest;
import com.nexusfin.equity.thirdparty.techplatform.TechPlatformClient;
import com.nexusfin.equity.thirdparty.techplatform.TechPlatformClientImpl;
import com.nexusfin.equity.thirdparty.techplatform.TechPlatformNotifyResponse;
import com.nexusfin.equity.util.TraceIdUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TechPlatformBenefitStatusClientImpl implements TechPlatformBenefitStatusClient {

    private static final Logger log = LoggerFactory.getLogger(TechPlatformBenefitStatusClientImpl.class);

    private final TechPlatformClient techPlatformClient;
    private final String benefitOrderNoticePath;

    @Autowired
    public TechPlatformBenefitStatusClientImpl(TechPlatformProperties properties, ObjectMapper objectMapper) {
        this(new TechPlatformClientImpl(properties, objectMapper), properties.getPaths().getBenefitOrderNotice());
    }

    public TechPlatformBenefitStatusClientImpl(TechPlatformClient techPlatformClient, String benefitOrderNoticePath) {
        this.techPlatformClient = techPlatformClient;
        this.benefitOrderNoticePath = benefitOrderNoticePath;
    }

    @Override
    public void push(BenefitStatusPushPayload payload) {
        TechPlatformNotifyResponse response = techPlatformClient.notifyBenefitOrder(new BenefitOrderNoticeRequest(
                payload.eventId(),
                payload.benefitOrderNo(),
                payload.eventType(),
                payload.statusAfter(),
                payload.externalUserId(),
                payload.statusBefore()
        ));
        log.info("traceId={} techPlatformPath={} eventId={} bizOrderNo={} techPlatformCode={}",
                TraceIdUtil.getTraceId(),
                benefitOrderNoticePath,
                payload.eventId(),
                payload.benefitOrderNo(),
                response.code());
    }
}
