package com.nexusfin.equity.service;

public interface XiaohuaAuthClient {

    JointIdentity exchange(String token);

    record JointIdentity(
            String externalUserId,
            String phone,
            String realName,
            String idCard,
            String channelCode
    ) {
    }
}
