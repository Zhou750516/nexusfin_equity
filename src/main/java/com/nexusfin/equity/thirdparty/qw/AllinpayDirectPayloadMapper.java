package com.nexusfin.equity.thirdparty.qw;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public interface AllinpayDirectPayloadMapper<T> {

    AllinpayDirectOperation operation();

    Class<T> requestType();

    JsonNode map(T request, ObjectMapper objectMapper);
}
