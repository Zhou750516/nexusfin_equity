package com.nexusfin.equity.dto.response;

public record BankAccountResponse(
        String bankName,
        String lastFour,
        String accountId
) {
}
