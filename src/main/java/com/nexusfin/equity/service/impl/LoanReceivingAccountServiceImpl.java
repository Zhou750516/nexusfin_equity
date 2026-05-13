package com.nexusfin.equity.service.impl;

import com.nexusfin.equity.entity.LoanReceivingAccount;
import com.nexusfin.equity.exception.BizException;
import com.nexusfin.equity.repository.LoanReceivingAccountRepository;
import com.nexusfin.equity.service.LoanReceivingAccountService;
import org.springframework.stereotype.Service;

@Service
public class LoanReceivingAccountServiceImpl implements LoanReceivingAccountService {

    private final LoanReceivingAccountRepository loanReceivingAccountRepository;

    public LoanReceivingAccountServiceImpl(LoanReceivingAccountRepository loanReceivingAccountRepository) {
        this.loanReceivingAccountRepository = loanReceivingAccountRepository;
    }

    @Override
    public ReceivingAccountDetails getDefaultReceivingAccount() {
        LoanReceivingAccount account = loanReceivingAccountRepository.selectDefaultActive();
        if (account == null) {
            throw new BizException("LOAN_RECEIVING_ACCOUNT_NOT_CONFIGURED",
                    "Default loan receiving account is not configured");
        }
        return new ReceivingAccountDetails(
                account.getAccountId(),
                account.getBankName(),
                account.getLastFour()
        );
    }
}
