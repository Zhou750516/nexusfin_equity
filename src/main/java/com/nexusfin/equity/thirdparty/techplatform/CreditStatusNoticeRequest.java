package com.nexusfin.equity.thirdparty.techplatform;

import java.math.BigDecimal;

public record CreditStatusNoticeRequest(
        String orderId,
        String userId,
        int code,
        String msg,
        String approveTime,
        String periods,
        String periodUnit,
        String repaymentMethod,
        LoanOption loanOption,
        CreditLimit creditLimit
) {

    public record LoanOption(
            BigDecimal minAmount,
            BigDecimal maxAmount,
            BigDecimal amountStep,
            String availableTime
    ) {
    }

    public record CreditLimit(
            BigDecimal totalCreditAmount,
            BigDecimal creditBalance,
            long creditBeginTime,
            long creditEndTime,
            String creditStatus,
            String beginDate,
            String endDate,
            String limitType
    ) {
    }
}
