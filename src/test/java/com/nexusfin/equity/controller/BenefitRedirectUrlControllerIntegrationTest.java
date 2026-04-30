package com.nexusfin.equity.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusfin.equity.dto.response.Result;
import com.nexusfin.equity.exception.BizException;
import com.nexusfin.equity.exception.GlobalExceptionHandler;
import com.nexusfin.equity.service.BenefitRedirectUrlService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BenefitRedirectUrlControllerIntegrationTest {

    @Mock
    private BenefitRedirectUrlService benefitRedirectUrlService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new BenefitRedirectUrlController(benefitRedirectUrlService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(new ObjectMapper()))
                .setValidator(validator)
                .build();
    }

    @Test
    void shouldReturnRedirectUrlPayload() throws Exception {
        when(benefitRedirectUrlService.generate(any()))
                .thenReturn(new BenefitRedirectUrlService.BenefitRedirectUrlResult("https://redirect.test/benefit"));

        mockMvc.perform(post("/api/auth/redrect_benefit_url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "joint-token-redirect-001",
                                  "benefitOrderNo": "BEN-REDIRECT-001"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.redirectUrl").value("https://redirect.test/benefit"));
    }

    @Test
    void shouldReturnControlledBusinessErrorInsteadOfGeneric500() throws Exception {
        when(benefitRedirectUrlService.generate(any()))
                .thenThrow(new BizException("REDRECT_BENEFIT_URL_UPSTREAM_FAILED", "Benefit redirect url is unavailable"));

        mockMvc.perform(post("/api/auth/redrect_benefit_url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "joint-token-redirect-002",
                                  "benefitOrderNo": "BEN-REDIRECT-002"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(-1))
                .andExpect(jsonPath("$.message").value("REDRECT_BENEFIT_URL_UPSTREAM_FAILED:Benefit redirect url is unavailable"));
    }
}
