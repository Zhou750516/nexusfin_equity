package com.nexusfin.equity.service;

import com.nexusfin.equity.dto.request.LoanApplyRequest;
import com.nexusfin.equity.dto.request.LoanCalculateRequest;
import com.nexusfin.equity.dto.response.LoanApprovalResultResponse;
import com.nexusfin.equity.dto.response.LoanApprovalStatusResponse;
import com.nexusfin.equity.dto.response.LoanApplyResponse;
import com.nexusfin.equity.dto.response.LoanCalculateResponse;
import com.nexusfin.equity.dto.response.LoanCalculatorConfigResponse;

public interface LoanService {

    LoanCalculatorConfigResponse getCalculatorConfig();

    LoanCalculateResponse calculate(String memberId, String uid, LoanCalculateRequest request);

    LoanApplyResponse apply(String memberId, String uid, LoanApplyRequest request);

    LoanApprovalStatusResponse getApprovalStatus(String memberId, String applicationId);

    LoanApprovalResultResponse getApprovalResult(String memberId, String applicationId);
}
