package com.nexusfin.equity.config;

import java.util.ArrayList;
import java.util.List;

public class QwHttpProperties {

    private String baseUrl = "https://t-api.test.qweimobile.com";
    private String methodPath = "/api/abs/method";
    private int connectTimeoutMs = OutboundHttpTimeoutDefaults.FIVE_SECONDS_MS;
    private int readTimeoutMs = OutboundHttpTimeoutDefaults.FIVE_SECONDS_MS;
    private boolean logPlaintextPayload = false;
    private boolean logFullPlaintextPayload = false;
    private List<String> logFullPlaintextPayloadAllowedProfiles = new ArrayList<>(List.of("test", "mysql-it", "local"));
    private String mockExerciseBaseUrl = "https://mock-qw.local/exercise";

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

    public boolean isLogPlaintextPayload() {
        return logPlaintextPayload;
    }

    public void setLogPlaintextPayload(boolean logPlaintextPayload) {
        this.logPlaintextPayload = logPlaintextPayload;
    }

    public boolean isLogFullPlaintextPayload() {
        return logFullPlaintextPayload;
    }

    public void setLogFullPlaintextPayload(boolean logFullPlaintextPayload) {
        this.logFullPlaintextPayload = logFullPlaintextPayload;
    }

    public List<String> getLogFullPlaintextPayloadAllowedProfiles() {
        return logFullPlaintextPayloadAllowedProfiles;
    }

    public void setLogFullPlaintextPayloadAllowedProfiles(List<String> logFullPlaintextPayloadAllowedProfiles) {
        this.logFullPlaintextPayloadAllowedProfiles = logFullPlaintextPayloadAllowedProfiles;
    }

    public String getMockExerciseBaseUrl() {
        return mockExerciseBaseUrl;
    }

    public void setMockExerciseBaseUrl(String mockExerciseBaseUrl) {
        this.mockExerciseBaseUrl = mockExerciseBaseUrl;
    }
}
