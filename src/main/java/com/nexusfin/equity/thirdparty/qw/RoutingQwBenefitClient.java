package com.nexusfin.equity.thirdparty.qw;

import com.nexusfin.equity.config.QwProperties;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
public class RoutingQwBenefitClient implements QwBenefitClient {

    private final QwProperties properties;
    private final QwBenefitClientImpl qweimobileClient;
    private final AllinpayDirectQwBenefitClient allinpayDirectClient;

    public RoutingQwBenefitClient(
            QwProperties properties,
            QwBenefitClientImpl qweimobileClient,
            AllinpayDirectQwBenefitClient allinpayDirectClient
    ) {
        this.properties = properties;
        this.qweimobileClient = qweimobileClient;
        this.allinpayDirectClient = allinpayDirectClient;
    }

    @Override
    public QwMemberSyncResponse syncMemberOrder(QwMemberSyncRequest request) {
        return currentDelegate().syncMemberOrder(request);
    }

    @Override
    public QwExerciseUrlResponse getExerciseUrl(QwExerciseUrlRequest request) {
        return currentDelegate().getExerciseUrl(request);
    }

    @Override
    public QwLendingNotifyResponse notifyLending(QwLendingNotifyRequest request) {
        return currentDelegate().notifyLending(request);
    }

    @Override
    public QwSignStatusResponse querySignStatus(QwSignStatusRequest request) {
        return qweimobileClient.querySignStatus(request);
    }

    @Override
    public QwSignApplyResponse applySign(QwSignApplyRequest request) {
        return qweimobileClient.applySign(request);
    }

    @Override
    public QwSignConfirmResponse confirmSign(QwSignConfirmRequest request) {
        return qweimobileClient.confirmSign(request);
    }

    private QwBenefitClient currentDelegate() {
        return switch (properties.getMode()) {
            case ALLINPAY_DIRECT -> allinpayDirectClient;
            case MOCK, HTTP, QWEIMOBILE_HTTP -> qweimobileClient;
        };
    }
}
