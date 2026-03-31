package com.nexusfin.equity.thirdparty.qw;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusfin.equity.exception.BizException;
import java.util.Arrays;
import java.util.List;

public class AllinpayDirectPayloadMapperRegistry {

    private final ObjectMapper objectMapper;
    private final List<AllinpayDirectPayloadMapper<?>> mappers;

    public AllinpayDirectPayloadMapperRegistry(
            ObjectMapper objectMapper,
            AllinpayDirectPayloadMapper<?>... mappers
    ) {
        this.objectMapper = objectMapper;
        this.mappers = Arrays.asList(mappers);
    }

    public JsonNode map(AllinpayDirectOperation operation, Object request) {
        AllinpayDirectPayloadMapper<?> mapper = mappers.stream()
                .filter(candidate -> candidate.operation() == operation)
                .filter(candidate -> candidate.requestType().isInstance(request))
                .findFirst()
                .orElseThrow(() -> new BizException(
                        "ALLINPAY_DIRECT_PAYLOAD_MAPPER_MISSING",
                        "Missing payload mapper for operation: " + operation
                ));
        return mapUnchecked(mapper, request);
    }

    @SuppressWarnings("unchecked")
    private <T> JsonNode mapUnchecked(AllinpayDirectPayloadMapper<T> mapper, Object request) {
        return mapper.map((T) request, objectMapper);
    }
}
