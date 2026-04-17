package com.nexusfin.equity.service;

import com.nexusfin.equity.dto.response.BenefitDispatchContextResponse;
import com.nexusfin.equity.dto.response.BenefitDispatchResolveResponse;

public interface BenefitDispatchService {

    BenefitDispatchContextResponse getContext(String benefitOrderNo);

    BenefitDispatchResolveResponse resolve(String benefitOrderNo);
}
