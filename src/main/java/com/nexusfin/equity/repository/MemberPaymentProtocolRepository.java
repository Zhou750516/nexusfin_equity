package com.nexusfin.equity.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nexusfin.equity.entity.MemberPaymentProtocol;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MemberPaymentProtocolRepository extends BaseMapper<MemberPaymentProtocol> {

    default MemberPaymentProtocol selectActiveByMemberId(String memberId, String providerCode) {
        return selectOne(Wrappers.<MemberPaymentProtocol>lambdaQuery()
                .eq(MemberPaymentProtocol::getMemberId, memberId)
                .eq(MemberPaymentProtocol::getProviderCode, providerCode)
                .eq(MemberPaymentProtocol::getProtocolStatus, "ACTIVE")
                .orderByDesc(MemberPaymentProtocol::getUpdatedTs)
                .last("limit 1"));
    }

    default MemberPaymentProtocol selectActiveByExternalUserId(String externalUserId, String providerCode) {
        return selectOne(Wrappers.<MemberPaymentProtocol>lambdaQuery()
                .eq(MemberPaymentProtocol::getExternalUserId, externalUserId)
                .eq(MemberPaymentProtocol::getProviderCode, providerCode)
                .eq(MemberPaymentProtocol::getProtocolStatus, "ACTIVE")
                .orderByDesc(MemberPaymentProtocol::getUpdatedTs)
                .last("limit 1"));
    }
}
