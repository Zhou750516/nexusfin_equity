package com.nexusfin.equity.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nexusfin.equity.entity.MemberReceivingAccount;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MemberReceivingAccountRepository extends BaseMapper<MemberReceivingAccount> {

    default MemberReceivingAccount selectDefaultActive(String memberId) {
        return selectOne(Wrappers.<MemberReceivingAccount>lambdaQuery()
                .eq(MemberReceivingAccount::getMemberId, memberId)
                .eq(MemberReceivingAccount::getIsDefault, 1)
                .eq(MemberReceivingAccount::getAccountStatus, "ACTIVE")
                .last("limit 1"));
    }

    default MemberReceivingAccount selectByMemberIdAndAccountId(String memberId, String accountId) {
        return selectOne(Wrappers.<MemberReceivingAccount>lambdaQuery()
                .eq(MemberReceivingAccount::getMemberId, memberId)
                .eq(MemberReceivingAccount::getAccountId, accountId)
                .eq(MemberReceivingAccount::getAccountStatus, "ACTIVE")
                .last("limit 1"));
    }

    default List<MemberReceivingAccount> selectByMemberId(String memberId) {
        return selectList(Wrappers.<MemberReceivingAccount>lambdaQuery()
                .eq(MemberReceivingAccount::getMemberId, memberId));
    }

    default List<MemberReceivingAccount> selectActiveByMemberId(String memberId) {
        return selectList(Wrappers.<MemberReceivingAccount>lambdaQuery()
                .eq(MemberReceivingAccount::getMemberId, memberId)
                .eq(MemberReceivingAccount::getAccountStatus, "ACTIVE"));
    }

    default void deactivateMissingCachedAccounts(String memberId, String source, Collection<String> activeAccountIds) {
        UpdateWrapper<MemberReceivingAccount> wrapper = new UpdateWrapper<MemberReceivingAccount>()
                .eq("member_id", memberId)
                .eq("source", source)
                .set("account_status", "INACTIVE")
                .set("is_default", 0)
                .set("updated_ts", LocalDateTime.now());
        if (activeAccountIds != null && !activeAccountIds.isEmpty()) {
            wrapper.notIn("account_id", activeAccountIds);
        }
        update(null, wrapper);
    }
}
