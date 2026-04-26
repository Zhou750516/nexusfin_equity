package com.nexusfin.equity.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record RepaymentResultResponse(
        String repaymentId,
        String swiftNumber,
        String status,
        BigDecimal amount,
        String repaymentTime,
        BankAccountResponse bankCard,
        BigDecimal interestSaved,
        List<String> tips
) {
}
