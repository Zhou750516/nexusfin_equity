package com.nexusfin.equity.thirdparty.yunka;

public record CardSmsSendRequest(
        String userId,
        Integer loanId,
        Integer type,
        String bankCardNum,
        String phone,
        String idno,
        String name
) {
}
