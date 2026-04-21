package com.nexusfin.equity.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusfin.equity.entity.MemberChannel;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MemberChannelRepository extends BaseMapper<MemberChannel> {

    default MemberChannel selectByChannelAndExternalUserId(String channelCode, String externalUserId) {
        return selectOne(Wrappers.<MemberChannel>lambdaQuery()
                .eq(MemberChannel::getChannelCode, channelCode)
                .eq(MemberChannel::getExternalUserId, externalUserId)
                .last("limit 1"));
    }

    default MemberChannel selectLatestByMemberId(String memberId) {
        return selectOne(Wrappers.<MemberChannel>lambdaQuery()
                .eq(MemberChannel::getMemberId, memberId)
                .orderByDesc(MemberChannel::getUpdatedTs)
                .last("limit 1"));
    }
}
