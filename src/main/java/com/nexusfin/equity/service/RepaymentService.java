package com.nexusfin.equity.service;

import com.nexusfin.equity.dto.request.RepaymentSubmitRequest;
import com.nexusfin.equity.dto.response.RepaymentInfoResponse;
import com.nexusfin.equity.dto.response.RepaymentResultResponse;
import com.nexusfin.equity.dto.response.RepaymentSubmitResponse;

public interface RepaymentService {

    RepaymentInfoResponse getInfo(String uid, String loanId);

    RepaymentSubmitResponse submit(String uid, RepaymentSubmitRequest request);

    RepaymentResultResponse getResult(String uid, String repaymentId);
}
