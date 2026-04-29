package com.nexusfin.equity.thirdparty.qw;

public record QwSignStatusRequest(
        String merchantId,
        String phone,
        String name,
        String accountNo
) {
}
