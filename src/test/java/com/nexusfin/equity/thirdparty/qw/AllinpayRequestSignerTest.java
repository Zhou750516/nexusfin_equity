package com.nexusfin.equity.thirdparty.qw;

import java.security.KeyStore;
import java.security.cert.X509Certificate;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AllinpayRequestSignerTest {

    private final AllinpayCertificateLoader certificateLoader = new AllinpayCertificateLoader();

    @Test
    void shouldSignPayloadWithPkcs12AndVerifyWithCertificate() {
        KeyStore keyStore = certificateLoader.loadPkcs12(
                "docs/third-part/齐为/通联测试证书/user-rsa.p12",
                "111111"
        );
        X509Certificate certificate = certificateLoader.loadCertificate(
                "docs/third-part/齐为/通联测试证书/public-rsa.cer"
        );
        AllinpayRequestSigner signer = new AllinpayRequestSigner(keyStore, "111111");
        AllinpayResponseVerifier verifier = new AllinpayResponseVerifier(certificate);

        String payload = "<AIPG><INFO>hello-allinpay</INFO></AIPG>";
        String signature = signer.sign(payload);

        assertThat(signature).isNotBlank();
        assertThat(verifier.verify(payload, signature)).isTrue();
    }
}
