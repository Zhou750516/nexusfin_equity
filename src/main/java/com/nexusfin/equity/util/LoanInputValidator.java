package com.nexusfin.equity.util;

import com.nexusfin.equity.config.H5LoanProperties;
import com.nexusfin.equity.exception.BizException;

public final class LoanInputValidator {

    private LoanInputValidator() {
    }

    public static void validateAmountAndTerm(H5LoanProperties h5LoanProperties, Long amount, Integer term) {
        H5LoanProperties.AmountRange amountRange = h5LoanProperties.amountRange();
        if (amount < amountRange.min() || amount > amountRange.max()) {
            throw new BizException(400, "amount is out of range");
        }
        if ((amount - amountRange.min()) % amountRange.step() != 0) {
            throw new BizException(400, "amount step is invalid");
        }
        boolean supportedTerm = h5LoanProperties.termOptions().stream()
                .anyMatch(termOption -> termOption.value().equals(term));
        if (!supportedTerm) {
            throw new BizException(400, "term is unsupported");
        }
    }
}
