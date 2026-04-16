package com.nexusfin.equity.service.impl;

import com.nexusfin.equity.config.AuthProperties;
import com.nexusfin.equity.dto.request.JointLoginRequest;
import com.nexusfin.equity.entity.MemberChannel;
import com.nexusfin.equity.entity.MemberInfo;
import com.nexusfin.equity.enums.MemberStatusEnum;
import com.nexusfin.equity.exception.BizException;
import com.nexusfin.equity.repository.MemberChannelRepository;
import com.nexusfin.equity.repository.MemberInfoRepository;
import com.nexusfin.equity.service.JointLoginService;
import com.nexusfin.equity.service.XiaohuaAuthClient;
import com.nexusfin.equity.util.JointLoginTargetPageResolver;
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
public class JointLoginServiceImpl implements JointLoginService {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^1\\d{10}$");
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("^[1-9]\\d{16}[\\dX]$");

    private final XiaohuaAuthClient xiaohuaAuthClient;
    private final MemberInfoRepository memberInfoRepository;
    private final MemberChannelRepository memberChannelRepository;
    private final JwtUtil jwtUtil;
    private final AuthProperties authProperties;
    private final SensitiveDataCipher sensitiveDataCipher;
    private final JointLoginTargetPageResolver targetPageResolver;

    public JointLoginServiceImpl(
            XiaohuaAuthClient xiaohuaAuthClient,
            MemberInfoRepository memberInfoRepository,
            MemberChannelRepository memberChannelRepository,
            JwtUtil jwtUtil,
            AuthProperties authProperties,
            SensitiveDataCipher sensitiveDataCipher,
            JointLoginTargetPageResolver targetPageResolver
    ) {
        this.xiaohuaAuthClient = xiaohuaAuthClient;
        this.memberInfoRepository = memberInfoRepository;
        this.memberChannelRepository = memberChannelRepository;
        this.jwtUtil = jwtUtil;
        this.authProperties = authProperties;
        this.sensitiveDataCipher = sensitiveDataCipher;
        this.targetPageResolver = targetPageResolver;
    }

    @Override
    @Transactional
    public JointLoginResult login(JointLoginRequest request) {
        if (request.token() == null || request.token().isBlank()) {
            throw new BizException("JOINT_LOGIN_TOKEN_REQUIRED", "Joint login token is required");
        }
        XiaohuaAuthClient.JointIdentity identity = xiaohuaAuthClient.exchange(request.token());
        MemberInfo memberInfo = findOrCreateMember(identity);
        ensureChannelBinding(memberInfo, identity.externalUserId(), resolveChannelCode(identity.channelCode()));
        String jwtToken = jwtUtil.generateToken(memberInfo.getMemberId(), identity.externalUserId());
        String benefitOrderNo = firstNonBlank(request.benefitOrderNo(), request.orderNo());
        return new JointLoginResult(
                jwtToken,
                normalizeScene(request.scene()),
                targetPageResolver.resolve(request.scene()),
                benefitOrderNo,
                true
        );
    }

    private MemberInfo findOrCreateMember(XiaohuaAuthClient.JointIdentity identity) {
        MemberInfo existing = memberInfoRepository.selectByExternalUserId(identity.externalUserId());
        if (existing != null) {
            syncMemberInfo(existing, identity);
            memberInfoRepository.updateById(existing);
            return existing;
        }
        MemberInfo memberInfo = new MemberInfo();
        memberInfo.setMemberId(RequestIdUtil.nextId("mem"));
        syncMemberInfo(memberInfo, identity);
        memberInfo.setMemberStatus(MemberStatusEnum.ACTIVE.name());
        memberInfo.setCreatedTs(LocalDateTime.now());
        memberInfo.setUpdatedTs(LocalDateTime.now());
        memberInfoRepository.insert(memberInfo);
        return memberInfo;
    }

    private void syncMemberInfo(MemberInfo memberInfo, XiaohuaAuthClient.JointIdentity identity) {
        boolean newMember = memberInfo.getCreatedTs() == null;
        String channelCode = resolveChannelCode(identity.channelCode());
        String phone = normalizePhone(identity.phone(), newMember);
        String idCard = normalizeIdCard(identity.idCard(), newMember);
        String realName = normalizeRealName(identity.realName(), newMember);
        memberInfo.setTechPlatformUserId(identity.externalUserId());
        memberInfo.setExternalUserId(identity.externalUserId());
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

    private void ensureChannelBinding(MemberInfo memberInfo, String externalUserId, String channelCode) {
        MemberChannel existingChannel = memberChannelRepository.selectByChannelAndExternalUserId(channelCode, externalUserId);
        if (existingChannel != null) {
            return;
        }
        MemberChannel memberChannel = new MemberChannel();
        memberChannel.setMemberId(memberInfo.getMemberId());
        memberChannel.setChannelCode(channelCode);
        memberChannel.setExternalUserId(externalUserId);
        memberChannel.setBindStatus("BOUND");
        memberChannel.setCreatedTs(LocalDateTime.now());
        memberChannel.setUpdatedTs(LocalDateTime.now());
        memberChannelRepository.insert(memberChannel);
    }

    private String resolveChannelCode(String channelCode) {
        return firstNonBlank(channelCode, authProperties.getDefaultChannelCode());
    }

    private String normalizeScene(String scene) {
        String normalized = firstNonBlank(scene, "unknown");
        return normalized.toLowerCase(Locale.ROOT);
    }

    private String normalizePhone(String rawPhone, boolean required) {
        if (rawPhone == null || rawPhone.isBlank()) {
            if (required) {
                throw new BizException("PHONE_REQUIRED", "Phone is required for joint login provisioning");
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
                throw new BizException("ID_CARD_REQUIRED", "Id card is required for joint login provisioning");
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
                throw new BizException("REAL_NAME_REQUIRED", "Real name is required for joint login provisioning");
            }
            return null;
        }
        String normalized = rawRealName.trim();
        if (normalized.isEmpty() || normalized.length() > 64) {
            throw new BizException("REAL_NAME_INVALID", "Real name format is invalid");
        }
        return normalized;
    }

    private String firstNonBlank(String preferred, String fallback) {
        return preferred == null || preferred.isBlank() ? fallback : preferred;
    }
}
