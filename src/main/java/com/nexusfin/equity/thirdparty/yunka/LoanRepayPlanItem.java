package com.nexusfin.equity.thirdparty.yunka;

public record LoanRepayPlanItem(
        Integer termNo,
        Integer periodNo,
        Integer status,
        String repayDate,
        Long repayPrincipal,
        Long repayInterest,
        Long repayAmount
) {

    public LoanRepayPlanItem(
            Integer termNo,
            String repayDate,
            Long repayPrincipal,
            Long repayInterest,
            Long repayAmount
    ) {
        this(termNo, termNo, null, repayDate, repayPrincipal, repayInterest, repayAmount);
    }
}
