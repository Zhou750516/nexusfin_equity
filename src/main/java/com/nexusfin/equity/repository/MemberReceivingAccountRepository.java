package com.nexusfin.equity.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nexusfin.equity.entity.MemberReceivingAccount;
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
}
