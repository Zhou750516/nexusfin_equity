package com.nexusfin.equity.thirdparty.qw;

import com.nexusfin.equity.exception.BizException;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AllinpayCertificateLoaderTest {

    private final AllinpayCertificateLoader loader = new AllinpayCertificateLoader();

    @Test
    void shouldLoadPkcs12FromFixture() throws Exception {
        KeyStore keyStore = loader.loadPkcs12(
                "docs/third-part/齐为/通联测试证书/user-rsa.p12",
                "111111"
        );

        assertThat(keyStore.size()).isEqualTo(1);
        assertThat(keyStore.aliases().nextElement()).isNotBlank();
    }

    @Test
    void shouldLoadPemCertificateFromFixture() {
        X509Certificate certificate = loader.loadCertificate(
                "docs/third-part/齐为/通联测试证书/public-rsa.cer"
        );

        assertThat(certificate.getSubjectX500Principal().getName()).contains("CN=allinpay");
    }

    @Test
    void shouldFailForMissingCertificateFile() {
        assertThatThrownBy(() -> loader.loadCertificate("docs/third-part/齐为/通联测试证书/missing.cer"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("ALLINPAY_CERT_LOAD_FAILED");
    }
}
