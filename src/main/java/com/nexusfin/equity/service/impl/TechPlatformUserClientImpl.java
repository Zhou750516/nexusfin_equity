package com.nexusfin.equity.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusfin.equity.config.AuthProperties;
import com.nexusfin.equity.dto.response.TechPlatformUserProfileResponse;
import com.nexusfin.equity.exception.BizException;
import com.nexusfin.equity.service.TechPlatformUserClient;
import java.net.URI;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class TechPlatformUserClientImpl implements TechPlatformUserClient {

    private final AuthProperties authProperties;
    private final ObjectMapper objectMapper;

    public TechPlatformUserClientImpl(AuthProperties authProperties, ObjectMapper objectMapper) {
        this.authProperties = authProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public TechPlatformUserProfileResponse getCurrentUser(String techToken) {
        try {
            RestClient restClient = RestClient.builder()
                    .requestFactory(requestFactory())
                    .build();
            String rawResponse = restClient.get()
                    .uri(URI.create(authProperties.getTechPlatformBaseUrl() + authProperties.getTechPlatformUserMePath()))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + techToken)
                    .retrieve()
                    .body(String.class);
            return parseUserProfile(rawResponse);
        } catch (RestClientException exception) {
            throw new BizException("UPSTREAM_AUTH_FAILED", "Failed to verify tech platform token");
        }
    }

    private SimpleClientHttpRequestFactory requestFactory() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(authProperties.getConnectTimeoutMs());
        requestFactory.setReadTimeout(authProperties.getReadTimeoutMs());
        return requestFactory;
    }

    private TechPlatformUserProfileResponse parseUserProfile(String rawResponse) {
        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            JsonNode payload = root.has("data") ? root.get("data") : root;
            String userId = textValue(payload, "userId");
            if (userId == null || userId.isBlank()) {
                throw new BizException("UPSTREAM_AUTH_INVALID", "Tech platform userId is missing");
            }
            return new TechPlatformUserProfileResponse(
                    userId,
                    textValue(payload, "phone"),
                    textValue(payload, "realName"),
                    textValue(payload, "idCard")
            );
        } catch (BizException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BizException("UPSTREAM_AUTH_INVALID", "Failed to parse tech platform user profile");
        }
    }

    private String textValue(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        return value == null || value.isNull() ? null : value.asText();
    }
}
