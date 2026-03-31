package com.nexusfin.equity.thirdparty.qw;

import java.security.KeyStore;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLContext;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

class AllinpayRestClientFactoryTest {

    private final AllinpayCertificateLoader certificateLoader = new AllinpayCertificateLoader();

    @Test
    void shouldBuildRestClientWithSslContextAndTimeouts() {
        KeyStore keyStore = certificateLoader.loadPkcs12(
                "docs/third-part/齐为/通联测试证书/user-rsa.p12",
                "111111"
        );
        X509Certificate certificate = certificateLoader.loadCertificate(
                "docs/third-part/齐为/通联测试证书/public-rsa.cer"
        );
        SSLContext sslContext = new AllinpaySslContextFactory().create(keyStore, "111111", certificate);
        AllinpayRestClientFactory factory = new AllinpayRestClientFactory();

        RestClient restClient = factory.create(sslContext, 3000, 5000);

        assertThat(restClient).isNotNull();
    }
}
