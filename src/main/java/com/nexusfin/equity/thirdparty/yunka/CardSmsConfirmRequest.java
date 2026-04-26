package com.nexusfin.equity.thirdparty.yunka;

public record CardSmsConfirmRequest(
        String userId,
        String phone,
        Integer type,
        String loanId,
        String captcha
) {
}
