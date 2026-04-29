package com.nexusfin.equity.dto.response;

public record BankCardSignApplyResponse(
        Long userSignId,
        String applyTime,
        String status
) {
}
