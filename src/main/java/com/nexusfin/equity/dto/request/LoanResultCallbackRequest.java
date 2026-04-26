package com.nexusfin.equity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record LoanResultCallbackRequest(
        @NotBlank String requestId,
        @NotBlank String userId,
        String cid,
        String benefitOrderNo,
        String platformBenefitOrderNo,
        @NotBlank String loanId,
        @NotNull Integer status,
        String remark,
        @PositiveOrZero Long loanAmount,
        @PositiveOrZero Long repayAmount,
        Long loanDate,
        Integer settleRuleType,
        Integer lockDays,
        String serviceChargeRule
) {

    public String bizOrderNo() {
        return firstNonBlank(benefitOrderNo, platformBenefitOrderNo, loanId);
    }

    public String resolvedBenefitOrderNo() {
        return firstNonBlank(benefitOrderNo, platformBenefitOrderNo);
    }

    public String loanOrderNo() {
        return loanId;
    }

    public Long actualAmount() {
        return loanAmount;
    }

    public boolean isSuccess() {
        return Integer.valueOf(7001).equals(status);
    }

    public boolean isProcessing() {
        return Integer.valueOf(7002).equals(status);
    }

    public boolean isFailure() {
        return Integer.valueOf(7003).equals(status);
    }

    public String normalizedGrantStatus() {
        if (isSuccess()) {
            return "SUCCESS";
        }
        if (isProcessing()) {
            return "PROCESSING";
        }
        if (isFailure()) {
            return "FAIL";
        }
        return status == null ? "" : String.valueOf(status);
    }

    public String idempotencyStatus() {
        return status == null ? "" : String.valueOf(status);
    }

    private static String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) {
                return candidate;
            }
        }
        return "";
    }
}
