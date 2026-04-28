package com.nexusfin.equity.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusfin.equity.config.AuthProperties;
import com.nexusfin.equity.exception.GlobalExceptionHandler;
import com.nexusfin.equity.service.JointLoginService;
import com.nexusfin.equity.util.CookieUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class JointLoginControllerIntegrationTest {

    @Mock
    private JointLoginService jointLoginService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AuthProperties authProperties = authProperties();
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new JointLoginController(
                        jointLoginService,
                        new CookieUtil(authProperties)
                ))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(new ObjectMapper()))
                .setValidator(validator)
                .build();
    }

    @Test
    void shouldCreateCookieAndReturnTargetPageForJointLogin() throws Exception {
        when(jointLoginService.login(any())).thenReturn(
                new JointLoginService.JointLoginResult(
                        "jwt-joint-token",
                        "push",
                        "joint-dispatch",
                        "BEN-20260417-001",
                        true
                )
        );

        mockMvc.perform(post("/api/auth/joint-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "joint-token-001",
                                  "scene": "push",
                                  "benefitOrderNo": "BEN-20260417-001"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.scene").value("push"))
                .andExpect(jsonPath("$.data.targetPage").value("joint-dispatch"))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("NEXUSFIN_AUTH=")));
    }

    @Test
    void shouldRejectUnsupportedSceneBeforeCallingService() throws Exception {
        mockMvc.perform(post("/api/auth/joint-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "joint-token-002",
                                  "scene": "unknown_scene"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("scene must be one of [push, exercise, refund]"));

        verifyNoInteractions(jointLoginService);
    }

    private AuthProperties authProperties() {
        AuthProperties authProperties = new AuthProperties();
        AuthProperties.Jwt jwt = new AuthProperties.Jwt();
        jwt.setCookieName("NEXUSFIN_AUTH");
        jwt.setSecret("secret-key-1234567890secret-key-1234567890");
        jwt.setIssuer("nexusfin-equity");
        jwt.setTtlSeconds(7200L);
        jwt.setCookieSecure(true);
        authProperties.setJwt(jwt);
        return authProperties;
    }
}
