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
import com.nexusfin.equity.service.MemberReceivingAccountService;
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
import com.nexusfin.equity.util.TraceIdUtil;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JointLoginServiceImpl implements JointLoginService {

    private static final Logger log = LoggerFactory.getLogger(JointLoginServiceImpl.class);
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1\\d{10}$");
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("^[1-9]\\d{16}[\\dX]$");

    private final XiaohuaGatewayService xiaohuaGatewayService;
    private final MemberInfoRepository memberInfoRepository;
    private final MemberChannelRepository memberChannelRepository;
    private final MemberReceivingAccountService memberReceivingAccountService;
    private final JwtUtil jwtUtil;
    private final AuthProperties authProperties;
    private final SensitiveDataCipher sensitiveDataCipher;
    private final JointLoginTargetPageResolver targetPageResolver;

    public JointLoginServiceImpl(
            XiaohuaGatewayService xiaohuaGatewayService,
            MemberInfoRepository memberInfoRepository,
            MemberChannelRepository memberChannelRepository,
            MemberReceivingAccountService memberReceivingAccountService,
            JwtUtil jwtUtil,
            AuthProperties authProperties,
            SensitiveDataCipher sensitiveDataCipher,
            JointLoginTargetPageResolver targetPageResolver
    ) {
        this.xiaohuaGatewayService = xiaohuaGatewayService;
        this.memberInfoRepository = memberInfoRepository;
        this.memberChannelRepository = memberChannelRepository;
        this.memberReceivingAccountService = memberReceivingAccountService;
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
        String normalizedScene = normalizeScene(request.scene());
        String benefitOrderNo = resolveBenefitOrderNo(normalizedScene, request);
        String requestId = RequestIdUtil.nextId("xa");
        UserTokenResponse tokenResponse = validateJointToken(requestId, benefitOrderNo, request.token());
        String cid = requiredValue(tokenResponse.cid(), "JOINT_LOGIN_CID_REQUIRED", "Xiaohua cid is required");
        MemberInfo existingMember = memberInfoRepository.selectByCid(cid);
        String absUserId = resolveAbsUserId(existingMember);
        log.info("traceId={} requestId={} scene={} absUserId={} cid={} joint login local user resolved",
                TraceIdUtil.getTraceId(),
                requestId,
                normalizedScene,
                absUserId,
                cid);
        UserQueryResponse userQueryResponse = queryJointUser(requestId, benefitOrderNo, absUserId, cid);
        validateJointUserMapping(userQueryResponse.payload(), absUserId, cid);
        JointIdentity identity = mapToIdentity(tokenResponse, userQueryResponse);
        logIdCardStatus(requestId, normalizedScene, absUserId, identity);
        MemberInfo memberInfo = findOrCreateMember(existingMember, absUserId, identity);
        syncReceivingAccountIfPresent(requestId, normalizedScene, memberInfo.getMemberId(), identity.cid(), userQueryResponse.payload());
        ensureChannelBinding(memberInfo, identity.cid(), resolveChannelCode(identity.channelCode()));
        log.info("traceId={} requestId={} scene={} absUserId={} cid={} joint login mapping bound",
                TraceIdUtil.getTraceId(),
                requestId,
                normalizedScene,
                memberInfo.getMemberId(),
                identity.cid());
        String jwtToken = jwtUtil.generateToken(memberInfo.getMemberId(), identity.cid());
        return new JointLoginResult(
                jwtToken,
                normalizedScene,
                targetPageResolver.resolve(normalizedScene),
                benefitOrderNo,
                identity.cid(),
                true
        );
    }

    private String resolveBenefitOrderNo(String normalizedScene, JointLoginRequest request) {
        if ("push".equals(normalizedScene)) {
            return null;
        }
        String benefitOrderNo = firstNonBlank(request.benefitOrderNo(), request.orderNo());
        if (benefitOrderNo == null || benefitOrderNo.isBlank()) {
            throw new BizException(
                    "JOINT_LOGIN_BENEFIT_ORDER_REQUIRED",
                    "Benefit order number is required for " + normalizedScene + " scene"
            );
        }
        return benefitOrderNo;
    }

    private UserTokenResponse validateJointToken(String requestId, String benefitOrderNo, String token) {
        try {
            return xiaohuaGatewayService.validateUserToken(
                    requestId,
                    benefitOrderNo,
                    new UserTokenRequest(token)
            );
        } catch (UpstreamTimeoutException exception) {
            throw new BizException("JOINT_LOGIN_UPSTREAM_TIMEOUT", "Joint login temporarily unavailable");
        } catch (BizException exception) {
            if (isTokenInvalidReject(exception)) {
                throw new BizException("JOINT_LOGIN_TOKEN_INVALID", "Joint login session expired");
            }
            throw new BizException("JOINT_LOGIN_UPSTREAM_FAILED", "Joint login temporarily unavailable");
        }
    }

    private boolean isTokenInvalidReject(BizException exception) {
        if (!ErrorCodes.YUNKA_UPSTREAM_REJECTED.equals(exception.getErrorNo())) {
            return false;
        }
        String message = exception.getErrorMsg();
        if (message == null || message.isBlank()) {
            return false;
        }
        String normalized = message.toLowerCase(Locale.ROOT);
        return normalized.contains("token invalid")
                || normalized.contains("invalid token")
                || normalized.contains("token expired")
                || normalized.contains("session expired");
    }

    private UserQueryResponse queryJointUser(
            String requestId,
            String benefitOrderNo,
            String absUserId,
            String cid
    ) {
        try {
            log.info("traceId={} requestId={} scene=joint-login outboundField=userId absUserId={} cid={} joint user query begin",
                    TraceIdUtil.getTraceId(),
                    requestId,
                    absUserId,
                    cid);
            return xiaohuaGatewayService.queryUser(
                    requestId,
                    benefitOrderNo,
                    new UserQueryRequest(absUserId, cid)
            );
        } catch (UpstreamTimeoutException exception) {
            throw new BizException("JOINT_LOGIN_UPSTREAM_TIMEOUT", "Joint login temporarily unavailable");
        } catch (BizException exception) {
            throw new BizException("JOINT_LOGIN_UPSTREAM_FAILED", "Joint login temporarily unavailable");
        }
    }

    private String resolveAbsUserId(MemberInfo existingMember) {
        return existingMember != null ? existingMember.getMemberId() : RequestIdUtil.nextId("mem");
    }

    private void validateJointUserMapping(JsonNode payload, String expectedAbsUserId, String expectedCid) {
        String returnedAbsUserId = firstNonBlank(nullableText(payload, "userId"), nullableText(payload, "uid"));
        if (returnedAbsUserId != null && !returnedAbsUserId.equals(expectedAbsUserId)) {
            throw new BizException("JOINT_LOGIN_USER_ID_MISMATCH", "Joint login userId mapping is inconsistent");
        }
        String returnedCid = nullableText(payload, "cid");
        if (returnedCid != null && !returnedCid.equals(expectedCid)) {
            throw new BizException("JOINT_LOGIN_CID_MISMATCH", "Joint login cid mapping is inconsistent");
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
        String idCard = normalizeIdCard(identity.idCard(), false);
        String realName = normalizeRealName(identity.realName(), newMember);
        memberInfo.setTechPlatformUserId(identity.cid());
        memberInfo.setExternalUserId(identity.cid());
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
        ResolvedIdentityField resolvedIdCard = resolveIdCard(idInfo, payload);
        return new JointIdentity(
                requiredValue(tokenResponse.cid(), "JOINT_LOGIN_CID_REQUIRED", "Xiaohua cid is required"),
                firstNonBlank(tokenResponse.phone(), text(payload, "phone"), text(payload.path("basicInfo"), "phone")),
                firstNonBlank(tokenResponse.name(), text(idInfo, "name"), text(payload, "name")),
                resolvedIdCard.value(),
                resolvedIdCard.fieldName(),
                authProperties.getDefaultChannelCode()
        );
    }

    private ResolvedIdentityField resolveIdCard(JsonNode idInfo, JsonNode payload) {
        for (String fieldName : new String[]{"idno", "idCard", "idCardNo"}) {
            String value = text(idInfo, fieldName);
            if (value != null && !value.isBlank()) {
                return new ResolvedIdentityField(value, fieldName);
            }
        }
        for (String fieldName : new String[]{"idno", "idCard", "idCardNo"}) {
            String value = text(payload, fieldName);
            if (value != null && !value.isBlank()) {
                return new ResolvedIdentityField(value, fieldName);
            }
        }
        return new ResolvedIdentityField(null, null);
    }

    private void logIdCardStatus(String requestId, String scene, String absUserId, JointIdentity identity) {
        if (identity.idCard() == null || identity.idCard().isBlank()) {
            log.warn("traceId={} requestId={} absUserId={} cid={} scene={} temporarily allowing joint login without id card",
                    TraceIdUtil.getTraceId(),
                    requestId,
                    absUserId,
                    identity.cid(),
                    scene);
            return;
        }
        log.info("traceId={} requestId={} absUserId={} cid={} scene={} idCardField={} joint login id card resolved",
                TraceIdUtil.getTraceId(),
                requestId,
                absUserId,
                identity.cid(),
                scene,
                identity.idCardField());
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

    private void syncReceivingAccountIfPresent(
            String requestId,
            String scene,
            String memberId,
            String cid,
            JsonNode payload
    ) {
        ResolvedReceivingAccount receivingAccount = resolveReceivingAccount(payload);
        if (receivingAccount == null) {
            log.info("traceId={} requestId={} absUserId={} cid={} scene={} joint login receiving account absent, skip initialization",
                    TraceIdUtil.getTraceId(),
                    requestId,
                    memberId,
                    cid,
                    scene);
            return;
        }
        memberReceivingAccountService.upsertDefaultReceivingAccount(
                memberId,
                new MemberReceivingAccountService.UpsertCommand(
                        receivingAccount.accountId(),
                        receivingAccount.bankName(),
                        receivingAccount.lastFour(),
                        "JOINT_LOGIN"
                )
        );
        log.info("traceId={} requestId={} absUserId={} cid={} scene={} accountId={} joint login receiving account initialized",
                TraceIdUtil.getTraceId(),
                requestId,
                memberId,
                cid,
                scene,
                receivingAccount.accountId());
    }

    private ResolvedReceivingAccount resolveReceivingAccount(JsonNode payload) {
        JsonNode accountNode = firstObject(payload.path("receivingAccount"), payload.path("bankCard"), payload);
        String accountId = firstNonBlank(
                nullableText(accountNode, "accountId"),
                nullableText(accountNode, "bankCardNo"),
                nullableText(accountNode, "bankCardNum"),
                nullableText(accountNode, "cardId"),
                nullableText(accountNode, "cardNo")
        );
        String bankName = firstNonBlank(
                nullableText(accountNode, "bankName"),
                nullableText(accountNode, "bank")
        );
        String lastFour = firstNonBlank(
                nullableText(accountNode, "lastFour"),
                nullableText(accountNode, "cardLastFour"),
                nullableText(accountNode, "cardNoTail"),
                nullableText(accountNode, "cardTail")
        );
        if (lastFour == null && accountId != null && accountId.length() >= 4) {
            lastFour = accountId.substring(accountId.length() - 4);
        }
        if (accountId == null || bankName == null || lastFour == null) {
            return null;
        }
        return new ResolvedReceivingAccount(accountId, bankName, lastFour);
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

    private String nullableText(JsonNode node, String fieldName) {
        String value = text(node, fieldName);
        return value == null || value.isBlank() ? null : value;
    }

    private JsonNode firstObject(JsonNode... candidates) {
        for (JsonNode candidate : candidates) {
            if (candidate != null && !candidate.isMissingNode() && !candidate.isNull()) {
                return candidate;
            }
        }
        return com.fasterxml.jackson.databind.node.MissingNode.getInstance();
    }

    private record JointIdentity(
            String cid,
            String phone,
            String realName,
            String idCard,
            String idCardField,
            String channelCode
    ) {
    }

    private record ResolvedIdentityField(
            String value,
            String fieldName
    ) {
    }

    private record ResolvedReceivingAccount(
            String accountId,
            String bankName,
            String lastFour
    ) {
    }
}
