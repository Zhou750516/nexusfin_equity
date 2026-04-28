package com.nexusfin.equity.service;

import com.nexusfin.equity.entity.LoanApplicationMapping;

public interface LoanApplicationGateway {

    void save(SaveCommand command);

    LoanApplicationMapping findActiveOrPendingMapping(String memberId, String applicationId);

    record SaveCommand(
            String memberId,
            String externalUserId,
            String applicationId,
            String benefitOrderNo,
            String upstreamLoanId,
            String purpose,
            String mappingStatus
    ) {
    }
}
