package com.nexusfin.equity.dto.response;

public record BankCardSignConfirmResponse(
        String requestNo,
        boolean signed,
        String status
) {
}
