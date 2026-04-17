package com.nexusfin.equity.dto.response;

public record RefundResultResponse(
        String refundId,
        String status,
        String message
) {
}
