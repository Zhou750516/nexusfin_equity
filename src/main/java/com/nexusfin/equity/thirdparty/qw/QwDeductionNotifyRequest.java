package com.nexusfin.equity.thirdparty.qw;

public record QwDeductionNotifyRequest(
        String uniqueId,
        String partnerOrderNo,
        String serialNo,
        Integer status,
        Long userSignId
) {
}
