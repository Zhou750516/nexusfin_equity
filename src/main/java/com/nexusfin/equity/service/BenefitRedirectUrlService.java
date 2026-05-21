package com.nexusfin.equity.service;

import com.nexusfin.equity.dto.request.BenefitRedirectUrlRequest;

public interface BenefitRedirectUrlService {

    /**
     * Generates the current runtime benefit URL used by benefit-order sync.
     * <p>
     * The external contract name remains {@code redrect_benefit_url}, but the concrete runtime source
     * is the QW exercise redirect URL produced after a joint-login step. This satisfies the current
     * benefit sync payload contract and local runtime baseline, but should not be read as a generic
     * redirect URL for every future benefit scene.
     */
    BenefitRedirectUrlResult generate(BenefitRedirectUrlRequest request);

    BenefitRedirectUrlResult generateForMember(String memberId, String benefitOrderNo);

    record BenefitRedirectUrlResult(
            String redirectUrl
    ) {
    }
}
