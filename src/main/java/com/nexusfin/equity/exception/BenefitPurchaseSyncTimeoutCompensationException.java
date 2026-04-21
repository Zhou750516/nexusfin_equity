package com.nexusfin.equity.exception;

public class BenefitPurchaseSyncTimeoutCompensationException extends BizException {

    public BenefitPurchaseSyncTimeoutCompensationException(String message) {
        super("QW_SYNC_TIMEOUT", message);
    }
}
