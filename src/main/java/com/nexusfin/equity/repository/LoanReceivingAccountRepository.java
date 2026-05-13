package com.nexusfin.equity.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nexusfin.equity.entity.LoanReceivingAccount;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LoanReceivingAccountRepository extends BaseMapper<LoanReceivingAccount> {

    default LoanReceivingAccount selectDefaultActive() {
        return selectOne(Wrappers.<LoanReceivingAccount>lambdaQuery()
                .eq(LoanReceivingAccount::getIsDefault, 1)
                .eq(LoanReceivingAccount::getAccountStatus, "ACTIVE")
                .last("limit 1"));
    }
}
