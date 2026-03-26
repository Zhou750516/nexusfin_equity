package com.nexusfin.equity.thirdparty.qw;

public record QwLendingNotifyRequest(
        String uniqueId,
        String partnerOrderNo,
        Integer status
) {
}
