package com.nexusfin.equity.thirdparty.qw;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record QwMemberSyncRequest(
        String uniqueId,
        String partnerOrderNo,
        Long payAmount,
        String productCode,
        String productName,
        Long userSignId,
        String cardNo,
        Integer shareFlag,
        String partnerMerchantNo,
        Long partnerShareAmount,
        String shareMerchantNo,
        Long shareAmount
) {
}
