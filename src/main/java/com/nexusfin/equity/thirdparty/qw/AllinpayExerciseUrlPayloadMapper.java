package com.nexusfin.equity.thirdparty.qw;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class AllinpayExerciseUrlPayloadMapper implements AllinpayDirectPayloadMapper<QwExerciseUrlRequest> {

    @Override
    public AllinpayDirectOperation operation() {
        return AllinpayDirectOperation.EXERCISE_URL;
    }

    @Override
    public Class<QwExerciseUrlRequest> requestType() {
        return QwExerciseUrlRequest.class;
    }

    @Override
    public JsonNode map(QwExerciseUrlRequest request, ObjectMapper objectMapper) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("uniqueId", request.uniqueId());
        node.put("partnerOrderNo", request.partnerOrderNo());
        return node;
    }
}
