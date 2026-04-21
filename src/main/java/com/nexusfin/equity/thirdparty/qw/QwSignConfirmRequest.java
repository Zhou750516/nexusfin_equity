package com.nexusfin.equity.thirdparty.qw;

public record QwSignConfirmRequest(
        String phone,
        String name,
        String accountNo,
        String idNo,
        String verCode
) {
}
