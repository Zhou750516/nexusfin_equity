package com.nexusfin.equity.config;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nexusfin.h5.loan")
public record H5LoanProperties(
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
