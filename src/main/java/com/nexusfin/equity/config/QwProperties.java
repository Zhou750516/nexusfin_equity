package com.nexusfin.equity.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nexusfin.third-party.qw")
public class QwProperties {

    private boolean enabled = true;
    private Mode mode = Mode.MOCK;
    private String baseUrl = "https://t-api.test.qweimobile.com";
    private String methodPath = "/api/abs/method";
    private String partnerNo = "abs";
    private String version = "v1.0";
    private String signKey = "abs-secret-key";
    private int connectTimeoutMs = 2000;
    private int readTimeoutMs = 3000;
    private String aesKey = "0123456789abcdef";
    private String aesKeyBase64 = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
    private AesKeyEncoding aesKeyEncoding = AesKeyEncoding.RAW;
    private String aesAlgorithm = "AES/ECB/PKCS5Padding";
    private CiphertextEncoding ciphertextEncoding = CiphertextEncoding.HEX;
    private int gcmTagBits = 128;
    private int ivLengthBytes = 12;
    private String defaultPayProtocolPrefix = "proto-";
    private String memberSyncPayProtocolNoOverride;
    private boolean allowMemberSyncPayProtocolNoOverride;
    private List<String> memberSyncPayProtocolNoOverrideAllowedProfiles = List.of("test", "mysql-it", "local");
    private String mockExerciseBaseUrl = "https://mock-qw.local/exercise";
    private final Direct direct = new Direct();

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

    public String getAesKey() {
        return aesKey;
    }

    public void setAesKey(String aesKey) {
        this.aesKey = aesKey;
    }

    public String getAesKeyBase64() {
        return aesKeyBase64;
    }

    public void setAesKeyBase64(String aesKeyBase64) {
        this.aesKeyBase64 = aesKeyBase64;
    }

    public AesKeyEncoding getAesKeyEncoding() {
        return aesKeyEncoding;
    }

    public void setAesKeyEncoding(AesKeyEncoding aesKeyEncoding) {
        this.aesKeyEncoding = aesKeyEncoding;
    }

    public String getAesAlgorithm() {
        return aesAlgorithm;
    }

    public void setAesAlgorithm(String aesAlgorithm) {
        this.aesAlgorithm = aesAlgorithm;
    }

    public CiphertextEncoding getCiphertextEncoding() {
        return ciphertextEncoding;
    }

    public void setCiphertextEncoding(CiphertextEncoding ciphertextEncoding) {
        this.ciphertextEncoding = ciphertextEncoding;
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

    public String getMemberSyncPayProtocolNoOverride() {
        return memberSyncPayProtocolNoOverride;
    }

    public void setMemberSyncPayProtocolNoOverride(String memberSyncPayProtocolNoOverride) {
        this.memberSyncPayProtocolNoOverride = memberSyncPayProtocolNoOverride;
    }

    public boolean isAllowMemberSyncPayProtocolNoOverride() {
        return allowMemberSyncPayProtocolNoOverride;
    }

    public void setAllowMemberSyncPayProtocolNoOverride(boolean allowMemberSyncPayProtocolNoOverride) {
        this.allowMemberSyncPayProtocolNoOverride = allowMemberSyncPayProtocolNoOverride;
    }

    public List<String> getMemberSyncPayProtocolNoOverrideAllowedProfiles() {
        return memberSyncPayProtocolNoOverrideAllowedProfiles;
    }

    public void setMemberSyncPayProtocolNoOverrideAllowedProfiles(List<String> memberSyncPayProtocolNoOverrideAllowedProfiles) {
        this.memberSyncPayProtocolNoOverrideAllowedProfiles = memberSyncPayProtocolNoOverrideAllowedProfiles;
    }

    public String getMockExerciseBaseUrl() {
        return mockExerciseBaseUrl;
    }

    public void setMockExerciseBaseUrl(String mockExerciseBaseUrl) {
        this.mockExerciseBaseUrl = mockExerciseBaseUrl;
    }

    public Direct getDirect() {
        return direct;
    }

    public enum Mode {
        MOCK,
        HTTP,
        QWEIMOBILE_HTTP,
        ALLINPAY_DIRECT
    }

    public enum AesKeyEncoding {
        RAW,
        BASE64
    }

    public enum CiphertextEncoding {
        BASE64,
        HEX
    }

    public static class Direct {

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
}
