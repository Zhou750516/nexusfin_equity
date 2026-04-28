package com.nexusfin.equity.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusfin.equity.config.AuthProperties;
import com.nexusfin.equity.config.JwtAuthenticationFilter;
import com.nexusfin.equity.dto.response.RefundApplyResponse;
import com.nexusfin.equity.dto.response.RefundInfoResponse;
import com.nexusfin.equity.dto.response.RefundResultResponse;
import com.nexusfin.equity.exception.GlobalExceptionHandler;
import com.nexusfin.equity.service.RefundService;
import com.nexusfin.equity.util.JwtUtil;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RefundControllerIntegrationTest {

    @Mock
    private RefundService refundService;

    private MockMvc mockMvc;
    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        AuthProperties authProperties = authProperties("/api/refund");
        jwtUtil = new JwtUtil(authProperties);
        JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter(
                authProperties,
                jwtUtil,
                new ObjectMapper()
        );
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new RefundController(refundService))
                .addFilters(jwtAuthenticationFilter)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(new ObjectMapper()))
                .setValidator(validator)
                .build();
    }

    @Test
    void shouldReturnRefundInfoForBenefitOrder() throws Exception {
        when(refundService.getInfo("BEN-20260418-001"))
                .thenReturn(new RefundInfoResponse(
                        "BEN-20260418-001",
                        true,
                        "NONE",
                        29900L,
                        "refund ready"
                ));

        mockMvc.perform(get("/api/refund/info/BEN-20260418-001")
                        .cookie(authCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.benefitOrderNo").value("BEN-20260418-001"))
                .andExpect(jsonPath("$.data.refundable").value(true));
    }

    @Test
    void shouldAcceptRefundApplyAndReturnRefundId() throws Exception {
        when(refundService.apply("BEN-20260418-001", "USER_REQUEST"))
                .thenReturn(new RefundApplyResponse(
                        "REFUND-BEN-20260418-001",
                        "processing",
                        "refund submitted"
                ));

        mockMvc.perform(post("/api/refund/apply")
                        .cookie(authCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "benefitOrderNo": "BEN-20260418-001",
                                  "reason": "USER_REQUEST"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.refundId").value("REFUND-BEN-20260418-001"))
                .andExpect(jsonPath("$.data.status").value("processing"));
    }

    @Test
    void shouldReturnRefundResultByRefundId() throws Exception {
        when(refundService.getResult("REFUND-BEN-20260418-001"))
                .thenReturn(new RefundResultResponse(
                        "REFUND-BEN-20260418-001",
                        "processing",
                        "refund still processing"
                ));

        mockMvc.perform(get("/api/refund/result/REFUND-BEN-20260418-001")
                        .cookie(authCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.refundId").value("REFUND-BEN-20260418-001"))
                .andExpect(jsonPath("$.data.status").value("processing"));
    }

    private Cookie authCookie() {
        return new Cookie("NEXUSFIN_AUTH", jwtUtil.generateToken("mem-refund-001", "external-refund-001"));
    }

    private AuthProperties authProperties(String protectedPathPrefix) {
        AuthProperties authProperties = new AuthProperties();
        AuthProperties.Jwt jwt = new AuthProperties.Jwt();
        jwt.setCookieName("NEXUSFIN_AUTH");
        jwt.setSecret("secret-key-1234567890secret-key-1234567890");
        jwt.setIssuer("nexusfin-equity");
        jwt.setTtlSeconds(7200L);
        jwt.setCookieSecure(true);
        authProperties.setJwt(jwt);
        authProperties.setProtectedPathPrefixes(java.util.List.of(protectedPathPrefix));
        authProperties.setExcludedPathPrefixes(java.util.List.of());
        return authProperties;
    }
}
