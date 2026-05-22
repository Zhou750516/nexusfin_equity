package com.nexusfin.equity.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.nexusfin.equity.config.H5BenefitsProperties;
import com.nexusfin.equity.config.H5LoanProperties;
import com.nexusfin.equity.config.YunkaProperties;
import com.nexusfin.equity.dto.request.CreateBenefitOrderRequest;
import com.nexusfin.equity.dto.request.LoanApplyRequest;
import com.nexusfin.equity.dto.response.CreateBenefitOrderResponse;
import com.nexusfin.equity.dto.response.LoanApplyResponse;
import com.nexusfin.equity.exception.BenefitPurchaseSyncTimeoutCompensationException;
import com.nexusfin.equity.exception.BizException;
import com.nexusfin.equity.exception.ErrorCodes;
import com.nexusfin.equity.exception.UpstreamTimeoutException;
import com.nexusfin.equity.entity.LoanApplicationMapping;
import com.nexusfin.equity.entity.MemberInfo;
import com.nexusfin.equity.repository.MemberInfoRepository;
import com.nexusfin.equity.service.AsyncCompensationEnqueueService;
import com.nexusfin.equity.service.AsyncCompensationEnqueuePayload;
import com.nexusfin.equity.service.BenefitOrderService;
import com.nexusfin.equity.service.H5I18nService;
import com.nexusfin.equity.service.LoanApplicationGateway;
import com.nexusfin.equity.service.LoanApplicationService;
import com.nexusfin.equity.service.MemberReceivingAccountService;
import com.nexusfin.equity.service.support.YunkaCallTemplate;
import com.nexusfin.equity.thirdparty.yunka.CreditImageQueryRequest;
import com.nexusfin.equity.thirdparty.yunka.UserQueryRequest;
import com.nexusfin.equity.thirdparty.yunka.YunkaGatewayClient;
import com.nexusfin.equity.util.SensitiveDataCipher;
import com.nexusfin.equity.util.TraceIdUtil;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.nexusfin.equity.util.BizIds.next;
import static com.nexusfin.equity.util.BizIds.newLoanId;
import static com.nexusfin.equity.util.JsonNodes.readRemark;
import static com.nexusfin.equity.util.LoanInputValidator.validateAmountAndTerm;
import static com.nexusfin.equity.util.MoneyUnits.yuanToCent;

@Service
public class LoanApplicationServiceImpl implements LoanApplicationService {

    private static final Logger log = LoggerFactory.getLogger(LoanApplicationServiceImpl.class);
    private static final String CREDIT_IMAGE_TYPES = "back,front,nature";

    private final H5LoanProperties h5LoanProperties;
    private final H5BenefitsProperties h5BenefitsProperties;
    private final YunkaProperties yunkaProperties;
    private final LoanApplicationGateway loanApplicationGateway;
    private final MemberInfoRepository memberInfoRepository;
    private final BenefitOrderService benefitOrderService;
    private final H5I18nService h5I18nService;
    private final MemberReceivingAccountService memberReceivingAccountService;
    private final AsyncCompensationEnqueueService asyncCompensationEnqueueService;
    private final SensitiveDataCipher sensitiveDataCipher;
    private final YunkaCallTemplate yunkaCallTemplate;

    public LoanApplicationServiceImpl(
            H5LoanProperties h5LoanProperties,
            H5BenefitsProperties h5BenefitsProperties,
            YunkaProperties yunkaProperties,
            LoanApplicationGateway loanApplicationGateway,
            MemberInfoRepository memberInfoRepository,
            BenefitOrderService benefitOrderService,
            H5I18nService h5I18nService,
            MemberReceivingAccountService memberReceivingAccountService,
            AsyncCompensationEnqueueService asyncCompensationEnqueueService,
            SensitiveDataCipher sensitiveDataCipher,
            YunkaCallTemplate yunkaCallTemplate
    ) {
        this.h5LoanProperties = h5LoanProperties;
        this.h5BenefitsProperties = h5BenefitsProperties;
        this.yunkaProperties = yunkaProperties;
        this.loanApplicationGateway = loanApplicationGateway;
        this.memberInfoRepository = memberInfoRepository;
        this.benefitOrderService = benefitOrderService;
        this.h5I18nService = h5I18nService;
        this.memberReceivingAccountService = memberReceivingAccountService;
        this.asyncCompensationEnqueueService = asyncCompensationEnqueueService;
        this.sensitiveDataCipher = sensitiveDataCipher;
        this.yunkaCallTemplate = yunkaCallTemplate;
    }

