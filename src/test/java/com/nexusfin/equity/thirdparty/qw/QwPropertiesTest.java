package com.nexusfin.equity.thirdparty.qw;

import com.nexusfin.equity.config.QwProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QwPropertiesTest {

    @Test
    void shouldExposeDirectPropertiesForAllinpayMode() {
        QwProperties properties = new QwProperties();

        properties.setMode(QwProperties.Mode.ALLINPAY_DIRECT);
        properties.getSecurity().setAesKey("FbRW7iaiwcEKk2kY");
        properties.getSecurity().setAesKeyEncoding(QwProperties.AesKeyEncoding.RAW);
        properties.getSecurity().setCiphertextEncoding(QwProperties.CiphertextEncoding.HEX);
        properties.getPayment().setAllowMemberSyncPayProtocolNoOverride(true);
        properties.getDirect().setBaseUrl("https://tlt-test.allinpay.com");
        properties.getDirect().setProcessPath("/aipg/ProcessServlet");
        properties.getDirect().setMerchantId("200000000007804");
        properties.getDirect().setUserName("20000000000780404");
        properties.getDirect().setPkcs12Path("docs/third-part/齐为/通联测试证书/user-rsa.p12");
        properties.getDirect().setVerifyCertPath("docs/third-part/齐为/通联测试证书/public-rsa.cer");

        assertThat(properties.getMode()).isEqualTo(QwProperties.Mode.ALLINPAY_DIRECT);
        assertThat(properties.getSecurity().getAesKey()).isEqualTo("FbRW7iaiwcEKk2kY");
        assertThat(properties.getSecurity().getAesKeyEncoding()).isEqualTo(QwProperties.AesKeyEncoding.RAW);
        assertThat(properties.getSecurity().getCiphertextEncoding()).isEqualTo(QwProperties.CiphertextEncoding.HEX);
        assertThat(properties.getPayment().isAllowMemberSyncPayProtocolNoOverride()).isTrue();
        assertThat(properties.getDirect().getBaseUrl()).isEqualTo("https://tlt-test.allinpay.com");
        assertThat(properties.getDirect().getProcessPath()).isEqualTo("/aipg/ProcessServlet");
        assertThat(properties.getDirect().getMerchantId()).isEqualTo("200000000007804");
        assertThat(properties.getDirect().getUserName()).isEqualTo("20000000000780404");
        assertThat(properties.getDirect().getPkcs12Path()).contains("user-rsa.p12");
        assertThat(properties.getDirect().getVerifyCertPath()).contains("public-rsa.cer");
    }
}
