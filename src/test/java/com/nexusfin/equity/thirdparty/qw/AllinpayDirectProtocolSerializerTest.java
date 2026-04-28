package com.nexusfin.equity.thirdparty.qw;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;

class AllinpayDirectProtocolSerializerTest {

    @Test
    void shouldSerializeEnvelopeIntoSkeletonRequest() {
        ObjectMapper objectMapper = new ObjectMapper();
        AllinpayDirectEnvelope envelope = new AllinpayDirectEnvelope(
                AllinpayDirectOperation.MEMBER_SYNC,
                java.net.URI.create("https://tlt-test.allinpay.com/aipg/ProcessServlet"),
                new AllinpayDirectEnvelopeHead(
                        "SYNC001",
                        "200000000007804",
                        "20000000000780404",
                        "111111",
                        "2026-03-31 22:00:00"
                ),
                new AllinpayMemberSyncPayloadMapper().map(
                        new QwMemberSyncRequest(
                                "user-1", "ord-1", 680000L, "P-1", "权益产品", "13800138000", "张三", "proto-1",
                                null, 0, null, null, null, null
                        ),
                        objectMapper
                )
        );

        AllinpayDirectProtocolSerializer serializer = new AllinpayDirectSkeletonProtocolSerializer(objectMapper);

        AllinpayDirectSerializedRequest serializedRequest = serializer.serialize(envelope);

        assertThat(serializedRequest.targetUri()).isEqualTo(envelope.targetUri());
        assertThat(serializedRequest.contentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(serializedRequest.requestBody()).contains("\"operation\":\"MEMBER_SYNC\"");
        assertThat(serializedRequest.requestBody()).contains("\"serviceCode\":\"SYNC001\"");
        assertThat(serializedRequest.requestBody()).contains("\"partnerOrderNo\":\"ord-1\"");
        assertThat(serializedRequest.signingPayload()).isEqualTo(serializedRequest.requestBody());
    }
}
