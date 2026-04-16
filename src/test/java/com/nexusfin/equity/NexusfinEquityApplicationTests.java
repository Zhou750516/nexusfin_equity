package com.nexusfin.equity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusfin.equity.dto.response.TechPlatformUserProfileResponse;
import com.nexusfin.equity.entity.MemberPaymentProtocol;
import com.nexusfin.equity.repository.MemberPaymentProtocolRepository;
import com.nexusfin.equity.service.ReconciliationService;
import com.nexusfin.equity.service.TechPlatformUserClient;
import com.nexusfin.equity.service.XiaohuaAuthClient;
import com.nexusfin.equity.util.SignatureUtil;
import java.time.LocalDateTime;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
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

    @Autowired
    private MemberPaymentProtocolRepository memberPaymentProtocolRepository;

    @MockBean
    private TechPlatformUserClient techPlatformUserClient;

    @MockBean
    private XiaohuaAuthClient xiaohuaAuthClient;

    @Test
    void shouldCompleteQuickstartSmokeFlow() throws Exception {
        when(techPlatformUserClient.getCurrentUser("tech-token-quickstart")).thenReturn(
                new TechPlatformUserProfileResponse("quickstart-user-001", "13800138000", "李四", "310101199001011111", "KJ")
        );

        MvcResult ssoResult = mockMvc.perform(get("/api/auth/sso-callback")
                        .param("token", "tech-token-quickstart")
                        .param("redirect_url", "/equity/index"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/equity/index"))
                .andReturn();

        jakarta.servlet.http.Cookie authCookie = ssoResult.getResponse().getCookie("NEXUSFIN_AUTH");
        assertThat(authCookie).isNotNull();

        MvcResult currentUserResult = mockMvc.perform(get("/api/users/me")
                        .cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.techPlatformUserId").value("quickstart-user-001"))
                .andReturn();
        JsonNode currentUserJson = objectMapper.readTree(currentUserResult.getResponse().getContentAsString());
        String memberId = currentUserJson.path("data").path("memberId").asText();
        createActiveProtocol(memberId, "quickstart-user-001");

        mockMvc.perform(get("/api/equity/products/{productCode}", "QS-PROD-001")
                        .cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.productCode").value("QS-PROD-001"))
                .andExpect(jsonPath("$.data.memberId").value(memberId));

        MvcResult createOrderResult = mockMvc.perform(post("/api/equity/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .cookie(authCookie)
                        .content(createOrderRequest()))
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

        mockMvc.perform(get("/api/equity/orders/{benefitOrderNo}", benefitOrderNo)
                        .cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderStatus").value("EXERCISE_PENDING"))
                .andExpect(jsonPath("$.data.grantStatus").value("SUCCESS"));

        mockMvc.perform(get("/api/equity/exercise-url/{benefitOrderNo}", benefitOrderNo)
                        .cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.exerciseUrl").value(org.hamcrest.Matchers.containsString("https://mock-qw.test/exercise")))
                .andExpect(jsonPath("$.data.exerciseUrl").value(org.hamcrest.Matchers.containsString(benefitOrderNo)))
                .andExpect(jsonPath("$.data.expireTime").isNotEmpty());

        assertThat(reconciliationService.queryOrderByBenefitOrderNo(benefitOrderNo)).isNotNull();
        assertThat(reconciliationService.queryOrdersByMemberId(memberId)).hasSize(1);
        assertThat(reconciliationService.queryByRequestId(grantRequestId)).hasSize(1);
        assertThat(reconciliationService.queryByBenefitOrderNo(benefitOrderNo)).hasSize(1);
    }

    @Test
    void shouldCompleteJointLoginSmokeFlow() throws Exception {
        when(xiaohuaAuthClient.exchange("joint-token-quickstart")).thenReturn(
                new XiaohuaAuthClient.JointIdentity(
                        "xh-quickstart-001",
                        "13800138001",
                        "王五",
                        "310101199001011112",
                        "KJ"
                )
        );

        MvcResult loginResult = mockMvc.perform(post("/api/auth/joint-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "joint-token-quickstart",
                                  "scene": "push",
                                  "benefitOrderNo": "BEN-SMOKE-001"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.targetPage").value("joint-dispatch"))
                .andReturn();

        jakarta.servlet.http.Cookie authCookie = loginResult.getResponse().getCookie("NEXUSFIN_AUTH");
        assertThat(authCookie).isNotNull();

        mockMvc.perform(get("/api/users/me").cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.techPlatformUserId").value("xh-quickstart-001"));
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

    private String createOrderRequest() {
        return """
                {
                  "requestId": "req-quickstart-create-order",
                  "productCode": "QS-PROD-001",
                  "loanAmount": 880000,
                  "agreementSigned": true
                }
                """;
    }

    private void createActiveProtocol(String memberId, String externalUserId) {
        MemberPaymentProtocol protocol = new MemberPaymentProtocol();
        protocol.setMemberId(memberId);
        protocol.setExternalUserId(externalUserId);
        protocol.setProviderCode("ALLINPAY");
        protocol.setProtocolNo("AIP-TEST-" + memberId);
        protocol.setProtocolStatus("ACTIVE");
        protocol.setChannelCode("KJ");
        protocol.setSignedTs(LocalDateTime.now());
        protocol.setLastVerifiedTs(LocalDateTime.now());
        protocol.setCreatedTs(LocalDateTime.now());
        protocol.setUpdatedTs(LocalDateTime.now());
        memberPaymentProtocolRepository.insert(protocol);
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
