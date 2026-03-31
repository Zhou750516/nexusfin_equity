package com.nexusfin.equity.thirdparty.qw;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AllinpayDirectPayloadMapperRegistryTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AllinpayDirectPayloadMapperRegistry registry = new AllinpayDirectPayloadMapperRegistry(
            objectMapper,
            new AllinpayMemberSyncPayloadMapper(),
            new AllinpayExerciseUrlPayloadMapper(),
            new AllinpayLendingNotifyPayloadMapper()
    );

    @Test
    void shouldMapMemberSyncRequestToNormalizedPayload() {
        JsonNode payload = registry.map(
                AllinpayDirectOperation.MEMBER_SYNC,
                new QwMemberSyncRequest(
                        "user-1", "ord-1", 680000L, "P-1", "权益产品", "13800138000", "张三", "proto-1",
                        null, 0, null, null, null, null
                )
        );

        assertThat(payload.path("uniqueId").asText()).isEqualTo("user-1");
        assertThat(payload.path("partnerOrderNo").asText()).isEqualTo("ord-1");
        assertThat(payload.path("payAmount").asLong()).isEqualTo(680000L);
        assertThat(payload.path("productCode").asText()).isEqualTo("P-1");
    }

    @Test
    void shouldMapExerciseRequestToNormalizedPayload() {
        JsonNode payload = registry.map(
                AllinpayDirectOperation.EXERCISE_URL,
                new QwExerciseUrlRequest("user-2", "ord-2")
        );

        assertThat(payload.path("uniqueId").asText()).isEqualTo("user-2");
        assertThat(payload.path("partnerOrderNo").asText()).isEqualTo("ord-2");
    }

    @Test
    void shouldMapLendingNotifyRequestToNormalizedPayload() {
        JsonNode payload = registry.map(
                AllinpayDirectOperation.LENDING_NOTIFY,
                new QwLendingNotifyRequest("user-3", "ord-3", 1)
        );

        assertThat(payload.path("uniqueId").asText()).isEqualTo("user-3");
        assertThat(payload.path("partnerOrderNo").asText()).isEqualTo("ord-3");
        assertThat(payload.path("status").asInt()).isEqualTo(1);
    }
}
