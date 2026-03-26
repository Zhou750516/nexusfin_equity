package com.nexusfin.equity.config;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nexusfin.crypto")
public class CryptoProperties {

    private Dek dek = new Dek();
    private Inbound inbound = new Inbound();

    public Dek getDek() {
        return dek;
    }

    public void setDek(Dek dek) {
        this.dek = dek;
    }

    public Inbound getInbound() {
        return inbound;
    }

    public void setInbound(Inbound inbound) {
        this.inbound = inbound;
    }

    public static class Dek {

        private String keyId = "DEK_USER_PROFILE";
        private int keyVersion = 1;
        private String algorithm = "AES/GCM/NoPadding";
        private String purpose = "encrypt_user_sensitive_fields";
        private String status = "ACTIVE";
        private String plaintextKeyBase64 = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

        public String getKeyId() {
            return keyId;
        }

        public void setKeyId(String keyId) {
            this.keyId = keyId;
        }

        public int getKeyVersion() {
            return keyVersion;
        }

        public void setKeyVersion(int keyVersion) {
            this.keyVersion = keyVersion;
        }

        public String getAlgorithm() {
            return algorithm;
        }

        public void setAlgorithm(String algorithm) {
            this.algorithm = algorithm;
        }

        public String getPurpose() {
            return purpose;
        }

        public void setPurpose(String purpose) {
            this.purpose = purpose;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getPlaintextKeyBase64() {
            return plaintextKeyBase64;
        }

        public void setPlaintextKeyBase64(String plaintextKeyBase64) {
            this.plaintextKeyBase64 = plaintextKeyBase64;
        }
    }

    public static class Inbound {

        private String defaultMode = "PLAIN";
        private Map<String, String> channelModes = new LinkedHashMap<>();

        public String getDefaultMode() {
            return defaultMode;
        }

        public void setDefaultMode(String defaultMode) {
            this.defaultMode = defaultMode;
        }

        public Map<String, String> getChannelModes() {
            return channelModes;
        }

        public void setChannelModes(Map<String, String> channelModes) {
            this.channelModes = channelModes;
        }
    }
}
