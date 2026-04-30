package com.nexusfin.equity.service;

import com.nexusfin.equity.dto.request.BenefitRedirectUrlRequest;

public interface BenefitRedirectUrlService {

    BenefitRedirectUrlResult generate(BenefitRedirectUrlRequest request);

    record BenefitRedirectUrlResult(
            String redirectUrl
    ) {
    }
}
