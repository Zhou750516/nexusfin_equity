package com.nexusfin.equity.thirdparty.qw;

public record QwMemberSyncResponse(
        String orderNo,
        String cardNo,
        String timestamp,
        Integer openFlag,
        String productCode,
        String productName,
        String productType,
        String cardCreatedDate,
        String cardExpiryDate
) {
}
