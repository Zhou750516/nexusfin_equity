package com.nexusfin.equity.service;

import com.nexusfin.equity.entity.LoanReceivingAccount;
import com.nexusfin.equity.exception.BizException;
import com.nexusfin.equity.repository.LoanReceivingAccountRepository;
import com.nexusfin.equity.service.impl.LoanReceivingAccountServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanReceivingAccountServiceTest {

    @Mock
    private LoanReceivingAccountRepository loanReceivingAccountRepository;

    @Test
    void shouldReturnDefaultReceivingAccountFromRepository() {
        LoanReceivingAccount account = new LoanReceivingAccount();
        account.setAccountId("acc-db-001");
        account.setBankName("测试银行");
        account.setLastFour("1234");
        when(loanReceivingAccountRepository.selectDefaultActive()).thenReturn(account);

        LoanReceivingAccountService service = new LoanReceivingAccountServiceImpl(loanReceivingAccountRepository);

        LoanReceivingAccountService.ReceivingAccountDetails result = service.getDefaultReceivingAccount();

        assertThat(result.accountId()).isEqualTo("acc-db-001");
        assertThat(result.bankName()).isEqualTo("测试银行");
        assertThat(result.lastFour()).isEqualTo("1234");
    }

    @Test
    void shouldThrowControlledExceptionWhenDefaultReceivingAccountIsMissing() {
        when(loanReceivingAccountRepository.selectDefaultActive()).thenReturn(null);

        LoanReceivingAccountService service = new LoanReceivingAccountServiceImpl(loanReceivingAccountRepository);

        assertThatThrownBy(service::getDefaultReceivingAccount)
                .isInstanceOf(BizException.class)
                .extracting(throwable -> ((BizException) throwable).getErrorNo())
                .isEqualTo("LOAN_RECEIVING_ACCOUNT_NOT_CONFIGURED");
    }
}
