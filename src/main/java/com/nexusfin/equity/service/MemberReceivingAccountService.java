package com.nexusfin.equity.service;

public interface MemberReceivingAccountService {

    record ReceivingAccountDetails(
            String accountId,
            String bankName,
            String lastFour
    ) {
    }

    record UpsertCommand(
            String accountId,
            String bankName,
            String lastFour,
            String source
    ) {
    }

    ReceivingAccountDetails getDefaultReceivingAccount(String memberId);

    ReceivingAccountDetails getReceivingAccount(String memberId, String accountId);

    void upsertDefaultReceivingAccount(String memberId, UpsertCommand command);
}
