package com.nexusfin.equity.dto.response;

public record RepaymentSmsSendResponse(
        String smsSeq,
        String status,
        String message
) {
}
