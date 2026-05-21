package com.nexusfin.equity.service;

import com.nexusfin.equity.entity.LoanApplicationMapping;

public interface LoanApplicationGateway {

    void save(SaveCommand command);

    LoanApplicationMapping findLatestPendingMapping(String memberId);

    LoanApplicationMapping findActiveOrPendingMapping(String memberId, String applicationId);

    record SaveCommand(
            String memberId,
            String externalUserId,
            String applicationId,
            String benefitOrderNo,
            Integer platformLoanId,
            String purpose,
            String mappingStatus
    ) {
    }
}
