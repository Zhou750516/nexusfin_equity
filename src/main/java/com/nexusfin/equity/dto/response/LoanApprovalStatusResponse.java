package com.nexusfin.equity.dto.response;

import java.util.List;

public record LoanApprovalStatusResponse(
        String applicationId,
        String status,
        String purpose,
        List<ApprovalStep> steps,
        BenefitsCardPreview benefitsCard
) {

    public record ApprovalStep(
            String name,
            String status,
            String description
    ) {
    }

    public record BenefitsCardPreview(
            boolean available,
            Long price,
            List<String> features
    ) {
    }
}
