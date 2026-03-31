package com.nexusfin.equity.thirdparty.qw;

import java.security.KeyStore;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AllinpaySslContextFactoryTest {

    private final AllinpayCertificateLoader certificateLoader = new AllinpayCertificateLoader();

    @Test
    void shouldBuildSslContextFromMerchantKeyStoreAndVerifyCertificate() {
        KeyStore keyStore = certificateLoader.loadPkcs12(
                "docs/third-part/齐为/通联测试证书/user-rsa.p12",
                "111111"
        );
        X509Certificate certificate = certificateLoader.loadCertificate(
                "docs/third-part/齐为/通联测试证书/public-rsa.cer"
        );
        AllinpaySslContextFactory factory = new AllinpaySslContextFactory();

        SSLContext sslContext = factory.create(keyStore, "111111", certificate);

        assertThat(sslContext).isNotNull();
        assertThat(sslContext.getProtocol()).isEqualTo("TLS");
    }
}
