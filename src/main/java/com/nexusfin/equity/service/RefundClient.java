package com.nexusfin.equity.service;

import com.nexusfin.equity.dto.response.RefundApplyResponse;
import com.nexusfin.equity.dto.response.RefundResultResponse;

public interface RefundClient {

    RefundApplyResponse apply(RefundApplyCommand command);

    RefundResultResponse getResult(String refundId);

    record RefundApplyCommand(
            String benefitOrderNo,
            String reason
    ) {
    }
}
