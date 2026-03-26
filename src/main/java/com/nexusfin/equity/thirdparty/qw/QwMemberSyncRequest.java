package com.nexusfin.equity.thirdparty.qw;

public record QwMemberSyncRequest(
        String uniqueId,
        String partnerOrderNo,
        Long payAmount,
        String productCode,
        String productName,
        String mobile,
        String username,
        String payProtocolNo,
        String cardNo,
        Integer shareFlag,
        String partnerMerchantNo,
        Long partnerShareAmount,
        String shareMerchantNo,
        Long shareAmount
) {
}
