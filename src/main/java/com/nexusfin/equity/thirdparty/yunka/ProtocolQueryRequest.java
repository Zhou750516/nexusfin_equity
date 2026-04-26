package com.nexusfin.equity.thirdparty.yunka;

public record ProtocolQueryRequest(
        String userId,
        Long loanAmount,
        Integer loanPeriod
) {
}
