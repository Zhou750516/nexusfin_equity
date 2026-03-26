package com.nexusfin.equity.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nexusfin.third-party.qw")
public class QwProperties {

    private boolean enabled = true;
    private Mode mode = Mode.MOCK;
    private String baseUrl = "https://t-api.test.qweimobile.com";
    private String methodPath = "/api/abs/method";
    private String partnerNo = "abs-app";
    private String version = "v1.0";
    private String signKey = "abs-secret-key";
    private int connectTimeoutMs = 2000;
    private int readTimeoutMs = 3000;
    private String aesKeyBase64 = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
    private String aesAlgorithm = "AES/GCM/NoPadding";
    private int gcmTagBits = 128;
    private int ivLengthBytes = 12;
    private String defaultPayProtocolPrefix = "proto-";
    private String mockExerciseBaseUrl = "https://mock-qw.local/exercise";

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

    public String getMethodPath() {
        return methodPath;
    }

    public void setMethodPath(String methodPath) {
        this.methodPath = methodPath;
    }

    public String getPartnerNo() {
        return partnerNo;
    }

    public void setPartnerNo(String partnerNo) {
        this.partnerNo = partnerNo;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getSignKey() {
        return signKey;
    }

    public void setSignKey(String signKey) {
        this.signKey = signKey;
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

    public String getDefaultPayProtocolPrefix() {
        return defaultPayProtocolPrefix;
    }

    public void setDefaultPayProtocolPrefix(String defaultPayProtocolPrefix) {
        this.defaultPayProtocolPrefix = defaultPayProtocolPrefix;
    }

    public String getMockExerciseBaseUrl() {
        return mockExerciseBaseUrl;
    }

    public void setMockExerciseBaseUrl(String mockExerciseBaseUrl) {
        this.mockExerciseBaseUrl = mockExerciseBaseUrl;
    }

    public enum Mode {
        MOCK,
        HTTP
    }
}
