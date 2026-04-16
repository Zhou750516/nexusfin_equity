package com.nexusfin.equity.service.impl;

import com.nexusfin.equity.exception.BizException;
import com.nexusfin.equity.service.XiaohuaAuthClient;
import org.springframework.stereotype.Service;

@Service
public class SkeletonXiaohuaAuthClient implements XiaohuaAuthClient {

    @Override
    public JointIdentity exchange(String token) {
        throw new BizException("XIAOHUA_JOINT_LOGIN_NOT_READY", "Xiaohua joint-login contract is not configured");
    }
}
