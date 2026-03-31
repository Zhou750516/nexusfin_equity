package com.nexusfin.equity.thirdparty.qw;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class AllinpayMemberSyncPayloadMapper implements AllinpayDirectPayloadMapper<QwMemberSyncRequest> {

    @Override
    public AllinpayDirectOperation operation() {
        return AllinpayDirectOperation.MEMBER_SYNC;
    }

    @Override
    public Class<QwMemberSyncRequest> requestType() {
        return QwMemberSyncRequest.class;
    }

    @Override
    public JsonNode map(QwMemberSyncRequest request, ObjectMapper objectMapper) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("uniqueId", request.uniqueId());
        node.put("partnerOrderNo", request.partnerOrderNo());
        if (request.payAmount() != null) {
            node.put("payAmount", request.payAmount());
        }
        node.put("productCode", request.productCode());
        node.put("productName", request.productName());
        node.put("mobile", request.mobile());
        node.put("username", request.username());
        node.put("payProtocolNo", request.payProtocolNo());
        if (request.cardNo() != null) {
            node.put("cardNo", request.cardNo());
        }
        if (request.shareFlag() != null) {
            node.put("shareFlag", request.shareFlag());
        }
        if (request.partnerMerchantNo() != null) {
            node.put("partnerMerchantNo", request.partnerMerchantNo());
        }
        if (request.partnerShareAmount() != null) {
            node.put("partnerShareAmount", request.partnerShareAmount());
        }
        if (request.shareMerchantNo() != null) {
            node.put("shareMerchantNo", request.shareMerchantNo());
        }
        if (request.shareAmount() != null) {
            node.put("shareAmount", request.shareAmount());
        }
        return node;
    }
}
