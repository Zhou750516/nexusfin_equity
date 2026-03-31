package com.nexusfin.equity.thirdparty.qw;

import com.nexusfin.equity.exception.BizException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import org.springframework.stereotype.Component;

@Component
public class AllinpayCertificateLoader {

    public KeyStore loadPkcs12(String path, String password) {
        try (InputStream inputStream = Files.newInputStream(Path.of(path))) {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(inputStream, password == null ? new char[0] : password.toCharArray());
            return keyStore;
        } catch (IOException | GeneralSecurityException exception) {
            throw new BizException("ALLINPAY_KEYSTORE_LOAD_FAILED", "Failed to load allinpay PKCS12 keystore: " + path);
        }
    }

    public X509Certificate loadCertificate(String path) {
        try (InputStream inputStream = Files.newInputStream(Path.of(path))) {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            return (X509Certificate) certificateFactory.generateCertificate(inputStream);
        } catch (IOException | GeneralSecurityException exception) {
            throw new BizException("ALLINPAY_CERT_LOAD_FAILED", "Failed to load allinpay certificate: " + path);
        }
    }
}
