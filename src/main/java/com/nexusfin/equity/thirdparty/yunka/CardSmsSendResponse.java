package com.nexusfin.equity.thirdparty.yunka;

public record CardSmsSendResponse(
        String smsSeq,
        String status,
        String message
) {
}
