package com.nexusfin.equity.thirdparty.qw;

public record QwExerciseUrlResponse(
        Integer memberFlag,
        String redirectUrl,
        String token,
        String cardCreatedDate,
        String cardExpiryDate
) {
}
