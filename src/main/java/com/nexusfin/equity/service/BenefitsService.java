package com.nexusfin.equity.service;

import com.nexusfin.equity.dto.request.BenefitsActivateRequest;
import com.nexusfin.equity.dto.response.BenefitsActivateResponse;
import com.nexusfin.equity.dto.response.BenefitsCardDetailResponse;

public interface BenefitsService {

    BenefitsCardDetailResponse getCardDetail(String memberId, String uid);

    BenefitsActivateResponse activate(String memberId, String uid, BenefitsActivateRequest request);
}
