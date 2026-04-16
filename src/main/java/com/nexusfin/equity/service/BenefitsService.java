package com.nexusfin.equity.service;

import com.nexusfin.equity.dto.request.BenefitsActivateRequest;
import com.nexusfin.equity.dto.response.BenefitsActivateResponse;
import com.nexusfin.equity.dto.response.BenefitsCardDetailResponse;

public interface BenefitsService {

    BenefitsCardDetailResponse getCardDetail();

    BenefitsActivateResponse activate(String memberId, BenefitsActivateRequest request);
}
