package com.nexusfin.equity.dto.response;

public record RepaymentSubmitResponse(
        String repaymentId,
        String status,
        String message
) {
}
