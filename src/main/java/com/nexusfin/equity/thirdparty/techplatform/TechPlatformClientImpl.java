package com.nexusfin.equity.thirdparty.techplatform;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusfin.equity.config.TechPlatformProperties;
import com.nexusfin.equity.exception.BizException;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

public class TechPlatformClientImpl implements TechPlatformClient {

    private static final Logger log = LoggerFactory.getLogger(TechPlatformClientImpl.class);

    private final TechPlatformProperties properties;
    private final ObjectMapper objectMapper;
    private final TechPlatformPayloadCodec payloadCodec;
    private final RestClient restClient;

    public TechPlatformClientImpl(TechPlatformProperties properties, ObjectMapper objectMapper) {
        this(properties, objectMapper, RestClient.builder().requestFactory(requestFactory(properties)));
    }

    public TechPlatformClientImpl(
            TechPlatformProperties properties,
            ObjectMapper objectMapper,
            RestClient.Builder restClientBuilder
    ) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.payloadCodec = new TechPlatformPayloadCodec(properties, objectMapper);
        this.restClient = restClientBuilder.build();
    }

    @Override
    public TechPlatformNotifyResponse notifyCreditStatus(CreditStatusNoticeRequest request) {
        ensureEnabled();
        if (properties.getMode() == TechPlatformProperties.Mode.MOCK) {
            return mockResponse("creditStatusNotice");
        }
        return invoke(properties.getPaths().getCreditStatusNotice(), request);
    }

    @Override
    public TechPlatformNotifyResponse notifyLoanInfo(LoanInfoNoticeRequest request) {
        ensureEnabled();
        if (properties.getMode() == TechPlatformProperties.Mode.MOCK) {
            return mockResponse("loanInfoNotice");
        }
        return invoke(properties.getPaths().getLoanInfoNotice(), request);
    }

    @Override
    public TechPlatformNotifyResponse notifyRepaymentInfo(RepayInfoNoticeRequest request) {
        ensureEnabled();
        if (properties.getMode() == TechPlatformProperties.Mode.MOCK) {
            return mockResponse("repayInfoNotice");
        }
        return invoke(properties.getPaths().getRepayInfoNotice(), request);
    }

    private TechPlatformNotifyResponse invoke(String path, Object businessRequest) {
        String param = payloadCodec.encrypt(businessRequest);
        String timestamp = String.valueOf(System.currentTimeMillis());
        String sign = payloadCodec.sign(timestamp, param);
        try {
            String rawResponse = restClient.post()
                    .uri(URI.create(properties.getBaseUrl() + path))
                    .header("channelId", properties.getChannelId())
                    .header("timestamp", timestamp)
                    .header("sign", sign)
                    .header("version", properties.getVersion())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new TechPlatformRequestBody(param))
                    .retrieve()
                    .body(String.class);
            TechPlatformNotifyResponse response = parseNotifyResponse(rawResponse);
            log.info("traceId={} techPlatformPath={} techPlatformCode={}",
                    com.nexusfin.equity.util.TraceIdUtil.getTraceId(),
                    path,
                    response.code());
            return response;
        } catch (RestClientException exception) {
            throw new BizException("TECH_PLATFORM_UPSTREAM_FAILED", "Failed to call tech platform API");
        }
    }

    TechPlatformNotifyResponse parseNotifyResponse(String rawResponse) {
        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            if (root.hasNonNull("param")) {
                root = objectMapper.readTree(payloadCodec.decrypt(root.path("param").asText()));
            }
            String code = root.path("code").asText();
            String msg = root.path("msg").asText("");
            if (!"0".equals(code) && !"000000".equals(code)) {
                throw new BizException("TECH_PLATFORM_REJECTED", "Tech platform rejected request: " + msg);
            }
            return new TechPlatformNotifyResponse(code, msg);
        } catch (BizException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BizException("TECH_PLATFORM_RESPONSE_INVALID", "Failed to parse tech platform response");
        }
    }

    private void ensureEnabled() {
        if (!properties.isEnabled()) {
            throw new BizException("TECH_PLATFORM_DISABLED", "Tech platform integration is disabled");
        }
    }

    private TechPlatformNotifyResponse mockResponse(String apiName) {
        return new TechPlatformNotifyResponse("0", "mock success: " + apiName);
    }

    private static SimpleClientHttpRequestFactory requestFactory(TechPlatformProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getConnectTimeoutMs());
        requestFactory.setReadTimeout(properties.getReadTimeoutMs());
        return requestFactory;
    }

    private record TechPlatformRequestBody(
            String param
    ) {
    }
}
