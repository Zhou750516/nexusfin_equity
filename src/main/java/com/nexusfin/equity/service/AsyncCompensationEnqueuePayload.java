package com.nexusfin.equity.service;

public sealed interface AsyncCompensationEnqueuePayload
        permits AsyncCompensationEnqueuePayload.YunkaLoanApplyRetry,
                AsyncCompensationEnqueuePayload.QwBenefitPurchaseRetry {

    record YunkaLoanApplyRetry(
            String requestId,
            String path,
            String bizOrderNo,
            String uid,
            String benefitOrderNo,
            String applyId,
            String loanId,
            Long loanAmount,
            Integer loanPeriod,
            String bankCardNo
    ) implements AsyncCompensationEnqueuePayload {
    }

    record QwBenefitPurchaseRetry(
            String externalUserId,
            String benefitOrderNo,
            String productCode,
            Long loanAmount
    ) implements AsyncCompensationEnqueuePayload {
    }
}
