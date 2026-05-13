package com.nexusfin.equity.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.nexusfin.equity.entity.MemberReceivingAccount;
import com.nexusfin.equity.exception.BizException;
import com.nexusfin.equity.repository.MemberReceivingAccountRepository;
import com.nexusfin.equity.service.MemberReceivingAccountService;
import java.time.LocalDateTime;
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
        existing.setUpdatedTs(LocalDateTime.now());
        memberReceivingAccountRepository.updateById(existing);
    }

    private ReceivingAccountDetails toDetails(MemberReceivingAccount account) {
        return new ReceivingAccountDetails(
                account.getAccountId(),
                account.getBankName(),
                account.getLastFour()
        );
    }
}
