package com.nexusfin.equity.thirdparty.qw;

import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AllinpayDirectResponseVerificationAdapterTest {

    private final AllinpayCertificateLoader certificateLoader = new AllinpayCertificateLoader();

    @Test
    void shouldResolveAndVerifyResponseSignature() {
        KeyStore keyStore = certificateLoader.loadPkcs12(
                "docs/third-part/齐为/通联测试证书/user-rsa.p12",
                "111111"
        );
        X509Certificate certificate = certificateLoader.loadCertificate(
                "docs/third-part/齐为/通联测试证书/public-rsa.cer"
        );
        String payload = "{\"status\":\"ok\"}";
        String signature = new AllinpayRequestSigner(keyStore, "111111").sign(payload);
        AllinpayDirectResponseVerificationAdapter adapter = new AllinpayDirectResponseVerificationAdapter(
                new AllinpayDirectHeaderResponseSignatureResolver("X-Resp-Signature"),
                new AllinpayResponseVerifier(certificate)
        );

        AllinpayDirectVerifiedResponse verifiedResponse = adapter.verify(new AllinpayDirectRawResponse(
                200,
                payload,
                null,
                Map.of("X-Resp-Signature", List.of(signature))
        ));

        assertThat(verifiedResponse.responseBody()).isEqualTo(payload);
        assertThat(verifiedResponse.signature()).isEqualTo(signature);
    }

    @Test
    void shouldRejectWhenResolvedSignatureDoesNotMatchPayload() {
        KeyStore keyStore = certificateLoader.loadPkcs12(
                "docs/third-part/齐为/通联测试证书/user-rsa.p12",
                "111111"
        );
        X509Certificate certificate = certificateLoader.loadCertificate(
                "docs/third-part/齐为/通联测试证书/public-rsa.cer"
        );
        String payload = "{\"status\":\"ok\"}";
        String signature = new AllinpayRequestSigner(keyStore, "111111").sign("{\"status\":\"mismatch\"}");
        AllinpayDirectResponseVerificationAdapter adapter = new AllinpayDirectResponseVerificationAdapter(
                new AllinpayDirectHeaderResponseSignatureResolver("X-Resp-Signature"),
                new AllinpayResponseVerifier(certificate)
        );

        assertThatThrownBy(() -> adapter.verify(new AllinpayDirectRawResponse(
                200,
                payload,
                null,
                Map.of("X-Resp-Signature", List.of(signature))
        )))
                .hasMessageContaining("ALLINPAY_DIRECT_RESPONSE_SIGNATURE_INVALID");
    }
}
