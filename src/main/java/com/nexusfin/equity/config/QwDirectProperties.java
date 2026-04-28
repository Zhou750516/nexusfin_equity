package com.nexusfin.equity.config;

public class QwDirectProperties {

    private String baseUrl = "https://tlt-test.allinpay.com";
    private String processPath = "/aipg/ProcessServlet";
    private String merchantId;
    private String userName;
    private String userPassword;
    private String channelCode;
    private String accountNo;
    private String pkcs12Path;
    private String pkcs12Password;
    private String verifyCertPath;
    private int connectTimeoutMs = 3000;
    private int readTimeoutMs = 5000;
    private String memberSyncServiceCode;
    private String exerciseUrlServiceCode;
    private String lendingNotifyServiceCode;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getProcessPath() {
        return processPath;
    }

    public void setProcessPath(String processPath) {
        this.processPath = processPath;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserPassword() {
        return userPassword;
    }

    public void setUserPassword(String userPassword) {
        this.userPassword = userPassword;
    }

    public String getChannelCode() {
        return channelCode;
    }

    public void setChannelCode(String channelCode) {
        this.channelCode = channelCode;
    }

    public String getAccountNo() {
        return accountNo;
    }

    public void setAccountNo(String accountNo) {
        this.accountNo = accountNo;
    }

    public String getPkcs12Path() {
        return pkcs12Path;
    }

    public void setPkcs12Path(String pkcs12Path) {
        this.pkcs12Path = pkcs12Path;
    }

    public String getPkcs12Password() {
        return pkcs12Password;
    }

    public void setPkcs12Password(String pkcs12Password) {
        this.pkcs12Password = pkcs12Password;
    }

    public String getVerifyCertPath() {
        return verifyCertPath;
    }

    public void setVerifyCertPath(String verifyCertPath) {
        this.verifyCertPath = verifyCertPath;
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

    public String getMemberSyncServiceCode() {
        return memberSyncServiceCode;
    }

    public void setMemberSyncServiceCode(String memberSyncServiceCode) {
        this.memberSyncServiceCode = memberSyncServiceCode;
    }

    public String getExerciseUrlServiceCode() {
        return exerciseUrlServiceCode;
    }

    public void setExerciseUrlServiceCode(String exerciseUrlServiceCode) {
        this.exerciseUrlServiceCode = exerciseUrlServiceCode;
    }

    public String getLendingNotifyServiceCode() {
        return lendingNotifyServiceCode;
    }

    public void setLendingNotifyServiceCode(String lendingNotifyServiceCode) {
        this.lendingNotifyServiceCode = lendingNotifyServiceCode;
    }
}
