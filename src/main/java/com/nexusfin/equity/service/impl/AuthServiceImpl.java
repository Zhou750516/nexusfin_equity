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
import com.nexusfin.equity.util.SensitiveDataCipher;
import com.nexusfin.equity.util.SensitiveDataUtil;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^1\\d{10}$");
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("^[1-9]\\d{16}[\\dX]$");

    private final TechPlatformUserClient techPlatformUserClient;
    private final MemberInfoRepository memberInfoRepository;
    private final MemberChannelRepository memberChannelRepository;
    private final JwtUtil jwtUtil;
    private final AuthProperties authProperties;
    private final SensitiveDataCipher sensitiveDataCipher;

    public AuthServiceImpl(
            TechPlatformUserClient techPlatformUserClient,
            MemberInfoRepository memberInfoRepository,
            MemberChannelRepository memberChannelRepository,
            JwtUtil jwtUtil,
            AuthProperties authProperties,
            SensitiveDataCipher sensitiveDataCipher
    ) {
        this.techPlatformUserClient = techPlatformUserClient;
        this.memberInfoRepository = memberInfoRepository;
        this.memberChannelRepository = memberChannelRepository;
        this.jwtUtil = jwtUtil;
        this.authProperties = authProperties;
        this.sensitiveDataCipher = sensitiveDataCipher;
    }

    @Override
    @Transactional
    public AuthLoginResult loginWithTechToken(String techToken, String redirectUrl) {
        if (techToken == null || techToken.isBlank()) {
            throw new BizException("TECH_TOKEN_REQUIRED", "SSO token is required");
        }
        TechPlatformUserProfileResponse userProfile = techPlatformUserClient.getCurrentUser(techToken);
        MemberInfo memberInfo = findOrCreateMember(userProfile);
        ensureChannelBinding(memberInfo, userProfile.userId(), resolveChannelCode(userProfile.channelCode()));
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
        boolean newMember = memberInfo.getCreatedTs() == null;
        String channelCode = resolveChannelCode(userProfile.channelCode());
        String phone = normalizePhone(sensitiveDataCipher.decodeInbound(channelCode, userProfile.phone()), newMember);
        String idCard = normalizeIdCard(sensitiveDataCipher.decodeInbound(channelCode, userProfile.idCard()), newMember);
        String realName = normalizeRealName(sensitiveDataCipher.decodeInbound(channelCode, userProfile.realName()), newMember);
        memberInfo.setTechPlatformUserId(userProfile.userId());
        memberInfo.setExternalUserId(userProfile.userId());
        if (phone != null) {
            memberInfo.setMobileEncrypted(sensitiveDataCipher.encrypt(phone));
            memberInfo.setMobileHash(SensitiveDataUtil.sha256(phone));
        }
        if (idCard != null) {
            memberInfo.setIdCardEncrypted(sensitiveDataCipher.encrypt(idCard));
            memberInfo.setIdCardHash(SensitiveDataUtil.sha256(idCard));
        }
        if (realName != null) {
            memberInfo.setRealNameEncrypted(sensitiveDataCipher.encrypt(realName));
        }
        if (memberInfo.getCreatedTs() == null) {
            memberInfo.setCreatedTs(LocalDateTime.now());
        }
        memberInfo.setUpdatedTs(LocalDateTime.now());
    }

    private void ensureChannelBinding(MemberInfo memberInfo, String techPlatformUserId, String channelCode) {
        MemberChannel existingChannel = memberChannelRepository.selectByChannelAndExternalUserId(
                channelCode,
                techPlatformUserId
        );
        if (existingChannel != null) {
            return;
        }
        MemberChannel memberChannel = new MemberChannel();
        memberChannel.setMemberId(memberInfo.getMemberId());
        memberChannel.setChannelCode(channelCode);
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

    private String resolveChannelCode(String channelCode) {
        return defaultValue(channelCode, authProperties.getDefaultChannelCode());
    }

    private String normalizePhone(String rawPhone, boolean required) {
        if (rawPhone == null || rawPhone.isBlank()) {
            if (required) {
                throw new BizException("PHONE_REQUIRED", "Phone is required for SSO provisioning");
            }
            return null;
        }
        String normalized = rawPhone.replaceAll("\\s+", "");
        if (!PHONE_PATTERN.matcher(normalized).matches()) {
            throw new BizException("PHONE_INVALID", "Phone format is invalid");
        }
        return normalized;
    }

    private String normalizeIdCard(String rawIdCard, boolean required) {
        if (rawIdCard == null || rawIdCard.isBlank()) {
            if (required) {
                throw new BizException("ID_CARD_REQUIRED", "Id card is required for SSO provisioning");
            }
            return null;
        }
        String normalized = rawIdCard.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
        if (!ID_CARD_PATTERN.matcher(normalized).matches()) {
            throw new BizException("ID_CARD_INVALID", "Id card format is invalid");
        }
        return normalized;
    }

    private String normalizeRealName(String rawRealName, boolean required) {
        if (rawRealName == null || rawRealName.isBlank()) {
            if (required) {
                throw new BizException("REAL_NAME_REQUIRED", "Real name is required for SSO provisioning");
            }
            return null;
        }
        String normalized = rawRealName.trim();
        if (normalized.isEmpty() || normalized.length() > 64) {
            throw new BizException("REAL_NAME_INVALID", "Real name format is invalid");
        }
        return normalized;
    }
}
