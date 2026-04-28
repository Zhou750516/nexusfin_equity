package com.nexusfin.equity.config;

public class QwSecurityProperties {

    private String signKey = "abs-secret-key";
    private String aesKey = "0123456789abcdef";
    private String aesKeyBase64 = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
    private QwProperties.AesKeyEncoding aesKeyEncoding = QwProperties.AesKeyEncoding.RAW;
    private String aesAlgorithm = "AES/ECB/PKCS5Padding";
    private QwProperties.CiphertextEncoding ciphertextEncoding = QwProperties.CiphertextEncoding.HEX;
    private int gcmTagBits = 128;
    private int ivLengthBytes = 12;

    public String getSignKey() {
        return signKey;
    }

    public void setSignKey(String signKey) {
        this.signKey = signKey;
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

    public QwProperties.AesKeyEncoding getAesKeyEncoding() {
        return aesKeyEncoding;
    }

    public void setAesKeyEncoding(QwProperties.AesKeyEncoding aesKeyEncoding) {
        this.aesKeyEncoding = aesKeyEncoding;
    }

    public String getAesAlgorithm() {
        return aesAlgorithm;
    }

    public void setAesAlgorithm(String aesAlgorithm) {
        this.aesAlgorithm = aesAlgorithm;
    }

    public QwProperties.CiphertextEncoding getCiphertextEncoding() {
        return ciphertextEncoding;
    }

    public void setCiphertextEncoding(QwProperties.CiphertextEncoding ciphertextEncoding) {
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
}
