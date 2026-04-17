package com.nexusfin.equity.controller;

import com.nexusfin.equity.dto.response.RefundInfoResponse;
import com.nexusfin.equity.dto.response.RefundApplyResponse;
import com.nexusfin.equity.dto.response.RefundResultResponse;
import com.nexusfin.equity.service.RefundService;
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
class RefundControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @MockBean
    private RefundService refundService;

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
}
