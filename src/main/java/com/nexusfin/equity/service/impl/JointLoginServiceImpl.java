package com.nexusfin.equity.service.impl;

import com.nexusfin.equity.config.AuthProperties;
import com.nexusfin.equity.dto.request.JointLoginRequest;
import com.nexusfin.equity.entity.MemberChannel;
import com.nexusfin.equity.entity.MemberInfo;
import com.nexusfin.equity.enums.MemberStatusEnum;
import com.nexusfin.equity.exception.BizException;
import com.nexusfin.equity.exception.ErrorCodes;
import com.nexusfin.equity.exception.UpstreamTimeoutException;
import com.nexusfin.equity.repository.MemberChannelRepository;
import com.nexusfin.equity.repository.MemberInfoRepository;
import com.nexusfin.equity.service.JointLoginService;
import com.nexusfin.equity.service.XiaohuaGatewayService;
import com.nexusfin.equity.thirdparty.yunka.UserQueryRequest;
import com.nexusfin.equity.thirdparty.yunka.UserQueryResponse;
import com.nexusfin.equity.thirdparty.yunka.UserTokenRequest;
import com.nexusfin.equity.thirdparty.yunka.UserTokenResponse;
import com.nexusfin.equity.util.JointLoginTargetPageResolver;
import com.nexusfin.equity.util.JwtUtil;
import com.nexusfin.equity.util.RequestIdUtil;
import com.nexusfin.equity.util.SensitiveDataCipher;
import com.nexusfin.equity.util.SensitiveDataUtil;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JointLoginServiceImpl implements JointLoginService {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^1\\d{10}$");
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("^[1-9]\\d{16}[\\dX]$");

    private final XiaohuaGatewayService xiaohuaGatewayService;
    private final MemberInfoRepository memberInfoRepository;
    private final MemberChannelRepository memberChannelRepository;
    private final JwtUtil jwtUtil;
    private final AuthProperties authProperties;
    private final SensitiveDataCipher sensitiveDataCipher;
    private final JointLoginTargetPageResolver targetPageResolver;

    public JointLoginServiceImpl(
            XiaohuaGatewayService xiaohuaGatewayService,
            MemberInfoRepository memberInfoRepository,
            MemberChannelRepository memberChannelRepository,
            JwtUtil jwtUtil,
            AuthProperties authProperties,
            SensitiveDataCipher sensitiveDataCipher,
            JointLoginTargetPageResolver targetPageResolver
    ) {
        this.xiaohuaGatewayService = xiaohuaGatewayService;
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
        String benefitOrderNo = firstNonBlank(request.benefitOrderNo(), request.orderNo());
        String requestId = RequestIdUtil.nextId("xa");
        UserTokenResponse tokenResponse = validateJointToken(requestId, benefitOrderNo, request.token());
        String externalUserId = requiredValue(tokenResponse.cid(), "JOINT_LOGIN_CID_REQUIRED", "Xiaohua cid is required");
        MemberInfo existingMember = memberInfoRepository.selectByExternalUserId(externalUserId);
        String memberId = existingMember != null ? existingMember.getMemberId() : RequestIdUtil.nextId("mem");
        UserQueryResponse userQueryResponse = queryJointUser(requestId, benefitOrderNo, memberId, externalUserId);
        JointIdentity identity = mapToIdentity(tokenResponse, userQueryResponse);
        MemberInfo memberInfo = findOrCreateMember(existingMember, memberId, identity);
        ensureChannelBinding(memberInfo, identity.externalUserId(), resolveChannelCode(identity.channelCode()));
        String jwtToken = jwtUtil.generateToken(memberInfo.getMemberId(), identity.externalUserId());
        return new JointLoginResult(
                jwtToken,
                normalizeScene(request.scene()),
                targetPageResolver.resolve(request.scene()),
                benefitOrderNo,
                true
        );
    }

    private UserTokenResponse validateJointToken(String requestId, String benefitOrderNo, String token) {
        try {
            return xiaohuaGatewayService.validateUserToken(
                    requestId,
                    benefitOrderNo,
                    new UserTokenRequest(null, token)
            );
        } catch (UpstreamTimeoutException exception) {
            throw new BizException("JOINT_LOGIN_UPSTREAM_TIMEOUT", "Joint login temporarily unavailable");
        } catch (BizException exception) {
            if (ErrorCodes.YUNKA_UPSTREAM_REJECTED.equals(exception.getErrorNo())) {
                throw new BizException("JOINT_LOGIN_TOKEN_INVALID", "Joint login session expired");
            }
            throw new BizException("JOINT_LOGIN_UPSTREAM_FAILED", "Joint login temporarily unavailable");
        }
    }

    private UserQueryResponse queryJointUser(
            String requestId,
            String benefitOrderNo,
            String memberId,
            String externalUserId
    ) {
        try {
            return xiaohuaGatewayService.queryUser(
                    requestId,
                    benefitOrderNo,
                    new UserQueryRequest(memberId, externalUserId)
            );
        } catch (UpstreamTimeoutException exception) {
            throw new BizException("JOINT_LOGIN_UPSTREAM_TIMEOUT", "Joint login temporarily unavailable");
        } catch (BizException exception) {
            throw new BizException("JOINT_LOGIN_UPSTREAM_FAILED", "Joint login temporarily unavailable");
        }
    }

    private MemberInfo findOrCreateMember(MemberInfo existing, String memberId, JointIdentity identity) {
        if (existing != null) {
            syncMemberInfo(existing, identity);
            memberInfoRepository.updateById(existing);
            return existing;
        }
        MemberInfo memberInfo = new MemberInfo();
        memberInfo.setMemberId(memberId);
        syncMemberInfo(memberInfo, identity);
        memberInfo.setMemberStatus(MemberStatusEnum.ACTIVE.name());
        memberInfo.setCreatedTs(LocalDateTime.now());
        memberInfo.setUpdatedTs(LocalDateTime.now());
        memberInfoRepository.insert(memberInfo);
        return memberInfo;
    }

    private void syncMemberInfo(MemberInfo memberInfo, JointIdentity identity) {
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

    private JointIdentity mapToIdentity(UserTokenResponse tokenResponse, UserQueryResponse userQueryResponse) {
        JsonNode payload = userQueryResponse.payload();
        JsonNode idInfo = payload.path("idInfo");
        return new JointIdentity(
                requiredValue(tokenResponse.cid(), "JOINT_LOGIN_CID_REQUIRED", "Xiaohua cid is required"),
                firstNonBlank(tokenResponse.phone(), text(payload, "phone"), text(payload.path("basicInfo"), "phone")),
                firstNonBlank(tokenResponse.name(), text(idInfo, "name"), text(payload, "name")),
                firstNonBlank(
                        text(idInfo, "idno"),
                        text(idInfo, "idCard"),
                        text(idInfo, "idCardNo"),
                        text(payload, "idno")
                ),
                authProperties.getDefaultChannelCode()
        );
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

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String requiredValue(String value, String errorNo, String errorMsg) {
        if (value == null || value.isBlank()) {
            throw new BizException(errorNo, errorMsg);
        }
        return value;
    }

    private String text(JsonNode node, String fieldName) {
        return node.path(fieldName).asText("");
    }

    private record JointIdentity(
            String externalUserId,
            String phone,
            String realName,
            String idCard,
            String channelCode
    ) {
    }
}
