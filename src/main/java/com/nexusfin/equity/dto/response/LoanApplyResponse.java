package com.nexusfin.equity.dto.response;

public record LoanApplyResponse(
        String applicationId,
        String status,
        String estimatedTime,
        boolean benefitsActivated,
        String benefitOrderNo,
        String message
) {
}