    @Override
    @Transactional(noRollbackFor = BenefitPurchaseSyncTimeoutCompensationException.class)
    public LoanApplyResponse apply(String memberId, String externalUserId, LoanApplyRequest request) {
        validateApplyRequest(memberId, request);
        LoanApplicationMapping pendingMapping = loanApplicationGateway.findLatestPendingMapping(memberId);
        if (pendingMapping != null) {
            return buildPendingResponse(
                    pendingMapping.getApplicationId(),
                    pendingMapping.getBenefitOrderNo(),
                    h5I18nService.text("loan.apply.pendingReview", "借款申请已提交，正在审核中")
            );
        }
        String applicationId = next("APP");
        Integer loanId = newLoanId();
        CreateBenefitOrderResponse benefitOrder = benefitOrderService.createLocalOrder(
                memberId,
                new CreateBenefitOrderRequest(
                        "loan-apply-" + applicationId,
                        h5BenefitsProperties.productCode(),
                        yuanToCent(request.orderAmount()),
                        Boolean.TRUE
                )
        );
        YunkaGatewayClient.YunkaGatewayResponse response;
        String requestId = next("LA");
        String upstreamBankCardNum = resolveBankCardNum(request);
        JsonNode userProfile;
        LoanApplyIdentity identity;
        JsonNode imageInfo;
        try {
            userProfile = queryRequiredUserProfile(
                    memberId,
                    externalUserId,
                    applicationId,
                    benefitOrder.benefitOrderNo()
            );
            identity = resolveLoanApplyIdentity(memberId, userProfile);
            imageInfo = queryRequiredImageInfo(memberId, applicationId, benefitOrder.benefitOrderNo());
        } catch (BizException exception) {
            logLoanApplyPreflightFailed(applicationId, benefitOrder.benefitOrderNo(),
                    exception.getErrorNo(), exception.getErrorMsg());
            return buildLoanFailedResponse(applicationId, benefitOrder.benefitOrderNo(), exception.getMessage());
        } catch (RuntimeException exception) {
            logLoanApplyPreflightFailed(applicationId, benefitOrder.benefitOrderNo(),
                    ErrorCodes.YUNKA_UPSTREAM_FAILED, exception.getMessage());
            return buildLoanFailedResponse(applicationId, benefitOrder.benefitOrderNo(), exception.getMessage());
        }
        LoanApplyForwardData forwardData = new LoanApplyForwardData(
                memberId,
                benefitOrder.benefitOrderNo(),
                applicationId,
                applicationId,
                loanId,
                BigDecimal.valueOf(request.amount()).setScale(2),
                request.term(),
                upstreamBankCardNum,
                upstreamBankCardNum,
                identity.phone(),
                identity.idno(),
                identity.name(),
                toLoanReason(request.purpose()),
                requiredNode(userProfile, "basicInfo", "LOAN_APPLY_USER_PROFILE_INCOMPLETE"),
                requiredNode(userProfile, "idInfo", "LOAN_APPLY_USER_PROFILE_INCOMPLETE"),
                requiredArray(userProfile, "contactInfo", "LOAN_APPLY_USER_PROFILE_INCOMPLETE"),
                requiredNode(userProfile, "supplementInfo", "LOAN_APPLY_USER_PROFILE_INCOMPLETE"),
                requiredNode(userProfile, "optionInfo", "LOAN_APPLY_USER_PROFILE_INCOMPLETE"),
                imageInfo
        );
        try {
            response = yunkaCallTemplate.execute(
                    YunkaCallTemplate.YunkaCall.of(
                            "loan apply",
                            requestId,
                            yunkaProperties.paths().loanApply(),
                            applicationId,
                            forwardData
                    ).withMemberId(memberId).withBenefitOrderNo(benefitOrder.benefitOrderNo()),
                    gatewayResponse -> {
                        YunkaGatewayClient.YunkaGatewayResponse presentResponse =
                                yunkaCallTemplate.requirePresentResponse(gatewayResponse);
                        if (!yunkaCallTemplate.isSuccessful(presentResponse)) {
                            throw new BizException(ErrorCodes.YUNKA_UPSTREAM_REJECTED, presentResponse.message());
                        }
                        return presentResponse;
                    }
            );
        } catch (UpstreamTimeoutException exception) {
            loanApplicationGateway.save(new LoanApplicationGateway.SaveCommand(
                    memberId,
                    externalUserId,
                    applicationId,
                    benefitOrder.benefitOrderNo(),
                    loanId,
                    request.purpose(),
                    "PENDING_REVIEW"
            ));
            asyncCompensationEnqueueService.enqueue(new AsyncCompensationEnqueueService.EnqueueCommand(
                    "YUNKA_LOAN_APPLY_RETRY",
                    "LOAN_APPLY:" + applicationId,
                    applicationId,
                    "YUNKA",
                    yunkaProperties.gatewayPath(),
                    "POST",
                    null,
                    new AsyncCompensationEnqueuePayload.YunkaLoanApplyRetry(
                            requestId,
                            yunkaProperties.paths().loanApply(),
                            applicationId,
                            memberId,
                            externalUserId,
                            benefitOrder.benefitOrderNo(),
                            applicationId,
                            loanId,
                            yuanToCent(request.amount()),
                            request.term(),
                            upstreamBankCardNum,
                            forwardData.phone(),
                            forwardData.idno(),
                            forwardData.name(),
                            forwardData.loanReason(),
                            forwardData.basicInfo(),
                            forwardData.idInfo(),
                            forwardData.contactInfo(),
                            forwardData.supplementInfo(),
                            forwardData.optionInfo(),
                            forwardData.imageInfo()
                    )
            ));
            return buildPendingResponse(
                    applicationId,
                    benefitOrder.benefitOrderNo(),
                    h5I18nService.text("loan.apply.pendingReview", "借款申请已提交，正在审核中")
            );
        } catch (BizException exception) {
            return buildLoanFailedResponse(
                    applicationId,
                    benefitOrder.benefitOrderNo(),
                    (ErrorCodes.YUNKA_RESPONSE_EMPTY.equals(exception.getErrorNo())
                            || ErrorCodes.YUNKA_UPSTREAM_REJECTED.equals(exception.getErrorNo()))
                            ? exception.getErrorMsg()
                            : exception.getMessage()
            );
        } catch (RuntimeException exception) {
            return buildLoanFailedResponse(applicationId, benefitOrder.benefitOrderNo(), exception.getMessage());
        }
        Integer platformLoanId = readInt(response.data(), "loanId", loanId);
        loanApplicationGateway.save(new LoanApplicationGateway.SaveCommand(
                memberId,
                externalUserId,
                applicationId,
                benefitOrder.benefitOrderNo(),
                platformLoanId,
                request.purpose(),
                "ACTIVE"
        ));
        return buildPendingResponse(
                applicationId,
                benefitOrder.benefitOrderNo(),
                readRemark(response.data(), "借款申请已提交，正在处理中")
        );
    }

