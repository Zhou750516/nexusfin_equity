package com.nexusfin.equity.thirdparty.qw;

import com.nexusfin.equity.config.QwProperties;
import com.nexusfin.equity.exception.BizException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AllinpayDirectRequestFactoryTest {

    @Test
    void shouldBuildMemberSyncInvocation() {
        QwProperties properties = directProperties();
        AllinpayDirectRequestFactory factory = new AllinpayDirectRequestFactory(properties);
        QwMemberSyncRequest request = new QwMemberSyncRequest(
                "user-1", "ord-1", 680000L, "P-1", "权益产品", "13800138000", "张三", "proto-1",
                null, 0, null, null, null, null
        );

        AllinpayDirectInvocation invocation = factory.prepareMemberSync(request);

        assertThat(invocation.operation()).isEqualTo(AllinpayDirectOperation.MEMBER_SYNC);
        assertThat(invocation.serviceCode()).isEqualTo("SYNC001");
        assertThat(invocation.targetUri().toString()).isEqualTo("https://tlt-test.allinpay.com/aipg/ProcessServlet");
        assertThat(invocation.merchantId()).isEqualTo("200000000007804");
        assertThat(invocation.userName()).isEqualTo("20000000000780404");
        assertThat(invocation.userPassword()).isEqualTo("111111");
        assertThat(invocation.businessRequest()).isSameAs(request);
    }

    @Test
    void shouldBuildExerciseInvocationWithExerciseServiceCode() {
        QwProperties properties = directProperties();
        AllinpayDirectRequestFactory factory = new AllinpayDirectRequestFactory(properties);

        AllinpayDirectInvocation invocation = factory.prepareExerciseUrl(new QwExerciseUrlRequest("user-1", "ord-1"));

        assertThat(invocation.operation()).isEqualTo(AllinpayDirectOperation.EXERCISE_URL);
        assertThat(invocation.serviceCode()).isEqualTo("TOKEN001");
    }

    @Test
    void shouldRejectMissingMerchantId() {
        QwProperties properties = directProperties();
        properties.getDirect().setMerchantId("");
        AllinpayDirectRequestFactory factory = new AllinpayDirectRequestFactory(properties);

        assertThatThrownBy(() -> factory.prepareLendingNotify(new QwLendingNotifyRequest("user-1", "ord-1", 1)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("ALLINPAY_DIRECT_CONFIG_MISSING")
                .hasMessageContaining("direct.merchantId");
    }

    private QwProperties directProperties() {
        QwProperties properties = new QwProperties();
        properties.setMode(QwProperties.Mode.ALLINPAY_DIRECT);
        properties.setEnabled(true);
        properties.getDirect().setBaseUrl("https://tlt-test.allinpay.com");
        properties.getDirect().setProcessPath("/aipg/ProcessServlet");
        properties.getDirect().setMerchantId("200000000007804");
        properties.getDirect().setUserName("20000000000780404");
        properties.getDirect().setUserPassword("111111");
        properties.getDirect().setMemberSyncServiceCode("SYNC001");
        properties.getDirect().setExerciseUrlServiceCode("TOKEN001");
        properties.getDirect().setLendingNotifyServiceCode("LEND001");
        return properties;
    }
}
