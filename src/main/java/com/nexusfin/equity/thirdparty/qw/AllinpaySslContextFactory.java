package com.nexusfin.equity.thirdparty.qw;

import com.nexusfin.equity.exception.BizException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

public class AllinpaySslContextFactory {

    public SSLContext create(KeyStore merchantKeyStore, String keyPassword, X509Certificate verifyCertificate) {
        try {
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(merchantKeyStore, keyPassword == null ? new char[0] : keyPassword.toCharArray());

            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null, null);
            trustStore.setCertificateEntry("allinpay-verify", verifyCertificate);

            TrustManagerFactory trustManagerFactory =
                    TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(trustStore);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), new SecureRandom());
            return sslContext;
        } catch (GeneralSecurityException | java.io.IOException exception) {
            throw new BizException("ALLINPAY_SSL_CONTEXT_INIT_FAILED", "Failed to initialize allinpay SSL context");
        }
    }
}
