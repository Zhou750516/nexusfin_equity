package com.nexusfin.equity.dto.response;

public record BankCardSignStatusResponse(
        String accountNo,
        boolean signed,
        String status,
        Long userSignId,
        boolean canApplySign
) {

    public BankCardSignStatusResponse(String accountNo, boolean signed, String status) {
        this(accountNo, signed, status, null, !signed);
    }
}
