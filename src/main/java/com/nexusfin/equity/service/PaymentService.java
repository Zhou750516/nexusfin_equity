package com.nexusfin.equity.service;

import com.nexusfin.equity.dto.request.DeductionCallbackRequest;
import com.nexusfin.equity.dto.response.PaymentStatusResponse;

public interface PaymentService {

    PaymentStatusResponse handleFirstDeductCallback(DeductionCallbackRequest request);

    PaymentStatusResponse handleFallbackDeductCallback(DeductionCallbackRequest request);
}
