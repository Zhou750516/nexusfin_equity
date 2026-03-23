package com.nexusfin.equity.dto.response;

public record PaymentStatusResponse(
        String paymentNo,
        String benefitOrderNo,
        String paymentType,
        String paymentStatus,
        String failReason
) {
}
