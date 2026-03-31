package com.nexusfin.equity.thirdparty.qw;

import com.nexusfin.equity.exception.BizException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AllinpayDirectHeaderResponseSignatureResolverTest {

    @Test
    void shouldResolveSignatureFromConfiguredHeader() {
        AllinpayDirectResponseSignatureResolver resolver =
                new AllinpayDirectHeaderResponseSignatureResolver("X-Resp-Signature");

        String signature = resolver.resolve(new AllinpayDirectRawResponse(
                200,
                "{\"status\":\"ok\"}",
                null,
                Map.of("X-Resp-Signature", List.of("resp-signature"))
        ));

        assertThat(signature).isEqualTo("resp-signature");
    }

    @Test
    void shouldRejectMissingSignatureHeader() {
        AllinpayDirectResponseSignatureResolver resolver =
                new AllinpayDirectHeaderResponseSignatureResolver("X-Resp-Signature");

        assertThatThrownBy(() -> resolver.resolve(new AllinpayDirectRawResponse(
                200,
                "{\"status\":\"ok\"}",
                null,
                Map.of()
        )))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("ALLINPAY_DIRECT_RESPONSE_SIGNATURE_MISSING");
    }
}
