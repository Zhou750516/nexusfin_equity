package com.nexusfin.equity.util;

import com.nexusfin.equity.config.H5LoanProperties;
import com.nexusfin.equity.exception.BizException;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoanInputValidatorTest {

    @Test
    void shouldAllowSupportedAmountAndTerm() {
        LoanInputValidator.validateAmountAndTerm(h5LoanProperties(), 3000L, 3);
    }

    @Test
    void shouldRejectOutOfRangeAmountInvalidStepAndUnsupportedTerm() {
        assertThatThrownBy(() -> LoanInputValidator.validateAmountAndTerm(h5LoanProperties(), 50L, 3))
                .isInstanceOf(BizException.class)
                .extracting(throwable -> ((BizException) throwable).getErrorMsg())
                .isEqualTo("amount is out of range");

        assertThatThrownBy(() -> LoanInputValidator.validateAmountAndTerm(h5LoanProperties(), 3050L, 3))
                .isInstanceOf(BizException.class)
                .extracting(throwable -> ((BizException) throwable).getErrorMsg())
                .isEqualTo("amount step is invalid");

        assertThatThrownBy(() -> LoanInputValidator.validateAmountAndTerm(h5LoanProperties(), 3000L, 12))
                .isInstanceOf(BizException.class)
                .extracting(throwable -> ((BizException) throwable).getErrorMsg())
                .isEqualTo("term is unsupported");
    }

    private H5LoanProperties h5LoanProperties() {
        return new H5LoanProperties(
                new H5LoanProperties.AmountRange(100L, 5000L, 100L, 3000L),
                List.of(
                        new H5LoanProperties.TermOption("3期", 3),
                        new H5LoanProperties.TermOption("6期", 6)
                ),
                BigDecimal.valueOf(0.18),
                "XX商业银行",
                new H5LoanProperties.ReceivingAccount("招商银行", "8648", "acc_001")
        );
    }
}
