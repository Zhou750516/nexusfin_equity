package com.nexusfin.equity.thirdparty.qw;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class AllinpayLendingNotifyPayloadMapper implements AllinpayDirectPayloadMapper<QwLendingNotifyRequest> {

    @Override
    public AllinpayDirectOperation operation() {
        return AllinpayDirectOperation.LENDING_NOTIFY;
    }

    @Override
    public Class<QwLendingNotifyRequest> requestType() {
        return QwLendingNotifyRequest.class;
    }

    @Override
    public JsonNode map(QwLendingNotifyRequest request, ObjectMapper objectMapper) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("uniqueId", request.uniqueId());
        node.put("partnerOrderNo", request.partnerOrderNo());
        if (request.status() != null) {
            node.put("status", request.status());
        }
        return node;
    }
}
