package com.nexusfin.equity.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record LoanCalculateResponse(
        BigDecimal totalFee,
        String annualRate,
        List<RepaymentPlanItem> repaymentPlan
) {

    public record RepaymentPlanItem(
            Integer period,
            String date,
            BigDecimal principal,
            BigDecimal interest,
            BigDecimal total
    ) {
    }
}
