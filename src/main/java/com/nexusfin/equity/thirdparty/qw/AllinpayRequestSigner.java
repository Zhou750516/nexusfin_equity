package com.nexusfin.equity.thirdparty.qw;

import com.nexusfin.equity.exception.BizException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.Base64;
import java.util.Enumeration;

public class AllinpayRequestSigner {

    private final PrivateKey privateKey;

    public AllinpayRequestSigner(KeyStore keyStore, String keyPassword) {
        this.privateKey = loadPrivateKey(keyStore, keyPassword);
    }

    public String sign(String payload) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);
            signature.update(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (GeneralSecurityException exception) {
            throw new BizException("ALLINPAY_SIGN_FAILED", "Failed to sign allinpay request payload");
        }
    }

    private PrivateKey loadPrivateKey(KeyStore keyStore, String keyPassword) {
        try {
            Enumeration<String> aliases = keyStore.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                if (!keyStore.isKeyEntry(alias)) {
                    continue;
                }
                Key key = keyStore.getKey(alias, keyPassword == null ? new char[0] : keyPassword.toCharArray());
                if (key instanceof PrivateKey privateKey) {
                    return privateKey;
                }
            }
        } catch (GeneralSecurityException exception) {
            throw new BizException("ALLINPAY_PRIVATE_KEY_LOAD_FAILED", "Failed to load allinpay private key");
        }
        throw new BizException("ALLINPAY_PRIVATE_KEY_LOAD_FAILED", "Failed to load allinpay private key");
    }
}
