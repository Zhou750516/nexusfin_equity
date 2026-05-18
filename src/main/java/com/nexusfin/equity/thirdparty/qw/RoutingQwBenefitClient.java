package com.nexusfin.equity.thirdparty.qw;

import com.nexusfin.equity.config.QwProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
public class RoutingQwBenefitClient implements QwBenefitClient {

    private static final Logger log = LoggerFactory.getLogger(RoutingQwBenefitClient.class);

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
        log.info("qw benefit client routing initialized mode={} businessDelegate={} signDelegate=qweimobileClient",
                properties.getMode(),
                currentBusinessDelegateName());
        log.info("qw runtime configuration qw_mode={} qw_base_url={} qw_method_path={} qw_partner_no={} "
                        + "qw_sign_key={} qw_aes_key={} businessDelegate={} signDelegate=qweimobileClient",
                properties.getMode(),
                properties.getHttp().getBaseUrl(),
                properties.getHttp().getMethodPath(),
                properties.getPartnerNo(),
                configuredState(properties.getSecurity().getSignKey()),
                configuredState(currentAesKeyValue()),
                currentBusinessDelegateName());
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
    public QwDeductionNotifyResponse notifyDeduction(QwDeductionNotifyRequest request) {
        return currentDelegate().notifyDeduction(request);
    }

    @Override
    public QwDeductionQueryResponse queryDeduction(QwDeductionQueryRequest request) {
        return currentDelegate().queryDeduction(request);
    }

    @Override
    public QwOrderCancelResponse cancelOrder(QwOrderCancelRequest request) {
        return currentDelegate().cancelOrder(request);
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

    private String currentBusinessDelegateName() {
        return switch (properties.getMode()) {
            case ALLINPAY_DIRECT -> "allinpayDirectClient";
            case MOCK, HTTP, QWEIMOBILE_HTTP -> "qweimobileClient";
        };
    }

    private String currentAesKeyValue() {
        return switch (properties.getSecurity().getAesKeyEncoding()) {
            case RAW -> properties.getSecurity().getAesKey();
            case BASE64 -> properties.getSecurity().getAesKeyBase64();
        };
    }

    private String configuredState(String value) {
        return value == null || value.isBlank() ? "missing" : "configured";
    }
}
