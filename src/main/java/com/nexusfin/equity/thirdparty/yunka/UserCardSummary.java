package com.nexusfin.equity.thirdparty.yunka;

public record UserCardSummary(
        String cardId,
        String bankName,
        String cardLastFour,
        Integer isDefault
) {
}
