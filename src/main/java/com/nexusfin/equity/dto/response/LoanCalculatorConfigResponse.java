package com.nexusfin.equity.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;

public record LoanCalculatorConfigResponse(
        AmountRange amountRange,
        List<TermOption> termOptions,
        BigDecimal annualRate,
        String lender,
        ReceivingAccount receivingAccount
) {

    public record AmountRange(
            Long min,
            Long max,
            Long step,
            @JsonProperty("default")
            Long defaultAmount
    ) {
    }

    public record TermOption(
            String label,
            Integer value
    ) {
    }

    public record ReceivingAccount(
            String bankName,
            String lastFour,
            String accountId
    ) {
    }
}