    private void validateApplyRequest(String memberId, LoanApplyRequest request) {
        validateAmountAndTerm(h5LoanProperties, request.amount(), request.term());
        memberReceivingAccountService.getReceivingAccount(memberId, request.receivingAccountId());
    }

    private LoanApplyResponse buildLoanFailedResponse(String applicationId, String benefitOrderNo, String reason) {
        String safeReason = reason == null || reason.isBlank() ? "Yunka gateway response is empty" : reason;
        return new LoanApplyResponse(
                null,
                "loan_failed",
                null,
                true,
                benefitOrderNo,
                h5I18nService.text("loan.apply.failurePrefix", "权益购买成功，借款申请失败：") + safeReason
        );
    }

    private LoanApplyResponse buildPendingResponse(String applicationId, String benefitOrderNo, String message) {
        return new LoanApplyResponse(
                applicationId,
                "pending",
                h5I18nService.text("loan.approval.arrivalTime", "30分钟"),
                true,
                benefitOrderNo,
                message
        );
    }

    private void logLoanApplyPreflightFailed(
            String applicationId,
            String benefitOrderNo,
            String errorNo,
            String errorMsg
    ) {
        log.warn("traceId={} bizOrderNo={} benefitOrderNo={} errorNo={} errorMsg={} loan apply required data missing",
                TraceIdUtil.getTraceId(),
                applicationId,
                benefitOrderNo,
                errorNo,
                errorMsg);
    }

    private String resolveBankCardNum(LoanApplyRequest request) {
        if (hasText(request.bankCardNum())) {
            return request.bankCardNum();
        }
        return request.receivingAccountId();
    }

