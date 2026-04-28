package com.nexusfin.equity.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record LoanApprovalResultResponse(
        String applicationId,
        String status,
        String purpose,
        BigDecimal approvedAmount,
        String estimatedArrivalTime,
        List<LoanApprovalStatusResponse.ApprovalStep> steps,
        boolean benefitsCardActivated,
        String tip,
        String loanId,
        List<RepaymentPlanItem> repaymentPlan
) {

    public record RepaymentPlanItem(
            Integer periodNo,
            String repaymentDate,
            BigDecimal repaymentPrincipal,
            BigDecimal repaymentInterest,
            BigDecimal repaymentAmount
    ) {
    }
}
