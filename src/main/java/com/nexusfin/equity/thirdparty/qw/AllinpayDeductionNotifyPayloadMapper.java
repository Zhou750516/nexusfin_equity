package com.nexusfin.equity.thirdparty.qw;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class AllinpayDeductionNotifyPayloadMapper implements AllinpayDirectPayloadMapper<QwDeductionNotifyRequest> {

    @Override
    public AllinpayDirectOperation operation() {
        return AllinpayDirectOperation.DEDUCTION_NOTIFY;
    }

    @Override
    public Class<QwDeductionNotifyRequest> requestType() {
        return QwDeductionNotifyRequest.class;
    }

    @Override
    public JsonNode map(QwDeductionNotifyRequest request, ObjectMapper objectMapper) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("uniqueId", request.uniqueId());
        node.put("partnerOrderNo", request.partnerOrderNo());
        node.put("serialNo", request.serialNo());
        if (request.status() != null) {
            node.put("status", request.status());
        }
        if (request.userSignId() != null) {
            node.put("userSignId", request.userSignId());
        }
        return node;
    }
}
