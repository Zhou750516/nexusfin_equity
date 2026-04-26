package com.nexusfin.equity.service;

import com.nexusfin.equity.dto.request.ExerciseCallbackRequest;
import com.nexusfin.equity.dto.request.LoanResultCallbackRequest;
import com.nexusfin.equity.dto.request.RepaymentResultCallbackRequest;
import com.nexusfin.equity.dto.request.RefundCallbackRequest;

public interface NotificationService {

    void handleGrant(LoanResultCallbackRequest request);

    void handleRepayment(RepaymentResultCallbackRequest request);

    void handleExercise(ExerciseCallbackRequest request);

    void handleRefund(RefundCallbackRequest request);
}
