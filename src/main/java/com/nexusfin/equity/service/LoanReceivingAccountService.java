package com.nexusfin.equity.service;

public interface LoanReceivingAccountService {

    record ReceivingAccountDetails(
            String accountId,
            String bankName,
            String lastFour
    ) {
    }

    ReceivingAccountDetails getDefaultReceivingAccount();
}
