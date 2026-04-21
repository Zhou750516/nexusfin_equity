package com.nexusfin.equity.service.impl;

import com.nexusfin.equity.config.TechPlatformProperties;
import com.nexusfin.equity.exception.BizException;
import com.nexusfin.equity.service.TechPlatformBenefitStatusClient;
import org.springframework.stereotype.Service;

@Service
public class SkeletonTechPlatformBenefitStatusClient implements TechPlatformBenefitStatusClient {

    private final TechPlatformProperties properties;

    public SkeletonTechPlatformBenefitStatusClient(TechPlatformProperties properties) {
        this.properties = properties;
    }

    @Override
    public void push(BenefitStatusPushPayload payload) {
        if (!properties.isEnabled()) {
            throw new BizException("TECH_PLATFORM_DISABLED", "Tech platform integration is disabled");
        }
        if (properties.getMode() == TechPlatformProperties.Mode.MOCK) {
            return;
        }
        throw new BizException("TECH_PLATFORM_STATUS_PUSH_NOT_READY", "Tech platform status push contract is not configured");
    }
}
