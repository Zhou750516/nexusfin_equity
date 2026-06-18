package com.nexusfin.equity.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusfin.equity.config.AuthProperties;
import com.nexusfin.equity.dto.request.RepaymentLoginRequest;
import com.nexusfin.equity.exception.BizException;
import com.nexusfin.equity.exception.GlobalExceptionHandler;
import com.nexusfin.equity.service.RepaymentLoginService;
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
class RepaymentLoginControllerIntegrationTest {

    @Mock
    private RepaymentLoginService repaymentLoginService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new RepaymentLoginController(
                        repaymentLoginService,
                        new CookieUtil(authProperties())
                ))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(new ObjectMapper()))
                .setValidator(validator)
                .build();
    }

    @Test
    void shouldCreateCookieForRepaymentLogin() throws Exception {
        when(repaymentLoginService.login(any())).thenReturn(
                new RepaymentLoginService.RepaymentLoginResult("jwt-repayment-token", 1781594032)
        );

        mockMvc.perform(post("/api/auth/repayment-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "joint-token-001",
                                  "loanId": 1781594032
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.authenticated").value(true))
                .andExpect(jsonPath("$.data.loanId").value(1781594032))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("NEXUSFIN_AUTH=")));
    }

    @Test
    void shouldRejectInvalidTokenWithoutCookie() throws Exception {
        when(repaymentLoginService.login(any()))
                .thenThrow(new BizException("REPAYMENT_LOGIN_TOKEN_INVALID", "Repayment login session expired"));

        mockMvc.perform(post("/api/auth/repayment-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "expired-token",
                                  "loanId": 1781594032
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(-1))
                .andExpect(jsonPath("$.message")
                        .value("REPAYMENT_LOGIN_TOKEN_INVALID:Repayment login session expired"))
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
    }

    @Test
    void shouldRejectMissingLoanIdBeforeCallingService() throws Exception {
        mockMvc.perform(post("/api/auth/repayment-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "joint-token-001"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("loanId loanId must not be null"));

        verifyNoInteractions(repaymentLoginService);
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
