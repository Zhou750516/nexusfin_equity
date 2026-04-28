package com.nexusfin.equity.service;

import com.nexusfin.equity.dto.response.LoanApprovalResultResponse;
import com.nexusfin.equity.dto.response.LoanApprovalStatusResponse;

public interface LoanApprovalQueryService {

    LoanApprovalStatusResponse getApprovalStatus(String memberId, String applicationId);

    LoanApprovalResultResponse getApprovalResult(String memberId, String applicationId);
}
