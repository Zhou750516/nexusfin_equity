package com.nexusfin.equity.service;

import java.util.List;

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

    record CardCacheCommand(
            String accountId,
            String bankName,
            String lastFour,
            Integer isDefault,
            Integer sourceIndex,
            String source
    ) {
    }

    ReceivingAccountDetails getDefaultReceivingAccount(String memberId);

    ReceivingAccountDetails getReceivingAccount(String memberId, String accountId);

    void upsertDefaultReceivingAccount(String memberId, UpsertCommand command);

    void cacheReceivingAccounts(String memberId, List<CardCacheCommand> commands);
}
