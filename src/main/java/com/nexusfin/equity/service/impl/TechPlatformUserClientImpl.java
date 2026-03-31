package com.nexusfin.equity.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusfin.equity.config.AuthProperties;
import com.nexusfin.equity.dto.response.TechPlatformUserProfileResponse;
import com.nexusfin.equity.exception.BizException;
import com.nexusfin.equity.service.TechPlatformUserClient;
import com.nexusfin.equity.util.TraceIdUtil;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class TechPlatformUserClientImpl implements TechPlatformUserClient {

    private static final Logger log = LoggerFactory.getLogger(TechPlatformUserClientImpl.class);

    private final AuthProperties authProperties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    @Autowired
    public TechPlatformUserClientImpl(AuthProperties authProperties, ObjectMapper objectMapper) {
        this(authProperties, objectMapper, RestClient.builder()
                .requestFactory(requestFactory(authProperties))
                .build());
    }

    TechPlatformUserClientImpl(
            AuthProperties authProperties,
            ObjectMapper objectMapper,
            RestClient restClient
    ) {
        this.authProperties = authProperties;
        this.objectMapper = objectMapper;
        this.restClient = restClient;
    }

    @Override
    public TechPlatformUserProfileResponse getCurrentUser(String techToken) {
        String path = authProperties.getTechPlatformUserMePath();
        URI targetUri = URI.create(authProperties.getTechPlatformBaseUrl() + path);
        int maxAttempts = Math.max(1, authProperties.getRetryMaxAttempts());
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                String rawResponse = restClient.get()
                        .uri(targetUri)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + techToken)
                        .retrieve()
                        .body(String.class);
                TechPlatformUserProfileResponse profile = parseUserProfile(rawResponse);
                log.info("traceId={} techPlatformPath={} attempt={}/{} techPlatformUserId={} upstream verification succeeded",
                        TraceIdUtil.getTraceId(), path, attempt, maxAttempts, profile.userId());
                return profile;
            } catch (RestClientException exception) {
                log.warn("traceId={} techPlatformPath={} attempt={}/{} upstream verification failed cause={}:{}",
                        TraceIdUtil.getTraceId(),
                        path,
                        attempt,
                        maxAttempts,
                        exception.getClass().getSimpleName(),
                        exception.getMessage());
                if (attempt == maxAttempts) {
                    throw new BizException("UPSTREAM_AUTH_FAILED", "Failed to verify tech platform token");
                }
            } catch (BizException exception) {
                log.warn("traceId={} techPlatformPath={} upstream verification rejected message={}",
                        TraceIdUtil.getTraceId(), path, exception.getMessage());
                throw exception;
            }
        }
        throw new BizException("UPSTREAM_AUTH_FAILED", "Failed to verify tech platform token");
    }

    private static SimpleClientHttpRequestFactory requestFactory(AuthProperties authProperties) {
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
                    textValue(payload, "idCard"),
                    textValue(payload, "channelCode")
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
