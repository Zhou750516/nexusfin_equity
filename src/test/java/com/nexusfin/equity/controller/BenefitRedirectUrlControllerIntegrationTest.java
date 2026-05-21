package com.nexusfin.equity.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusfin.equity.dto.response.Result;
import com.nexusfin.equity.exception.BizException;
import com.nexusfin.equity.exception.GlobalExceptionHandler;
import com.nexusfin.equity.service.BenefitRedirectUrlService;
import com.nexusfin.equity.util.AuthContextUtil;
import com.nexusfin.equity.util.AuthPrincipal;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BenefitRedirectUrlControllerIntegrationTest {

    @Mock
    private BenefitRedirectUrlService benefitRedirectUrlService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AuthContextUtil.clear();
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

    @Test
    void shouldRedirectAuthenticatedGetRequestToQwExerciseUrl() throws Exception {
        AuthContextUtil.bind(new AuthPrincipal("mem-redirect-001", "tech-user-redirect-001"));
        when(benefitRedirectUrlService.generateForMember("mem-redirect-001", "QW-ORDER-001"))
                .thenReturn(new BenefitRedirectUrlService.BenefitRedirectUrlResult("https://qw.test/exercise"));

        mockMvc.perform(get("/api/auth/redrect_benefit_url")
                        .param("benefitOrderNo", "QW-ORDER-001"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://qw.test/exercise"));

        verify(benefitRedirectUrlService).generateForMember("mem-redirect-001", "QW-ORDER-001");
    }

    @Test
    void shouldRejectGetRequestWithoutAuthenticatedMember() throws Exception {
        mockMvc.perform(get("/api/auth/redrect_benefit_url")
                        .param("benefitOrderNo", "QW-ORDER-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void shouldReturnControlledErrorWhenBenefitOrderBelongsToAnotherMember() throws Exception {
        AuthContextUtil.bind(new AuthPrincipal("mem-redirect-other", "tech-user-redirect-other"));
        when(benefitRedirectUrlService.generateForMember("mem-redirect-other", "QW-ORDER-OTHER"))
                .thenThrow(new BizException(403, "BENEFIT_REDIRECT_FORBIDDEN", "Benefit order does not belong to current member"));

        mockMvc.perform(get("/api/auth/redrect_benefit_url")
                        .param("benefitOrderNo", "QW-ORDER-OTHER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("Benefit order does not belong to current member"));
    }
}
