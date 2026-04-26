package com.nexusfin.equity.thirdparty.yunka;

public record LoanRepayPlanItem(
        Integer termNo,
        String repayDate,
        Long repayPrincipal,
        Long repayInterest,
        Long repayAmount
) {
}
