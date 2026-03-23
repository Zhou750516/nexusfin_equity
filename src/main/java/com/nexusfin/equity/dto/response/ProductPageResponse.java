package com.nexusfin.equity.dto.response;

import java.util.List;

public record ProductPageResponse(
        String productCode,
        String productName,
        Integer feeRate,
        Long feeAmount,
        Long loanAmount,
        List<String> agreements,
        String memberId,
        String externalUserId
) {
}
