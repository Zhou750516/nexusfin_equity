package com.nexusfin.equity.service;

import com.nexusfin.equity.dto.request.LoanCalculateRequest;
import com.nexusfin.equity.dto.response.LoanCalculateResponse;
import com.nexusfin.equity.dto.response.LoanCalculatorConfigResponse;

public interface LoanCalculatorService {

    LoanCalculatorConfigResponse getCalculatorConfig(String memberId);

    LoanCalculateResponse calculate(String memberId, String uid, LoanCalculateRequest request);
}
