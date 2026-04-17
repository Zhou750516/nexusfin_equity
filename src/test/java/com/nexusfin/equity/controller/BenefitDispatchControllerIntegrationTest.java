package com.nexusfin.equity.controller;

import com.nexusfin.equity.dto.response.BenefitDispatchContextResponse;
import com.nexusfin.equity.dto.response.BenefitDispatchResolveResponse;
import com.nexusfin.equity.service.BenefitDispatchService;
import com.nexusfin.equity.util.JwtUtil;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BenefitDispatchControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @MockBean
    private BenefitDispatchService benefitDispatchService;

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
}
