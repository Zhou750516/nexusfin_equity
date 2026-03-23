package com.nexusfin.equity.service;

import com.nexusfin.equity.dto.response.SignTaskResponse;
import com.nexusfin.equity.entity.BenefitOrder;

public interface AgreementService {

    SignTaskResponse ensureAgreementArtifacts(BenefitOrder order);
}
