package com.nexusfin.equity.dto.response;

public record RefundInfoResponse(
        String benefitOrderNo,
        boolean refundable,
        String refundStatus,
        long refundableAmount,
        String message
) {
}
