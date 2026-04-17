package com.nexusfin.equity.service.impl;

import com.nexusfin.equity.exception.BizException;
import com.nexusfin.equity.service.TechPlatformBenefitStatusClient;
import org.springframework.stereotype.Service;

@Service
public class SkeletonTechPlatformBenefitStatusClient implements TechPlatformBenefitStatusClient {

    @Override
    public void push(BenefitStatusPushPayload payload) {
        throw new BizException("TECH_PLATFORM_STATUS_PUSH_NOT_READY", "Tech platform status push contract is not configured");
    }
}
