package com.nexusfin.equity.thirdparty.qw;

public interface QwBenefitClient {

    QwMemberSyncResponse syncMemberOrder(QwMemberSyncRequest request);

    QwExerciseUrlResponse getExerciseUrl(QwExerciseUrlRequest request);

    QwLendingNotifyResponse notifyLending(QwLendingNotifyRequest request);

    QwSignStatusResponse querySignStatus(QwSignStatusRequest request);

    QwSignApplyResponse applySign(QwSignApplyRequest request);

    QwSignConfirmResponse confirmSign(QwSignConfirmRequest request);
}
