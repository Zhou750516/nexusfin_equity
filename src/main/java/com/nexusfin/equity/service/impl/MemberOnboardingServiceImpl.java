package com.nexusfin.equity.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nexusfin.equity.dto.request.RegisterUserRequest;
import com.nexusfin.equity.dto.response.RegisterUserResponse;
import com.nexusfin.equity.entity.MemberChannel;
import com.nexusfin.equity.entity.MemberInfo;
import com.nexusfin.equity.enums.MemberStatusEnum;
import com.nexusfin.equity.repository.MemberChannelRepository;
import com.nexusfin.equity.repository.MemberInfoRepository;
import com.nexusfin.equity.service.IdempotencyService;
import com.nexusfin.equity.service.MemberOnboardingService;
import com.nexusfin.equity.util.RequestIdUtil;
import com.nexusfin.equity.util.SensitiveDataUtil;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberOnboardingServiceImpl implements MemberOnboardingService {

    private static final Logger log = LoggerFactory.getLogger(MemberOnboardingServiceImpl.class);

    private final MemberInfoRepository memberInfoRepository;
    private final MemberChannelRepository memberChannelRepository;
    private final IdempotencyService idempotencyService;

    public MemberOnboardingServiceImpl(
            MemberInfoRepository memberInfoRepository,
            MemberChannelRepository memberChannelRepository,
            IdempotencyService idempotencyService
    ) {
        this.memberInfoRepository = memberInfoRepository;
        this.memberChannelRepository = memberChannelRepository;
        this.idempotencyService = idempotencyService;
    }

    @Override
    @Transactional
    public RegisterUserResponse register(RegisterUserRequest request) {
        // 先按“渠道 + 外部用户标识”判断是否重复导流，保证同一个上游身份不会反复生成会员。
        MemberChannel existingChannel = memberChannelRepository.selectOne(Wrappers.<MemberChannel>lambdaQuery()
                .eq(MemberChannel::getChannelCode, request.channelCode())
                .eq(MemberChannel::getExternalUserId, request.userInfo().externalUserId())
                .last("limit 1"));
        if (existingChannel != null || idempotencyService.isProcessed(request.requestId())) {
            String memberId = existingChannel != null
                    ? existingChannel.getMemberId()
                    : idempotencyService.getByRequestId(request.requestId()).getBizKey();
            log.info("traceId={} bizOrderNo={} duplicate onboarding request", com.nexusfin.equity.util.TraceIdUtil.getTraceId(), memberId);
            return new RegisterUserResponse(memberId, "DUPLICATE");
        }

        RegisterUserRequest.UserInfo userInfo = request.userInfo();
        String mobileHash = SensitiveDataUtil.sha256(userInfo.mobileEncrypted());
        String idCardHash = SensitiveDataUtil.sha256(userInfo.idCardEncrypted());
        // 这里用 hash 做归并查找，用加密值做落库存储，兼顾“可查找”和“敏感信息不明文保存”。
        MemberInfo memberInfo = memberInfoRepository.selectOne(Wrappers.<MemberInfo>lambdaQuery()
                .eq(MemberInfo::getMobileHash, mobileHash)
                .eq(MemberInfo::getIdCardHash, idCardHash)
                .last("limit 1"));
        if (memberInfo == null) {
            memberInfo = new MemberInfo();
            memberInfo.setMemberId(RequestIdUtil.nextId("mem"));
            memberInfo.setExternalUserId(userInfo.externalUserId());
            memberInfo.setMobileEncrypted(SensitiveDataUtil.encrypt(userInfo.mobileEncrypted()));
            memberInfo.setMobileHash(mobileHash);
            memberInfo.setIdCardEncrypted(SensitiveDataUtil.encrypt(userInfo.idCardEncrypted()));
            memberInfo.setIdCardHash(idCardHash);
            memberInfo.setRealNameEncrypted(SensitiveDataUtil.encrypt(userInfo.realNameEncrypted()));
            memberInfo.setMemberStatus(MemberStatusEnum.ACTIVE.name());
            memberInfo.setCreatedTs(LocalDateTime.now());
            memberInfo.setUpdatedTs(LocalDateTime.now());
            memberInfoRepository.insert(memberInfo);
        }
        MemberChannel memberChannel = new MemberChannel();
        memberChannel.setMemberId(memberInfo.getMemberId());
        memberChannel.setChannelCode(request.channelCode());
        memberChannel.setExternalUserId(userInfo.externalUserId());
        memberChannel.setBindStatus("BOUND");
        memberChannel.setCreatedTs(LocalDateTime.now());
        memberChannel.setUpdatedTs(LocalDateTime.now());
        memberChannelRepository.insert(memberChannel);
        // 幂等记录和业务主键绑定，后续重复请求可以直接回放相同业务结果。
        idempotencyService.markProcessed(request.requestId(), "REGISTER", memberInfo.getMemberId(), memberInfo.getMemberId());
        log.info("traceId={} bizOrderNo={} member onboarded", com.nexusfin.equity.util.TraceIdUtil.getTraceId(), memberInfo.getMemberId());
        return new RegisterUserResponse(memberInfo.getMemberId(), "SUCCESS");
    }
}
