package com.nexusfin.equity.service.impl;

import com.nexusfin.equity.dto.request.LoanApplyRequest;
import com.nexusfin.equity.dto.request.LoanCalculateRequest;
import com.nexusfin.equity.dto.response.LoanApprovalResultResponse;
import com.nexusfin.equity.dto.response.LoanApprovalStatusResponse;
import com.nexusfin.equity.dto.response.LoanApplyResponse;
import com.nexusfin.equity.dto.response.LoanCalculateResponse;
import com.nexusfin.equity.dto.response.LoanCalculatorConfigResponse;
import com.nexusfin.equity.service.LoanApplicationService;
import com.nexusfin.equity.service.LoanApprovalQueryService;
import com.nexusfin.equity.service.LoanCalculatorService;
import com.nexusfin.equity.service.LoanService;
import org.springframework.stereotype.Service;

@Service
public class LoanServiceImpl implements LoanService {

    private final LoanCalculatorService loanCalculatorService;
    private final LoanApplicationService loanApplicationService;
    private final LoanApprovalQueryService loanApprovalQueryService;

    public LoanServiceImpl(
            LoanCalculatorService loanCalculatorService,
            LoanApplicationService loanApplicationService,
            LoanApprovalQueryService loanApprovalQueryService
    ) {
        this.loanCalculatorService = loanCalculatorService;
        this.loanApplicationService = loanApplicationService;
        this.loanApprovalQueryService = loanApprovalQueryService;
    }

    @Override
    public LoanCalculatorConfigResponse getCalculatorConfig() {
        return loanCalculatorService.getCalculatorConfig();
    }

    @Override
    public LoanCalculateResponse calculate(String memberId, String uid, LoanCalculateRequest request) {
        return loanCalculatorService.calculate(memberId, uid, request);
    }

    @Override
    public LoanApplyResponse apply(String memberId, String uid, LoanApplyRequest request) {
        return loanApplicationService.apply(memberId, uid, request);
    }

    @Override
    public LoanApprovalStatusResponse getApprovalStatus(String memberId, String applicationId) {
        return loanApprovalQueryService.getApprovalStatus(memberId, applicationId);
    }

    @Override
    public LoanApprovalResultResponse getApprovalResult(String memberId, String applicationId) {
        return loanApprovalQueryService.getApprovalResult(memberId, applicationId);
    }
}
