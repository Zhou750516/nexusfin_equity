package com.nexusfin.equity.service;

import com.fasterxml.jackson.databind.JsonNode;

public sealed interface AsyncCompensationEnqueuePayload
        permits AsyncCompensationEnqueuePayload.YunkaLoanApplyRetry,
                AsyncCompensationEnqueuePayload.QwBenefitPurchaseRetry {

    record YunkaLoanApplyRetry(
            String requestId,
            String path,
            String bizOrderNo,
            String memberId,
            String uid,
            String benefitOrderNo,
            String applyId,
            Integer loanId,
            Long loanAmount,
            Integer loanPeriod,
            String bankCardNo,
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
    ) implements AsyncCompensationEnqueuePayload {
    }

    record QwBenefitPurchaseRetry(
            String externalUserId,
            String benefitOrderNo,
            String productCode,
            Long loanAmount,
            Long benefitAmount,
            Long userSignId
    ) implements AsyncCompensationEnqueuePayload {
    }
}
