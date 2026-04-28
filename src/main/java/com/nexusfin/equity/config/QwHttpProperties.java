package com.nexusfin.equity.config;

public class QwHttpProperties {

    private String baseUrl = "https://t-api.test.qweimobile.com";
    private String methodPath = "/api/abs/method";
    private int connectTimeoutMs = 2000;
    private int readTimeoutMs = 3000;
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

    public String getMockExerciseBaseUrl() {
        return mockExerciseBaseUrl;
    }

    public void setMockExerciseBaseUrl(String mockExerciseBaseUrl) {
        this.mockExerciseBaseUrl = mockExerciseBaseUrl;
    }
}
