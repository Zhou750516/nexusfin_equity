package com.nexusfin.equity.thirdparty.qw;

import com.nexusfin.equity.exception.BizException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.util.Base64;

public class AllinpayResponseVerifier {

    private final X509Certificate certificate;

    public AllinpayResponseVerifier(X509Certificate certificate) {
        this.certificate = certificate;
    }

    public boolean verify(String payload, String signatureBase64) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(certificate.getPublicKey());
            signature.update(payload.getBytes(StandardCharsets.UTF_8));
            return signature.verify(Base64.getDecoder().decode(signatureBase64));
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new BizException("ALLINPAY_VERIFY_FAILED", "Failed to verify allinpay response signature");
        }
    }
}
