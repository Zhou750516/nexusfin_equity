package com.nexusfin.equity.dto.response;

public record JointLoginResponse(
        boolean loginSuccess,
        String scene,
        String targetPage,
        String benefitOrderNo,
        boolean localUserReady
) {
}
