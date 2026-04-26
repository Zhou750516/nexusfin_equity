package com.nexusfin.equity.service;

import com.nexusfin.equity.dto.request.LoanResultCallbackRequest;
import com.nexusfin.equity.dto.response.PaymentStatusResponse;
import com.nexusfin.equity.entity.BenefitOrder;

public interface FallbackDeductService {

    PaymentStatusResponse triggerFallback(BenefitOrder order, LoanResultCallbackRequest request);
}
