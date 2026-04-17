package com.nexusfin.equity.service;

import com.nexusfin.equity.dto.response.RefundApplyResponse;
import com.nexusfin.equity.dto.response.RefundInfoResponse;
import com.nexusfin.equity.dto.response.RefundResultResponse;

public interface RefundService {

    RefundInfoResponse getInfo(String benefitOrderNo);

    RefundApplyResponse apply(String benefitOrderNo, String reason);

    RefundResultResponse getResult(String refundId);
}
