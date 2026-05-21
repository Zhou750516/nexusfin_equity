package com.nexusfin.equity.thirdparty.yunka;

public record CardSmsConfirmRequest(
        String userId,
        String phone,
        Integer type,
        Integer loanId,
        String captcha
) {
}
