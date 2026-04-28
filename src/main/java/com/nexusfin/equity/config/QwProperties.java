package com.nexusfin.equity.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nexusfin.third-party.qw")
public class QwProperties {

    private boolean enabled = true;
    private Mode mode = Mode.MOCK;
    private String partnerNo = "abs";
    private String version = "v1.0";
    private final QwHttpProperties http = new QwHttpProperties();
    private final QwSecurityProperties security = new QwSecurityProperties();
    private final QwPaymentProperties payment = new QwPaymentProperties();
    private final QwDirectProperties direct = new QwDirectProperties();

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

    public QwHttpProperties getHttp() {
        return http;
    }

    public QwSecurityProperties getSecurity() {
        return security;
    }

    public QwPaymentProperties getPayment() {
        return payment;
    }

    public QwDirectProperties getDirect() {
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
}