    private JsonNode queryRequiredUserProfile(
            String memberId,
            String externalUserId,
            String applicationId,
            String benefitOrderNo
    ) {
        JsonNode data = yunkaCallTemplate.executeForData(
                YunkaCallTemplate.YunkaCall.of(
                        "loan apply user query",
                        next("LAU"),
                        yunkaProperties.paths().userQuery(),
                        applicationId,
                        new UserQueryRequest(memberId, externalUserId)
                ).withMemberId(memberId).withBenefitOrderNo(benefitOrderNo)
        );
        validateUserProfile(data);
        return data;
    }

    private JsonNode queryRequiredImageInfo(String memberId, String applicationId, String benefitOrderNo) {
        JsonNode data = yunkaCallTemplate.executeForData(
                YunkaCallTemplate.YunkaCall.of(
                        "loan apply credit image query",
                        next("LAI"),
                        yunkaProperties.paths().creditImageQuery(),
                        applicationId,
                        new CreditImageQueryRequest(memberId, CREDIT_IMAGE_TYPES)
                ).withMemberId(memberId).withBenefitOrderNo(benefitOrderNo)
        );
        JsonNode imageInfo = toLoanApplyImageInfo(data);
        if (!imageInfo.isArray() || imageInfo.isEmpty()) {
            throw new BizException("LOAN_APPLY_IMAGE_INFO_MISSING",
                    "credit image query data is required for loan apply");
        }
        return imageInfo;
    }

    private void validateUserProfile(JsonNode data) {
        requiredNode(data, "basicInfo", "LOAN_APPLY_USER_PROFILE_INCOMPLETE");
        requiredNode(data, "idInfo", "LOAN_APPLY_USER_PROFILE_INCOMPLETE");
        requiredArray(data, "contactInfo", "LOAN_APPLY_USER_PROFILE_INCOMPLETE");
        requiredNode(data, "supplementInfo", "LOAN_APPLY_USER_PROFILE_INCOMPLETE");
        requiredNode(data, "optionInfo", "LOAN_APPLY_USER_PROFILE_INCOMPLETE");
    }

    private LoanApplyIdentity resolveLoanApplyIdentity(String memberId, JsonNode userProfile) {
        LocalMemberIdentity localIdentity = loadLocalMemberIdentity(memberId);
        LoanApplyIdentity identity = new LoanApplyIdentity(
                firstNonBlank(resolvePhone(userProfile), localIdentity.phone()),
                firstNonBlank(resolveIdNo(userProfile), localIdentity.idno()),
                firstNonBlank(resolveName(userProfile), localIdentity.name())
        );
        if (!hasText(identity.phone()) || !hasText(identity.idno()) || !hasText(identity.name())) {
            throw new BizException("LOAN_APPLY_USER_PROFILE_INCOMPLETE",
                    "loan apply profile is missing required name/idno/phone after local fallback");
        }
        return identity;
    }

    private LocalMemberIdentity loadLocalMemberIdentity(String memberId) {
        MemberInfo memberInfo = memberInfoRepository.selectById(memberId);
        if (memberInfo == null) {
            return LocalMemberIdentity.EMPTY;
        }
        return new LocalMemberIdentity(
                decryptOptional(memberInfo.getMobileEncrypted()),
                decryptOptional(memberInfo.getIdCardEncrypted()),
                decryptOptional(memberInfo.getRealNameEncrypted())
        );
    }

    private String decryptOptional(String ciphertext) {
        if (!hasText(ciphertext)) {
            return "";
        }
        String plaintext = sensitiveDataCipher.decrypt(ciphertext);
        return hasText(plaintext) ? plaintext : "";
    }

    private JsonNode requiredNode(JsonNode data, String fieldName, String errorNo) {
        JsonNode node = data == null ? null : data.path(fieldName);
        if (node == null || node.isMissingNode() || node.isNull()
                || (node.isObject() && node.isEmpty())) {
            throw new BizException(errorNo, fieldName + " is required for loan apply");
        }
        return node;
    }

    private JsonNode requiredArray(JsonNode data, String fieldName, String errorNo) {
        JsonNode node = requiredNode(data, fieldName, errorNo);
        if (!node.isArray() || node.isEmpty()) {
            throw new BizException(errorNo, fieldName + " is required for loan apply");
        }
        return node;
    }

