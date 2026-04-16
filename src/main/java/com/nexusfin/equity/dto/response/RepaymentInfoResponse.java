package com.nexusfin.equity.dto.response;

import java.math.BigDecimal;

public record RepaymentInfoResponse(
        String loanId,
        BigDecimal repaymentAmount,
        String repaymentType,
        BankAccountResponse bankCard,
        String tip
) {
}
