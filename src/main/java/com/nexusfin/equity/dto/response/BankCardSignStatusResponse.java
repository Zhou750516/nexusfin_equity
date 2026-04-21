package com.nexusfin.equity.dto.response;

public record BankCardSignStatusResponse(
        String accountNo,
        boolean signed,
        String status
) {
}
