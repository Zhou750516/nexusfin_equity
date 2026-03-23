package com.nexusfin.equity.service;

import com.nexusfin.equity.dto.request.GrantForwardCallbackRequest;
import com.nexusfin.equity.dto.response.PaymentStatusResponse;
import com.nexusfin.equity.entity.BenefitOrder;

public interface FallbackDeductService {

    PaymentStatusResponse triggerFallback(BenefitOrder order, GrantForwardCallbackRequest request);
}
