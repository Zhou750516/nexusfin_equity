package com.nexusfin.equity.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nexusfin.third-party.tech-platform")
public class TechPlatformProperties {

    private boolean enabled = true;
    private Mode mode = Mode.MOCK;
    private String baseUrl = "https://tech-platform.local";
    private String channelId = "abs-app";
    private String version = "1.0.0";
    private String signSecret = "tech-platform-secret";
    private SignAlgorithm signAlgorithm = SignAlgorithm.HMAC_SHA256;
    private String aesKeyBase64 = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
    private String aesAlgorithm = "AES/ECB/PKCS5Padding";
    private int gcmTagBits = 128;
    private int ivLengthBytes = 12;
    private int connectTimeoutMs = 2000;
    private int readTimeoutMs = 3000;
    private Paths paths = new Paths();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getSignSecret() {
        return signSecret;
    }

    public void setSignSecret(String signSecret) {
        this.signSecret = signSecret;
    }

    public SignAlgorithm getSignAlgorithm() {
        return signAlgorithm;
    }

    public void setSignAlgorithm(SignAlgorithm signAlgorithm) {
        this.signAlgorithm = signAlgorithm;
    }

    public String getAesKeyBase64() {
        return aesKeyBase64;
    }

    public void setAesKeyBase64(String aesKeyBase64) {
        this.aesKeyBase64 = aesKeyBase64;
    }

    public String getAesAlgorithm() {
        return aesAlgorithm;
    }

    public void setAesAlgorithm(String aesAlgorithm) {
        this.aesAlgorithm = aesAlgorithm;
    }

    public int getGcmTagBits() {
        return gcmTagBits;
    }

    public void setGcmTagBits(int gcmTagBits) {
        this.gcmTagBits = gcmTagBits;
    }

    public int getIvLengthBytes() {
        return ivLengthBytes;
    }

    public void setIvLengthBytes(int ivLengthBytes) {
        this.ivLengthBytes = ivLengthBytes;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }

    public Paths getPaths() {
        return paths;
    }

    public void setPaths(Paths paths) {
        this.paths = paths;
    }

    public enum Mode {
        MOCK,
        HTTP
    }

    public enum SignAlgorithm {
        HMAC_SHA256,
        MD5
    }

    public static class Paths {

        private String creditStatusNotice = "/guide/api/creditStatusNotice";
        private String loanInfoNotice = "/guide/api/loanInfoNotice";
        private String repayInfoNotice = "/guide/api/repayInfoNotice";

        public String getCreditStatusNotice() {
            return creditStatusNotice;
        }

        public void setCreditStatusNotice(String creditStatusNotice) {
            this.creditStatusNotice = creditStatusNotice;
        }

        public String getLoanInfoNotice() {
            return loanInfoNotice;
        }

        public void setLoanInfoNotice(String loanInfoNotice) {
            this.loanInfoNotice = loanInfoNotice;
        }

        public String getRepayInfoNotice() {
            return repayInfoNotice;
        }

        public void setRepayInfoNotice(String repayInfoNotice) {
            this.repayInfoNotice = repayInfoNotice;
        }
    }
}
