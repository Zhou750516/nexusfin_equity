package com.nexusfin.equity.thirdparty.qw;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusfin.equity.config.QwProperties;
import com.nexusfin.equity.exception.BizException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AllinpayDirectRequestBuilderTest {

    @Test
    void shouldBuildSignedMemberSyncPreparedRequest() {
        QwProperties properties = directProperties();
        AllinpayRequestSigner requestSigner = mock(AllinpayRequestSigner.class);
        when(requestSigner.sign(anyString())).thenReturn("signed-payload");
        AllinpayDirectRequestBuilder builder = new AllinpayDirectRequestBuilder(
                properties,
                new ObjectMapper(),
                requestSigner,
                new AllinpayMemberSyncPayloadMapper(),
                new AllinpayExerciseUrlPayloadMapper(),
                new AllinpayDeductionNotifyPayloadMapper()
        );

        AllinpayDirectPreparedRequest prepared = builder.prepareMemberSync(new QwMemberSyncRequest(
                "uid-001",
                "ord-001",
                29900L,
                "PROD-001",
                "权益会员",
                99887766L,
                "6222020202020202",
                0,
                null,
                null,
                null,
                null
        ));

        assertThat(prepared.targetUri().toString()).isEqualTo("https://tlt-test.allinpay.com/aipg/ProcessServlet");
        assertThat(prepared.signature()).isEqualTo("signed-payload");
        assertThat(prepared.requestBody()).contains("\"serviceCode\":\"SYNC001\"");
        assertThat(prepared.requestBody()).contains("\"uniqueId\":\"uid-001\"");
        assertThat(prepared.requestBody()).contains("\"userSignId\":99887766");
        assertThat(prepared.requestBody()).doesNotContain("payProtocolNo");
        assertThat(prepared.requestBody()).doesNotContain("mobile");
        assertThat(prepared.requestBody()).doesNotContain("username");
    }

    @Test
    void shouldRejectMissingDirectMerchantId() {
        QwProperties properties = directProperties();
        properties.getDirect().setMerchantId("");
        AllinpayRequestSigner requestSigner = mock(AllinpayRequestSigner.class);
        AllinpayDirectRequestBuilder builder = new AllinpayDirectRequestBuilder(
                properties,
                new ObjectMapper(),
                requestSigner,
                new AllinpayMemberSyncPayloadMapper(),
                new AllinpayExerciseUrlPayloadMapper(),
                new AllinpayDeductionNotifyPayloadMapper()
        );

        assertThatThrownBy(() -> builder.prepareMemberSync(new QwMemberSyncRequest(
                "uid-001",
                "ord-001",
                29900L,
                "PROD-001",
                "权益会员",
                99887766L,
                "6222020202020202",
                0,
                null,
                null,
                null,
                null
        )))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("direct.merchantId");
    }

    @Test
    void shouldBuildDeductionNotifyPreparedRequestWith0515Fields() {
        QwProperties properties = directProperties();
        AllinpayRequestSigner requestSigner = mock(AllinpayRequestSigner.class);
        when(requestSigner.sign(anyString())).thenReturn("signed-payload");
        AllinpayDirectRequestBuilder builder = new AllinpayDirectRequestBuilder(
                properties,
                new ObjectMapper(),
                requestSigner,
                new AllinpayMemberSyncPayloadMapper(),
                new AllinpayExerciseUrlPayloadMapper(),
                new AllinpayDeductionNotifyPayloadMapper()
        );

        AllinpayDirectPreparedRequest prepared = builder.prepareDeductionNotify(
                new QwDeductionNotifyRequest("uid-001", "ord-001", "serial-001", 1, 99887766L));

        assertThat(prepared.requestBody()).contains("\"serviceCode\":\"DEDUCT_NOTIFY001\"");
        assertThat(prepared.requestBody()).contains("\"uniqueId\":\"uid-001\"");
        assertThat(prepared.requestBody()).contains("\"partnerOrderNo\":\"ord-001\"");
        assertThat(prepared.requestBody()).contains("\"serialNo\":\"serial-001\"");
        assertThat(prepared.requestBody()).contains("\"status\":1");
        assertThat(prepared.requestBody()).contains("\"userSignId\":99887766");
    }

    private QwProperties directProperties() {
        QwProperties properties = new QwProperties();
        properties.setMode(QwProperties.Mode.ALLINPAY_DIRECT);
        properties.getDirect().setBaseUrl("https://tlt-test.allinpay.com");
        properties.getDirect().setProcessPath("/aipg/ProcessServlet");
        properties.getDirect().setMerchantId("200000000007804");
        properties.getDirect().setUserName("20000000000780404");
        properties.getDirect().setUserPassword("111111");
        properties.getDirect().setMemberSyncServiceCode("SYNC001");
        properties.getDirect().setExerciseUrlServiceCode("TOKEN001");
        properties.getDirect().setDeductionNotifyServiceCode("DEDUCT_NOTIFY001");
        return properties;
    }
}
