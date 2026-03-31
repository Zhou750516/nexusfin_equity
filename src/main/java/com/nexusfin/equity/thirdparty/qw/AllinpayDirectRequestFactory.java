package com.nexusfin.equity.thirdparty.qw;

import com.nexusfin.equity.config.QwProperties;
import com.nexusfin.equity.exception.BizException;
import java.net.URI;

public class AllinpayDirectRequestFactory {

    private final QwProperties properties;

    public AllinpayDirectRequestFactory(QwProperties properties) {
        this.properties = properties;
    }

    public AllinpayDirectInvocation prepareMemberSync(QwMemberSyncRequest request) {
        return buildInvocation(
                AllinpayDirectOperation.MEMBER_SYNC,
                properties.getDirect().getMemberSyncServiceCode(),
                request
        );
    }

    public AllinpayDirectInvocation prepareExerciseUrl(QwExerciseUrlRequest request) {
        return buildInvocation(
                AllinpayDirectOperation.EXERCISE_URL,
                properties.getDirect().getExerciseUrlServiceCode(),
                request
        );
    }

    public AllinpayDirectInvocation prepareLendingNotify(QwLendingNotifyRequest request) {
        return buildInvocation(
                AllinpayDirectOperation.LENDING_NOTIFY,
                properties.getDirect().getLendingNotifyServiceCode(),
                request
        );
    }

    private AllinpayDirectInvocation buildInvocation(
            AllinpayDirectOperation operation,
            String serviceCode,
            Object businessRequest
    ) {
        requireNotBlank(properties.getDirect().getBaseUrl(), "direct.baseUrl");
        requireNotBlank(properties.getDirect().getProcessPath(), "direct.processPath");
        requireNotBlank(properties.getDirect().getMerchantId(), "direct.merchantId");
        requireNotBlank(properties.getDirect().getUserName(), "direct.userName");
        requireNotBlank(properties.getDirect().getUserPassword(), "direct.userPassword");
        requireNotBlank(serviceCode, serviceCodeField(operation));
        return new AllinpayDirectInvocation(
                operation,
                serviceCode,
                URI.create(properties.getDirect().getBaseUrl() + properties.getDirect().getProcessPath()),
                properties.getDirect().getMerchantId(),
                properties.getDirect().getUserName(),
                properties.getDirect().getUserPassword(),
                businessRequest
        );
    }

    private String serviceCodeField(AllinpayDirectOperation operation) {
        return switch (operation) {
            case MEMBER_SYNC -> "direct.memberSyncServiceCode";
            case EXERCISE_URL -> "direct.exerciseUrlServiceCode";
            case LENDING_NOTIFY -> "direct.lendingNotifyServiceCode";
        };
    }

    private void requireNotBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new BizException(
                    "ALLINPAY_DIRECT_CONFIG_MISSING",
                    "Missing required allinpay direct configuration: " + field
            );
        }
    }
}
