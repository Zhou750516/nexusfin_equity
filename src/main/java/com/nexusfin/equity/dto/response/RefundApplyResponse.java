package com.nexusfin.equity.dto.response;

public record RefundApplyResponse(
        String refundId,
        String status,
        String message
) {
}
