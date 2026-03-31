package com.nexusfin.equity.thirdparty.qw;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusfin.equity.config.QwProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AllinpayDirectEnvelopeFactoryTest {

    @Test
    void shouldCreateEnvelopeFromInvocationAndPayload() {
        QwProperties properties = new QwProperties();
        properties.getDirect().setBaseUrl("https://tlt-test.allinpay.com");
        properties.getDirect().setProcessPath("/aipg/ProcessServlet");
        properties.getDirect().setMerchantId("200000000007804");
        properties.getDirect().setUserName("20000000000780404");
        properties.getDirect().setUserPassword("111111");
        properties.getDirect().setMemberSyncServiceCode("SYNC001");
        AllinpayDirectRequestFactory requestFactory = new AllinpayDirectRequestFactory(properties);
        AllinpayDirectInvocation invocation = requestFactory.prepareMemberSync(new QwMemberSyncRequest(
                "user-1", "ord-1", 680000L, "P-1", "权益产品", "13800138000", "张三", "proto-1",
                null, 0, null, null, null, null
        ));
        AllinpayDirectPayloadMapperRegistry registry = new AllinpayDirectPayloadMapperRegistry(
                new ObjectMapper(),
                new AllinpayMemberSyncPayloadMapper(),
                new AllinpayExerciseUrlPayloadMapper(),
                new AllinpayLendingNotifyPayloadMapper()
        );
        AllinpayDirectEnvelopeFactory envelopeFactory = new AllinpayDirectEnvelopeFactory();

        AllinpayDirectEnvelope envelope = envelopeFactory.create(
                invocation,
                registry.map(invocation.operation(), invocation.businessRequest()),
                "2026-03-31T20:00:00"
        );

        assertThat(envelope.operation()).isEqualTo(AllinpayDirectOperation.MEMBER_SYNC);
        assertThat(envelope.targetUri().toString()).isEqualTo("https://tlt-test.allinpay.com/aipg/ProcessServlet");
        assertThat(envelope.head().serviceCode()).isEqualTo("SYNC001");
        assertThat(envelope.head().merchantId()).isEqualTo("200000000007804");
        assertThat(envelope.head().userName()).isEqualTo("20000000000780404");
        assertThat(envelope.head().timestamp()).isEqualTo("2026-03-31T20:00:00");
        assertThat(envelope.businessPayload().path("partnerOrderNo").asText()).isEqualTo("ord-1");
    }
}
