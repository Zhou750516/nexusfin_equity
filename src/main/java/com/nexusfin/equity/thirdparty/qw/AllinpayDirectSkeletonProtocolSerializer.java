package com.nexusfin.equity.thirdparty.qw;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusfin.equity.exception.BizException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.MediaType;

public class AllinpayDirectSkeletonProtocolSerializer implements AllinpayDirectProtocolSerializer {

    private final ObjectMapper objectMapper;

    public AllinpayDirectSkeletonProtocolSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public AllinpayDirectSerializedRequest serialize(AllinpayDirectEnvelope envelope) {
        try {
            Map<String, Object> skeletonRequest = new LinkedHashMap<>();
            skeletonRequest.put("operation", envelope.operation().name());
            skeletonRequest.put("targetUri", envelope.targetUri().toString());

            Map<String, Object> head = new LinkedHashMap<>();
            head.put("serviceCode", envelope.head().serviceCode());
            head.put("merchantId", envelope.head().merchantId());
            head.put("userName", envelope.head().userName());
            head.put("userPassword", envelope.head().userPassword());
            head.put("timestamp", envelope.head().timestamp());
            skeletonRequest.put("head", head);
            skeletonRequest.put("businessPayload", envelope.businessPayload());

            String requestBody = objectMapper.writeValueAsString(skeletonRequest);
            return new AllinpayDirectSerializedRequest(
                    envelope.targetUri(),
                    MediaType.APPLICATION_JSON,
                    requestBody,
                    requestBody
            );
        } catch (JsonProcessingException exception) {
            throw new BizException(
                    "ALLINPAY_DIRECT_SERIALIZE_FAILED",
                    "Failed to serialize allinpay direct skeleton request"
            );
        }
    }
}
