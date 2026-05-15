package com.nexusfin.equity.thirdparty.qw;

public interface QwBenefitClient {

    QwMemberSyncResponse syncMemberOrder(QwMemberSyncRequest request);

    QwExerciseUrlResponse getExerciseUrl(QwExerciseUrlRequest request);

    QwDeductionNotifyResponse notifyDeduction(QwDeductionNotifyRequest request);

    QwDeductionQueryResponse queryDeduction(QwDeductionQueryRequest request);

    QwOrderCancelResponse cancelOrder(QwOrderCancelRequest request);

    QwSignStatusResponse querySignStatus(QwSignStatusRequest request);

    QwSignApplyResponse applySign(QwSignApplyRequest request);

    QwSignConfirmResponse confirmSign(QwSignConfirmRequest request);
}
