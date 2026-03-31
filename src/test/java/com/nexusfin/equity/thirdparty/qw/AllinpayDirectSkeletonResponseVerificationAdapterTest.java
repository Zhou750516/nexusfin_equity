package com.nexusfin.equity.thirdparty.qw;

import com.nexusfin.equity.exception.BizException;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AllinpayDirectSkeletonResponseVerificationAdapterTest {

    @Test
    void shouldReportProtocolUnimplementedWhenVerifyingRawResponse() {
        AllinpayDirectResponseVerificationStage verificationStage =
                new AllinpayDirectSkeletonResponseVerificationStage();

        assertThatThrownBy(() -> verificationStage.verify(new AllinpayDirectRawResponse(
                200,
                "{\"status\":\"ok\"}",
                null,
                Map.of()
        )))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("ALLINPAY_DIRECT_PROTOCOL_UNIMPLEMENTED");
    }
}
