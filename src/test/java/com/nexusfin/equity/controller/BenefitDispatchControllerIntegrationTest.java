package com.nexusfin.equity.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusfin.equity.config.AuthProperties;
import com.nexusfin.equity.config.JwtAuthenticationFilter;
import com.nexusfin.equity.dto.response.BenefitDispatchContextResponse;
import com.nexusfin.equity.dto.response.BenefitDispatchResolveResponse;
import com.nexusfin.equity.exception.GlobalExceptionHandler;
import com.nexusfin.equity.service.BenefitDispatchService;
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
class BenefitDispatchControllerIntegrationTest {

    @Mock
    private BenefitDispatchService benefitDispatchService;

    private MockMvc mockMvc;
    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        AuthProperties authProperties = authProperties("/api/benefit-dispatch");
        jwtUtil = new JwtUtil(authProperties);
        JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter(
                authProperties,
                jwtUtil,
                new ObjectMapper()
        );
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new BenefitDispatchController(benefitDispatchService))
                .addFilters(jwtAuthenticationFilter)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(new ObjectMapper()))
                .setValidator(validator)
                .build();
    }

    @Test
    void shouldReturnDispatchContextForPushScene() throws Exception {
        when(benefitDispatchService.getContext("BEN-20260418-001"))
                .thenReturn(new BenefitDispatchContextResponse(
                        "BEN-20260418-001",
                        "push",
                        "ACTIVE",
                        true,
                        "INTERMEDIATE",
                        "ready"
                ));

        mockMvc.perform(get("/api/benefit-dispatch/context/BEN-20260418-001")
                        .cookie(authCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.benefitOrderNo").value("BEN-20260418-001"))
                .andExpect(jsonPath("$.data.allowRedirect").value(true))
                .andExpect(jsonPath("$.data.scene").value("push"));
    }

    @Test
    void shouldResolveDispatchTargetForDirectRedirect() throws Exception {
        when(benefitDispatchService.resolve("BEN-20260418-001"))
                .thenReturn(new BenefitDispatchResolveResponse(
                        "BEN-20260418-001",
                        true,
                        "DIRECT",
                        "https://supplier.example/benefit",
                        "ready"
                ));

        mockMvc.perform(post("/api/benefit-dispatch/resolve")
                        .cookie(authCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "benefitOrderNo": "BEN-20260418-001"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.allowRedirect").value(true))
                .andExpect(jsonPath("$.data.redirectMode").value("DIRECT"))
                .andExpect(jsonPath("$.data.supplierUrl").value("https://supplier.example/benefit"));
    }

    private Cookie authCookie() {
        return new Cookie("NEXUSFIN_AUTH", jwtUtil.generateToken("mem-dispatch-001", "external-dispatch-001"));
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
