package com.nexusfin.equity.service.impl;

import com.nexusfin.equity.config.AuthProperties;
import com.nexusfin.equity.dto.response.CurrentUserResponse;
import com.nexusfin.equity.dto.response.TechPlatformUserProfileResponse;
import com.nexusfin.equity.entity.MemberChannel;
import com.nexusfin.equity.entity.MemberInfo;
import com.nexusfin.equity.enums.MemberStatusEnum;
import com.nexusfin.equity.exception.BizException;
import com.nexusfin.equity.repository.MemberChannelRepository;
import com.nexusfin.equity.repository.MemberInfoRepository;
import com.nexusfin.equity.service.AuthService;
import com.nexusfin.equity.service.TechPlatformUserClient;
import com.nexusfin.equity.util.AuthContextUtil;
import com.nexusfin.equity.util.AuthPrincipal;
import com.nexusfin.equity.util.JwtUtil;
import com.nexusfin.equity.util.RequestIdUtil;
import com.nexusfin.equity.util.SensitiveDataUtil;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private final TechPlatformUserClient techPlatformUserClient;
    private final MemberInfoRepository memberInfoRepository;
    private final MemberChannelRepository memberChannelRepository;
    private final JwtUtil jwtUtil;
    private final AuthProperties authProperties;

    public AuthServiceImpl(
            TechPlatformUserClient techPlatformUserClient,
            MemberInfoRepository memberInfoRepository,
            MemberChannelRepository memberChannelRepository,
            JwtUtil jwtUtil,
            AuthProperties authProperties
    ) {
        this.techPlatformUserClient = techPlatformUserClient;
        this.memberInfoRepository = memberInfoRepository;
        this.memberChannelRepository = memberChannelRepository;
        this.jwtUtil = jwtUtil;
        this.authProperties = authProperties;
    }

    @Override
    @Transactional
    public AuthLoginResult loginWithTechToken(String techToken, String redirectUrl) {
        if (techToken == null || techToken.isBlank()) {
            throw new BizException("TECH_TOKEN_REQUIRED", "SSO token is required");
        }
        TechPlatformUserProfileResponse userProfile = techPlatformUserClient.getCurrentUser(techToken);
        MemberInfo memberInfo = findOrCreateMember(userProfile);
        ensureChannelBinding(memberInfo, userProfile.userId());
        return new AuthLoginResult(
                resolveRedirectUrl(redirectUrl),
                jwtUtil.generateToken(memberInfo.getMemberId(), userProfile.userId())
        );
    }

    @Override
    public CurrentUserResponse getCurrentUser() {
        AuthPrincipal principal = AuthContextUtil.getRequiredPrincipal();
        MemberInfo memberInfo = memberInfoRepository.selectById(principal.memberId());
        if (memberInfo == null) {
            throw new BizException(401, "Unauthorized");
        }
        return new CurrentUserResponse(
                memberInfo.getMemberId(),
                memberInfo.getTechPlatformUserId(),
                memberInfo.getExternalUserId(),
                memberInfo.getMemberStatus()
        );
    }

    private MemberInfo findOrCreateMember(TechPlatformUserProfileResponse userProfile) {
        MemberInfo existing = memberInfoRepository.selectByTechPlatformUserId(userProfile.userId());
        if (existing != null) {
            syncMemberInfo(existing, userProfile);
            memberInfoRepository.updateById(existing);
            return existing;
        }
        MemberInfo memberInfo = new MemberInfo();
        memberInfo.setMemberId(RequestIdUtil.nextId("mem"));
        syncMemberInfo(memberInfo, userProfile);
        memberInfo.setMemberStatus(MemberStatusEnum.ACTIVE.name());
        memberInfo.setCreatedTs(LocalDateTime.now());
        memberInfo.setUpdatedTs(LocalDateTime.now());
        memberInfoRepository.insert(memberInfo);
        return memberInfo;
    }

    private void syncMemberInfo(MemberInfo memberInfo, TechPlatformUserProfileResponse userProfile) {
        String phone = defaultValue(userProfile.phone(), userProfile.userId());
        String idCardSeed = defaultValue(userProfile.idCard(), userProfile.userId());
        memberInfo.setTechPlatformUserId(userProfile.userId());
        memberInfo.setExternalUserId(userProfile.userId());
        memberInfo.setMobileEncrypted(SensitiveDataUtil.encrypt(phone));
        memberInfo.setMobileHash(SensitiveDataUtil.sha256(phone));
        memberInfo.setIdCardEncrypted(SensitiveDataUtil.encrypt(idCardSeed));
        memberInfo.setIdCardHash(SensitiveDataUtil.sha256(idCardSeed));
        memberInfo.setRealNameEncrypted(SensitiveDataUtil.encrypt(defaultValue(userProfile.realName(), "N/A")));
        if (memberInfo.getCreatedTs() == null) {
            memberInfo.setCreatedTs(LocalDateTime.now());
        }
        memberInfo.setUpdatedTs(LocalDateTime.now());
    }

    private void ensureChannelBinding(MemberInfo memberInfo, String techPlatformUserId) {
        MemberChannel existingChannel = memberChannelRepository.selectByChannelAndExternalUserId(
                authProperties.getDefaultChannelCode(),
                techPlatformUserId
        );
        if (existingChannel != null) {
            return;
        }
        MemberChannel memberChannel = new MemberChannel();
        memberChannel.setMemberId(memberInfo.getMemberId());
        memberChannel.setChannelCode(authProperties.getDefaultChannelCode());
        memberChannel.setExternalUserId(techPlatformUserId);
        memberChannel.setBindStatus("BOUND");
        memberChannel.setCreatedTs(LocalDateTime.now());
        memberChannel.setUpdatedTs(LocalDateTime.now());
        memberChannelRepository.insert(memberChannel);
    }

    private String resolveRedirectUrl(String redirectUrl) {
        if (redirectUrl == null || redirectUrl.isBlank()) {
            return authProperties.getDefaultRedirectUrl();
        }
        boolean allowed = authProperties.getRedirectWhitelist().stream()
                .anyMatch(redirectUrl::startsWith);
        if (!allowed) {
            throw new BizException("REDIRECT_URL_INVALID", "Redirect url is not allowed");
        }
        return redirectUrl;
    }

    private String defaultValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
