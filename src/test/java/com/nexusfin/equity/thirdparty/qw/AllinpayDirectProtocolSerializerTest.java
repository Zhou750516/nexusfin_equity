package com.nexusfin.equity.thirdparty.qw;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusfin.equity.config.QwProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;

class AllinpayDirectProtocolSerializerTest {

    @Test
    void shouldSerializeEnvelopeIntoSkeletonRequest() {
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
        AllinpayDirectEnvelope envelope = new AllinpayDirectEnvelopeFactory().create(
                invocation,
                registry.map(invocation.operation(), invocation.businessRequest()),
                "2026-03-31 22:00:00"
        );

        AllinpayDirectProtocolSerializer serializer = new AllinpayDirectSkeletonProtocolSerializer(new ObjectMapper());

        AllinpayDirectSerializedRequest serializedRequest = serializer.serialize(envelope);

        assertThat(serializedRequest.targetUri()).isEqualTo(envelope.targetUri());
        assertThat(serializedRequest.contentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(serializedRequest.requestBody()).contains("\"operation\":\"MEMBER_SYNC\"");
        assertThat(serializedRequest.requestBody()).contains("\"serviceCode\":\"SYNC001\"");
        assertThat(serializedRequest.requestBody()).contains("\"partnerOrderNo\":\"ord-1\"");
        assertThat(serializedRequest.signingPayload()).isEqualTo(serializedRequest.requestBody());
    }
}
