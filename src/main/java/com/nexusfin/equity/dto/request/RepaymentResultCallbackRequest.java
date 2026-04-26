package com.nexusfin.equity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record RepaymentResultCallbackRequest(
        @NotBlank String requestId,
        @NotBlank String userId,
        String cid,
        String benefitOrderNo,
        String platformBenefitOrderNo,
        @NotBlank String loanId,
        String swiftNumber,
        @NotNull Integer status,
        @NotNull Integer repaymentType,
        @NotBlank String periods,
        String remark,
        @PositiveOrZero Long amount,
        Long successTime,
        @PositiveOrZero Long discount,
        @PositiveOrZero Long originalRepay
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
