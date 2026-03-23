package com.nexusfin.equity.service;

import com.nexusfin.equity.dto.request.ExerciseCallbackRequest;
import com.nexusfin.equity.dto.request.GrantForwardCallbackRequest;
import com.nexusfin.equity.dto.request.RepaymentForwardCallbackRequest;
import com.nexusfin.equity.dto.request.RefundCallbackRequest;

public interface NotificationService {

    void handleGrant(GrantForwardCallbackRequest request);

    void handleRepayment(RepaymentForwardCallbackRequest request);

    void handleExercise(ExerciseCallbackRequest request);

    void handleRefund(RefundCallbackRequest request);
}
