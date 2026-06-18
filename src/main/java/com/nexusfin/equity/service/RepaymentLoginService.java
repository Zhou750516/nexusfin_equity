package com.nexusfin.equity.service;

import com.nexusfin.equity.dto.request.RepaymentLoginRequest;

public interface RepaymentLoginService {

    RepaymentLoginResult login(RepaymentLoginRequest request);

    record RepaymentLoginResult(
            String jwtToken,
            Integer loanId
    ) {
    }
}