    private JsonNode toLoanApplyImageInfo(JsonNode imageData) {
        if (imageData == null || imageData.isMissingNode() || imageData.isNull()) {
            return JsonNodeFactory.instance.arrayNode();
        }
        if (imageData.isArray()) {
            return imageData;
        }
        JsonNode arrayCandidate = firstExisting(imageData, "imageInfo", "list", "images");
        if (arrayCandidate != null && arrayCandidate.isArray()) {
            return arrayCandidate;
        }
        String back = firstText(imageData, "back");
        String front = firstText(imageData, "front");
        String nature = firstText(imageData, "nature");
        if (!hasText(back) && !hasText(front) && !hasText(nature)) {
            return JsonNodeFactory.instance.arrayNode();
        }
        if (!hasText(back) || !hasText(front) || !hasText(nature)) {
            throw new BizException("LOAN_APPLY_IMAGE_INFO_MISSING",
                    "back/front/nature images are required for loan apply");
        }
        ArrayNode imageInfo = JsonNodeFactory.instance.arrayNode();
        imageInfo.add(JsonNodeFactory.instance.objectNode()
                .put("back", back)
                .put("front", front)
                .put("nature", nature)
                .put("type", CREDIT_IMAGE_TYPES));
        return imageInfo;
    }

    private JsonNode firstExisting(JsonNode data, String... fields) {
        for (String field : fields) {
            JsonNode value = data.path(field);
            if (!value.isMissingNode() && !value.isNull()) {
                return value;
            }
        }
        return null;
    }

    private String resolvePhone(JsonNode userProfile) {
        String phone = firstText(userProfile, "phone", "mobile", "userPhone");
        if (hasText(phone)) {
            return phone;
        }
        return firstText(userProfile.path("idInfo"), "phone", "mobile");
    }

    private String resolveIdNo(JsonNode userProfile) {
        return firstText(userProfile.path("idInfo"), "idno", "idNo", "idCardNo");
    }

    private String resolveName(JsonNode userProfile) {
        return firstText(userProfile.path("idInfo"), "name");
    }

    private String firstText(JsonNode data, String... fields) {
        if (data == null || data.isMissingNode() || data.isNull()) {
            return "";
        }
        for (String field : fields) {
            JsonNode value = data.path(field);
            if (!value.isMissingNode() && !value.isNull()) {
                String text = value.asText("");
                if (hasText(text)) {
                    return text;
                }
            }
        }
        return "";
    }

    private String firstNonBlank(String primary, String fallback) {
        return hasText(primary) ? primary : fallback;
    }

    private String toLoanReason(String purpose) {
        if (!hasText(purpose)) {
            return "70010";
        }
        return switch (purpose.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "shopping", "rent" -> "70006";
            case "working_capital", "turnover", "capital_turnover" -> "70007";
            case "education" -> "70005";
            case "decoration" -> "70011";
            case "travel" -> "70012";
            case "medical" -> "70014";
            case "other" -> "70010";
            default -> "70010";
        };
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record LoanApplyForwardData(
            String userId,
            String benefitOrderNo,
            String platformBenefitOrderNo,
            String applyId,
            Integer loanId,
            BigDecimal loanAmount,
            Integer loanPeriod,
            String bankCardNo,
            String bankCardNum,
            String phone,
            String idno,
            String name,
            String loanReason,
            JsonNode basicInfo,
            JsonNode idInfo,
            JsonNode contactInfo,
            JsonNode supplementInfo,
            JsonNode optionInfo,
            JsonNode imageInfo
    ) {
    }

    private record LoanApplyIdentity(
            String phone,
            String idno,
            String name
    ) {
    }

    private record LocalMemberIdentity(
            String phone,
            String idno,
            String name
    ) {
        private static final LocalMemberIdentity EMPTY = new LocalMemberIdentity("", "", "");
    }

    private Integer readInt(JsonNode data, String fieldName, Integer fallback) {
        JsonNode value = data == null ? null : data.path(fieldName);
        if (value == null || value.isMissingNode() || value.isNull()) {
            return fallback;
        }
        if (value.isInt() || value.isLong()) {
            return value.asInt();
        }
        String text = value.asText("");
        if (text.isBlank()) {
            return fallback;
        }
        try {
            return Integer.valueOf(text);
        } catch (NumberFormatException exception) {
            throw new BizException("YUNKA_RESPONSE_INVALID", "Yunka loan apply response loanId is invalid");
        }
    }
}
