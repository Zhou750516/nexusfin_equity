package com.nexusfin.equity.thirdparty.qw;

public interface AllinpayDirectResponseVerificationStage {

    AllinpayDirectVerifiedResponse verify(AllinpayDirectRawResponse rawResponse);
}
