package com.nexusfin.equity.service;

import com.nexusfin.equity.dto.request.LoanApplyRequest;
import com.nexusfin.equity.dto.response.LoanApplyResponse;

public interface LoanApplicationService {

    LoanApplyResponse apply(String memberId, String uid, LoanApplyRequest request);
}
