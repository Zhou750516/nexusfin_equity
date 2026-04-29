package com.nexusfin.equity.thirdparty.qw;

public record QwSignConfirmRequest(
        Long userSignId,
        String verCode
) {
}
