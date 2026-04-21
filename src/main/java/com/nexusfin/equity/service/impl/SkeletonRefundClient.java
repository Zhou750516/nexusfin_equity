package com.nexusfin.equity.service.impl;

import com.nexusfin.equity.dto.response.RefundApplyResponse;
import com.nexusfin.equity.dto.response.RefundResultResponse;
import com.nexusfin.equity.service.RefundClient;
import org.springframework.stereotype.Service;

@Service
public class SkeletonRefundClient implements RefundClient {

    @Override
    public RefundApplyResponse apply(RefundApplyCommand command) {
        return new RefundApplyResponse(
                "REFUND-" + command.benefitOrderNo(),
                "processing",
                "refund submitted"
        );
    }

    @Override
    public RefundResultResponse getResult(String refundId) {
        return new RefundResultResponse(
                refundId,
                "processing",
                "refund still processing"
        );
    }
}
