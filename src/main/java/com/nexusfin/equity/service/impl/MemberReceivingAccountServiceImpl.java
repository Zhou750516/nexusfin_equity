package com.nexusfin.equity.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.nexusfin.equity.entity.MemberReceivingAccount;
import com.nexusfin.equity.exception.BizException;
import com.nexusfin.equity.repository.MemberReceivingAccountRepository;
import com.nexusfin.equity.service.MemberReceivingAccountService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class MemberReceivingAccountServiceImpl implements MemberReceivingAccountService {

    private final MemberReceivingAccountRepository memberReceivingAccountRepository;

    public MemberReceivingAccountServiceImpl(MemberReceivingAccountRepository memberReceivingAccountRepository) {
        this.memberReceivingAccountRepository = memberReceivingAccountRepository;
    }

    @Override
    public ReceivingAccountDetails getDefaultReceivingAccount(String memberId) {
        MemberReceivingAccount account = memberReceivingAccountRepository.selectDefaultActive(memberId);
        if (account == null) {
            throw new BizException("MEMBER_RECEIVING_ACCOUNT_NOT_CONFIGURED",
                    "Member receiving account is not configured");
        }
        return toDetails(account);
    }

    @Override
    public ReceivingAccountDetails getReceivingAccount(String memberId, String accountId) {
        MemberReceivingAccount account = memberReceivingAccountRepository.selectByMemberIdAndAccountId(memberId, accountId);
        if (account == null) {
            throw new BizException("MEMBER_RECEIVING_ACCOUNT_NOT_FOUND",
                    "Receiving account does not belong to current member");
        }
        return toDetails(account);
    }

    @Override
    public void upsertDefaultReceivingAccount(String memberId, UpsertCommand command) {
        MemberReceivingAccount existing = memberReceivingAccountRepository.selectByMemberIdAndAccountId(memberId, command.accountId());
        memberReceivingAccountRepository.update(
                null,
                new UpdateWrapper<MemberReceivingAccount>()
                        .eq("member_id", memberId)
                        .set("is_default", 0)
                        .set("updated_ts", LocalDateTime.now())
        );
        if (existing == null) {
            MemberReceivingAccount created = new MemberReceivingAccount();
            created.setMemberId(memberId);
            created.setAccountId(command.accountId());
            created.setBankName(command.bankName());
            created.setLastFour(command.lastFour());
            created.setAccountStatus("ACTIVE");
            created.setIsDefault(1);
            created.setSource(command.source());
            created.setSourceIndex(0);
            created.setCreatedTs(LocalDateTime.now());
            created.setUpdatedTs(LocalDateTime.now());
            memberReceivingAccountRepository.insert(created);
            return;
        }
        existing.setBankName(command.bankName());
        existing.setLastFour(command.lastFour());
        existing.setAccountStatus("ACTIVE");
        existing.setIsDefault(1);
        existing.setSource(command.source());
        existing.setSourceIndex(0);
        existing.setUpdatedTs(LocalDateTime.now());
        memberReceivingAccountRepository.updateById(existing);
    }

    @Override
    public void cacheReceivingAccounts(String memberId, List<CardCacheCommand> commands) {
        if (commands == null || commands.isEmpty()) {
            return;
        }
        String source = commands.get(0).source();
        List<String> activeAccountIds = commands.stream()
                .map(CardCacheCommand::accountId)
                .toList();
        Map<String, MemberReceivingAccount> existingByAccountId = memberReceivingAccountRepository
                .selectByMemberId(memberId)
                .stream()
                .collect(Collectors.toMap(MemberReceivingAccount::getAccountId, Function.identity(), (left, right) -> left));
        memberReceivingAccountRepository.deactivateMissingCachedAccounts(memberId, source, activeAccountIds);
        for (CardCacheCommand command : commands) {
            MemberReceivingAccount existing = existingByAccountId.get(command.accountId());
            if (existing == null) {
                MemberReceivingAccount created = new MemberReceivingAccount();
                fillCachedAccount(created, memberId, command, LocalDateTime.now());
                created.setCreatedTs(LocalDateTime.now());
                memberReceivingAccountRepository.insert(created);
                continue;
            }
            fillCachedAccount(existing, memberId, command, existing.getCreatedTs());
            memberReceivingAccountRepository.updateById(existing);
        }
    }

    private void fillCachedAccount(
            MemberReceivingAccount account,
            String memberId,
            CardCacheCommand command,
            LocalDateTime createdTs
    ) {
        account.setMemberId(memberId);
        account.setAccountId(command.accountId());
        account.setBankName(command.bankName());
        account.setLastFour(command.lastFour());
        account.setAccountStatus("ACTIVE");
        account.setIsDefault(command.isDefault() == null ? 0 : command.isDefault());
        account.setSource(command.source());
        account.setSourceIndex(command.sourceIndex());
        account.setCreatedTs(createdTs == null ? LocalDateTime.now() : createdTs);
        account.setUpdatedTs(LocalDateTime.now());
    }

    private ReceivingAccountDetails toDetails(MemberReceivingAccount account) {
        return new ReceivingAccountDetails(
                account.getAccountId(),
                account.getBankName(),
                account.getLastFour()
        );
    }
}
