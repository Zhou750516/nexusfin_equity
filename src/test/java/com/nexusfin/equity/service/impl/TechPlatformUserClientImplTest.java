package com.nexusfin.equity.service.impl;

import com.nexusfin.equity.config.AuthProperties;
import com.nexusfin.equity.dto.response.TechPlatformUserProfileResponse;
import com.nexusfin.equity.exception.BizException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(OutputCaptureExtension.class)
class TechPlatformUserClientImplTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldRetryTechPlatformUserLookupAfterTransientFailure() {
        AuthProperties authProperties = authProperties();
        authProperties.setRetryMaxAttempts(2);
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();

        server.expect(requestTo("https://tech-platform.test/api/users/me"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer tech-token"))
                .andRespond(withServerError());
        server.expect(requestTo("https://tech-platform.test/api/users/me"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer tech-token"))
                .andRespond(withSuccess("""
                        {
                          "data": {
                            "userId": "tech-user-001",
                            "phone": "13800138000",
                            "realName": "张三",
                            "idCard": "310101199001011111",
                            "channelCode": "KJ"
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        TechPlatformUserClientImpl client = new TechPlatformUserClientImpl(authProperties, objectMapper, restClientBuilder.build());

        TechPlatformUserProfileResponse response = client.getCurrentUser("tech-token");

        assertThat(response.userId()).isEqualTo("tech-user-001");
        server.verify();
    }

    @Test
    void shouldLogAttemptsAndThrowAfterRetryExhausted(CapturedOutput output) {
        AuthProperties authProperties = authProperties();
        authProperties.setRetryMaxAttempts(2);
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();

        server.expect(requestTo("https://tech-platform.test/api/users/me"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withServerError());
        server.expect(requestTo("https://tech-platform.test/api/users/me"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withServerError());

        TechPlatformUserClientImpl client = new TechPlatformUserClientImpl(authProperties, objectMapper, restClientBuilder.build());

        assertThatThrownBy(() -> client.getCurrentUser("tech-token"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("UPSTREAM_AUTH_FAILED");
        assertThat(output).contains("techPlatformPath=/api/users/me attempt=1/2 upstream verification failed");
        assertThat(output).contains("techPlatformPath=/api/users/me attempt=2/2 upstream verification failed");
        server.verify();
    }

    private AuthProperties authProperties() {
        AuthProperties authProperties = new AuthProperties();
        authProperties.setTechPlatformBaseUrl("https://tech-platform.test");
        authProperties.setTechPlatformUserMePath("/api/users/me");
        authProperties.setConnectTimeoutMs(2000);
        authProperties.setReadTimeoutMs(3000);
        return authProperties;
    }
}
