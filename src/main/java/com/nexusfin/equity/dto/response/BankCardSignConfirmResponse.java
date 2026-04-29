package com.nexusfin.equity.dto.response;

public record BankCardSignConfirmResponse(
        Long userSignId,
        String agreementNo,
        boolean signed,
        String status
) {
}
