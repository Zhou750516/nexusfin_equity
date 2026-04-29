package com.nexusfin.equity.thirdparty.qw;

public record QwSignApplyRequest(
        String merchantId,
        String phone,
        String name,
        String accountNo,
        String idNo
) {
}
