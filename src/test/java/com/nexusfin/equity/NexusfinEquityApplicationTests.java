package com.nexusfin.equity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusfin.equity.service.ReconciliationService;
import com.nexusfin.equity.util.SignatureUtil;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = "classpath:db/test-data.sql")
class NexusfinEquityApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ReconciliationService reconciliationService;

    @Test
    void shouldCompleteQuickstartSmokeFlow() throws Exception {
        String registerRequestId = "req-quickstart-register";
        MvcResult registerResult = mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .headers(signatureHeaders("nonce-quickstart-register"))
                        .content(registerRequest(registerRequestId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.registerStatus").value("SUCCESS"))
                .andReturn();

        JsonNode registerJson = objectMapper.readTree(registerResult.getResponse().getContentAsString());
        String memberId = registerJson.path("data").path("memberId").asText();
        assertThat(memberId).isNotBlank();

        mockMvc.perform(get("/api/equity/products/{productCode}", "QS-PROD-001")
                        .param("memberId", memberId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.productCode").value("QS-PROD-001"))
                .andExpect(jsonPath("$.data.memberId").value(memberId));

        MvcResult createOrderResult = mockMvc.perform(post("/api/equity/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createOrderRequest(memberId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderStatus").value("FIRST_DEDUCT_PENDING"))
                .andReturn();

        JsonNode orderJson = objectMapper.readTree(createOrderResult.getResponse().getContentAsString());
        String benefitOrderNo = orderJson.path("data").path("benefitOrderNo").asText();
        assertThat(benefitOrderNo).isNotBlank();

        mockMvc.perform(post("/api/callbacks/first-deduction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .headers(signatureHeaders("nonce-quickstart-first-deduct"))
                        .content(firstDeductRequest(benefitOrderNo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paymentType").value("FIRST_DEDUCT"))
                .andExpect(jsonPath("$.data.paymentStatus").value("SUCCESS"));

        String grantRequestId = "req-quickstart-grant";
        mockMvc.perform(post("/api/callbacks/grant/forward")
                        .contentType(MediaType.APPLICATION_JSON)
                        .headers(signatureHeaders("nonce-quickstart-grant"))
                        .content(grantRequest(grantRequestId, benefitOrderNo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/equity/orders/{benefitOrderNo}", benefitOrderNo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderStatus").value("EXERCISE_PENDING"))
                .andExpect(jsonPath("$.data.grantStatus").value("SUCCESS"));

        mockMvc.perform(get("/api/equity/exercise-url/{benefitOrderNo}", benefitOrderNo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.exerciseUrl").value("https://abs.example.com/exercise/" + benefitOrderNo));

        assertThat(reconciliationService.queryOrderByBenefitOrderNo(benefitOrderNo)).isNotNull();
        assertThat(reconciliationService.queryOrdersByMemberId(memberId)).hasSize(1);
        assertThat(reconciliationService.queryByRequestId(grantRequestId)).hasSize(1);
        assertThat(reconciliationService.queryByBenefitOrderNo(benefitOrderNo)).hasSize(1);
    }

    private HttpHeaders signatureHeaders(String nonce) {
        String appId = "test-app";
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String signature = SignatureUtil.sign(appId, timestamp, nonce, "test-secret");
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-App-Id", appId);
        headers.add("X-Timestamp", timestamp);
        headers.add("X-Nonce", nonce);
        headers.add("X-Signature", signature);
        return headers;
    }

    private String registerRequest(String requestId) {
        return """
                {
                  "requestId": "%s",
                  "channelCode": "KJ",
                  "userInfo": {
                    "externalUserId": "quickstart-user-001",
                    "mobileEncrypted": "13800138000",
                    "idCardEncrypted": "310101199001011111",
                    "realNameEncrypted": "李四"
                  }
                }
                """.formatted(requestId);
    }

    private String createOrderRequest(String memberId) {
        return """
                {
                  "memberId": "%s",
                  "productCode": "QS-PROD-001",
                  "loanAmount": 880000,
                  "agreementSigned": true
                }
                """.formatted(memberId);
    }

    private String firstDeductRequest(String benefitOrderNo) {
        return """
                {
                  "requestId": "req-quickstart-first-deduct",
                  "benefitOrderNo": "%s",
                  "qwTradeNo": "qw-quickstart-first-deduct",
                  "deductStatus": "SUCCESS",
                  "deductAmount": 880000,
                  "failReason": null,
                  "deductTime": "2026-03-23T20:00:00"
                }
                """.formatted(benefitOrderNo);
    }

    private String grantRequest(String requestId, String benefitOrderNo) {
        return """
                {
                  "requestId": "%s",
                  "benefitOrderNo": "%s",
                  "grantStatus": "SUCCESS",
                  "actualAmount": 880000,
                  "loanOrderNo": "loan-quickstart-001",
                  "failReason": null,
                  "grantTime": "2026-03-23T20:30:00",
                  "timestamp": 1774269000
                }
                """.formatted(requestId, benefitOrderNo);
    }
}
