package com.nexusfin.equity.service;

import com.nexusfin.equity.dto.request.JointLoginRequest;

public interface JointLoginService {

    JointLoginResult login(JointLoginRequest request);

    record JointLoginResult(
            String jwtToken,
            String scene,
            String targetPage,
            String benefitOrderNo,
            boolean localUserReady
    ) {
    }
}
