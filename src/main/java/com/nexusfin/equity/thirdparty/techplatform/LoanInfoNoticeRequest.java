package com.nexusfin.equity.thirdparty.techplatform;

import java.math.BigDecimal;

public record LoanInfoNoticeRequest(
        String orderId,
        String loanNo,
        BigDecimal loanAmount,
        int period,
        String loanBeginDate,
        String loanEndDate,
        String orderStatus,
        Long loanDate,
        String contractNo,
        String loanBankCard,
        String loanBank,
        String reason,
        Integer repayDate,
        Integer outAccountDays,
        Boolean isRepayInfo,
        String isFirstLoan
) {
}
