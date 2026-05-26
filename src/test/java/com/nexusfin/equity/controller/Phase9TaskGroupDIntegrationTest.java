package com.nexusfin.equity.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusfin.equity.entity.LoanApplicationMapping;
import com.nexusfin.equity.entity.MemberInfo;
import com.nexusfin.equity.entity.MemberReceivingAccount;
import com.nexusfin.equity.enums.MemberStatusEnum;
import com.nexusfin.equity.repository.BenefitOrderRepository;
import com.nexusfin.equity.repository.IdempotencyRecordRepository;
import com.nexusfin.equity.repository.LoanApplicationMappingRepository;
import com.nexusfin.equity.repository.MemberChannelRepository;
import com.nexusfin.equity.repository.MemberInfoRepository;
import com.nexusfin.equity.repository.MemberPaymentProtocolRepository;
import com.nexusfin.equity.repository.MemberReceivingAccountRepository;
import com.nexusfin.equity.repository.SignTaskRepository;
import com.nexusfin.equity.support.AbstractYunkaXiaohuaIT;
import com.nexusfin.equity.thirdparty.yunka.LoanRepayPlanItem;
import com.nexusfin.equity.thirdparty.yunka.LoanRepayPlanResponse;
import com.nexusfin.equity.thirdparty.yunka.YunkaGatewayClient;
import com.nexusfin.equity.util.JwtUtil;
import com.nexusfin.equity.util.SensitiveDataCipher;
import jakarta.servlet.http.Cookie;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(OutputCaptureExtension.class)
class Phase9TaskGroupDIntegrationTest extends AbstractYunkaXiaohuaIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MemberInfoRepository memberInfoRepository;

    @Autowired
    private MemberChannelRepository memberChannelRepository;

    @Autowired
    private MemberPaymentProtocolRepository memberPaymentProtocolRepository;

    @Autowired
    private BenefitOrderRepository benefitOrderRepository;

    @Autowired
    private SignTaskRepository signTaskRepository;

    @Autowired
    private com.nexusfin.equity.repository.ContractArchiveRepository contractArchiveRepository;

    @Autowired
    private IdempotencyRecordRepository idempotencyRecordRepository;

    @Autowired
    private LoanApplicationMappingRepository loanApplicationMappingRepository;

    @Autowired
    private MemberReceivingAccountRepository memberReceivingAccountRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private SensitiveDataCipher sensitiveDataCipher;

    @BeforeEach
    void setUp() {
        contractArchiveRepository.delete(null);
        signTaskRepository.delete(null);
        benefitOrderRepository.delete(null);
        idempotencyRecordRepository.delete(null);
        memberPaymentProtocolRepository.delete(null);
        memberChannelRepository.delete(null);
        loanApplicationMappingRepository.delete(null);
        memberReceivingAccountRepository.delete(null);
        memberInfoRepository.delete(null);
    }

    @Test
    void shouldForwardRepaymentInfoToYunkaRepayTrial(CapturedOutput output) throws Exception {
        MemberInfo memberInfo = createMember("mem-repay-info", "user-repay-info");
        insertReceivingAccount(memberInfo.getMemberId(), "acc-repay-info-001", "招商银行", "8648");
        createApplicationMapping(memberInfo, "APP-REPAY-INFO-001", 2026041301);
        when(xiaohuaGatewayService.queryLoanRepayPlan(any(), eq("2026041301"), any()))
                .thenReturn(new LoanRepayPlanResponse(List.of(
                        new LoanRepayPlanItem(3, 3, 1, "2026-07-07", 100000L, 3000L, 103000L),
                        new LoanRepayPlanItem(1, 1, 1, "2026-05-07", 100000L, 4500L, 104500L),
                        new LoanRepayPlanItem(2, 2, 1, "2026-06-07", 100000L, 3000L, 103000L),
                        new LoanRepayPlanItem(2, 2, 2, "2026-06-07", 100000L, 3000L, 103000L)
                )));
        JsonNode yunkaData = objectMapper.readTree("""
                {
                  "repayAmount": 1018.50,
                  "repayPrincipal": 1000.00,
                  "repayInterest": 18.50,
                  "repayPenaltyInt": 0,
                  "discount": 26.50,
                  "remark": "试算成功，请确认还款"
                }
                """);
        when(yunkaGatewayClient.proxy(any()))
                .thenReturn(new YunkaGatewayClient.YunkaGatewayResponse(0, "SUCCESS", yunkaData));

        mockMvc.perform(get("/api/repayment/info/2026041301")
                        .cookie(authCookie(memberInfo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.loanId").value(2026041301))
                .andExpect(jsonPath("$.data.repaymentAmount").value(1018.5))
                .andExpect(jsonPath("$.data.repaymentType").value("当前应还"))
                .andExpect(jsonPath("$.data.remark").value("试算成功，请确认还款"))
                .andExpect(jsonPath("$.data.tip").value("试算成功，请确认还款"))
                .andExpect(jsonPath("$.data.fees.repayPrincipal").value(1000.0))
                .andExpect(jsonPath("$.data.fees.repayInterest").value(18.5))
                .andExpect(jsonPath("$.data.fees.discount").value(26.5))
                .andExpect(jsonPath("$.data.bankCard.bankName").value("招商银行"))
                .andExpect(jsonPath("$.data.bankCard.lastFour").value("8648"));

        ArgumentCaptor<YunkaGatewayClient.YunkaGatewayRequest> requestCaptor =
                ArgumentCaptor.forClass(YunkaGatewayClient.YunkaGatewayRequest.class);
        org.mockito.Mockito.verify(yunkaGatewayClient).proxy(requestCaptor.capture());
        JsonNode data = objectMapper.valueToTree(requestCaptor.getValue().data());
        assertThat(requestCaptor.getValue().path()).isEqualTo("/repay/trial");
        assertThat(data.get("userId").asText()).isEqualTo("mem-repay-info");
        assertThat(data.has("uid")).isFalse();
        assertThat(data.get("loanId").isInt()).isTrue();
        assertThat(data.get("loanId").asInt()).isEqualTo(2026041301);
        assertThat(data.get("repayType").isInt()).isTrue();
        assertThat(data.get("repayType").asInt()).isEqualTo(2);
        assertThat(data.get("periods").asText()).isEqualTo("1,2,3");
        assertThat(output).contains("repayment info yunka request begin");
        assertThat(output).contains("scene=repayment info elapsedMs=");
        assertThat(output).contains("yunka request success");
        assertThat(output).contains("path=/repay/trial");
        assertThat(output).contains("bizOrderNo=2026041301");
    }

    @Test
    void shouldForwardRepaymentSubmitToYunkaRepayApply() throws Exception {
        MemberInfo memberInfo = createMember("mem-repay-submit", "user-repay-submit");
        insertReceivingAccount(memberInfo.getMemberId(), "acc-repay-submit-001", "招商银行", "8648");
        createApplicationMapping(memberInfo, "APP-REPAY-SUBMIT-001", 2026041302);
        when(xiaohuaGatewayService.queryLoanRepayPlan(any(), eq("2026041302"), any()))
                .thenReturn(new LoanRepayPlanResponse(List.of(
                        new LoanRepayPlanItem(1, 1, 1, "2026-05-07", 100000L, 4500L, 104500L),
                        new LoanRepayPlanItem(2, 2, 1, "2026-06-07", 100000L, 3000L, 103000L),
                        new LoanRepayPlanItem(3, 3, 1, "2026-07-07", 100000L, 3000L, 103000L)
                )));
        JsonNode yunkaData = objectMapper.readTree("""
                {
                  "status": "PROCESSING",
                  "swiftNumber": "REP202604130001",
                  "remark": "还款请求已提交，正在处理中"
                }
                """);
        when(yunkaGatewayClient.proxy(any()))
                .thenAnswer(invocation -> {
                    YunkaGatewayClient.YunkaGatewayRequest gatewayRequest = invocation.getArgument(0);
                    if ("/repay/trial".equals(gatewayRequest.path())) {
                        return new YunkaGatewayClient.YunkaGatewayResponse(
                                0,
                                "SUCCESS",
                                objectMapper.readTree("""
                                        {"repayAmount":1018.50}
                                        """)
                        );
                    }
                    return new YunkaGatewayClient.YunkaGatewayResponse(0, "SUCCESS", yunkaData);
                });

        mockMvc.perform(post("/api/repayment/submit")
                        .cookie(authCookie(memberInfo))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "loanId": 2026041302,
                                  "amount": 1018.50,
                                  "bankCardId": "acc-repay-submit-001",
                                  "repaymentType": "scheduled"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.repaymentId").value("REP202604130001"))
                .andExpect(jsonPath("$.data.status").value("processing"))
                .andExpect(jsonPath("$.data.message").value("还款请求已提交，正在处理中"));

        ArgumentCaptor<YunkaGatewayClient.YunkaGatewayRequest> requestCaptor =
                ArgumentCaptor.forClass(YunkaGatewayClient.YunkaGatewayRequest.class);
        org.mockito.Mockito.verify(yunkaGatewayClient, org.mockito.Mockito.times(2)).proxy(requestCaptor.capture());
        YunkaGatewayClient.YunkaGatewayRequest repayApplyRequest = requestCaptor.getAllValues().stream()
                .filter(request -> "/repay/apply".equals(request.path()))
                .findFirst()
                .orElseThrow();
        JsonNode data = objectMapper.valueToTree(repayApplyRequest.data());
        assertThat(repayApplyRequest.path()).isEqualTo("/repay/apply");
        assertThat(data.get("userId").asText()).isEqualTo("mem-repay-submit");
        assertThat(data.has("uid")).isFalse();
        assertThat(data.get("loanId").isInt()).isTrue();
        assertThat(data.get("loanId").asInt()).isEqualTo(2026041302);
        assertThat(data.get("bankCardNo").asText()).isEqualTo("acc-repay-submit-001");
        assertThat(data.get("repayAmount").decimalValue()).isEqualByComparingTo("1018.50");
    }

    @Test
    void shouldForwardRepaymentResultToYunkaRepayQuery() throws Exception {
        MemberInfo memberInfo = createMember("mem-repay-result", "user-repay-result");
        insertReceivingAccount(memberInfo.getMemberId(), "acc-repay-result-001", "招商银行", "8648");
        createApplicationMapping(memberInfo, "APP-REPAY-RESULT-001", 2026041302);
        JsonNode yunkaData = objectMapper.readTree("""
                {
                  "status": "SUCCESS",
                  "amount": 1018.50,
                  "successTime": "2026-04-13T14:32:00+08:00",
                  "remark": "还款成功",
                  "swiftNumber": "RP-2026041302"
                }
                """);
        when(yunkaGatewayClient.proxy(any()))
                .thenReturn(new YunkaGatewayClient.YunkaGatewayResponse(0, "SUCCESS", yunkaData));

        mockMvc.perform(get("/api/repayment/result/RP-2026041302")
                        .cookie(authCookie(memberInfo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.repaymentId").value("RP-2026041302"))
                .andExpect(jsonPath("$.data.status").value("success"))
                .andExpect(jsonPath("$.data.amount").value(1018.5))
                .andExpect(jsonPath("$.data.repaymentTime").value("2026-04-13T14:32:00+08:00"))
                .andExpect(jsonPath("$.data.tips[0]").isNotEmpty());

        ArgumentCaptor<YunkaGatewayClient.YunkaGatewayRequest> requestCaptor =
                ArgumentCaptor.forClass(YunkaGatewayClient.YunkaGatewayRequest.class);
        org.mockito.Mockito.verify(yunkaGatewayClient).proxy(requestCaptor.capture());
        JsonNode data = objectMapper.valueToTree(requestCaptor.getValue().data());
        assertThat(requestCaptor.getValue().path()).isEqualTo("/repay/query");
        assertThat(data.get("userId").asText()).isEqualTo("mem-repay-result");
        assertThat(data.has("uid")).isFalse();
        assertThat(data.get("loanId").isInt()).isTrue();
        assertThat(data.get("loanId").asInt()).isEqualTo(2026041302);
        assertThat(data.get("swiftNumber").asText()).isEqualTo("RP-2026041302");
    }

    @Test
    void shouldReturnRepaymentInfoInEnglishWhenAcceptLanguageProvided() throws Exception {
        MemberInfo memberInfo = createMember("mem-repay-info-en", "user-repay-info-en");
        insertReceivingAccount(memberInfo.getMemberId(), "acc-repay-info-en-001", "招商银行", "8648");
        createApplicationMapping(memberInfo, "APP-REPAY-INFO-EN-001", 2026041309);
        when(xiaohuaGatewayService.queryLoanRepayPlan(any(), eq("2026041309"), any()))
                .thenReturn(new LoanRepayPlanResponse(List.of(
                        new LoanRepayPlanItem(1, 1, 1, "2026-05-07", 100000L, 4500L, 104500L),
                        new LoanRepayPlanItem(2, 2, 1, "2026-06-07", 100000L, 3000L, 103000L),
                        new LoanRepayPlanItem(3, 3, 1, "2026-07-07", 100000L, 3000L, 103000L)
                )));
        JsonNode yunkaData = objectMapper.readTree("""
                {
                  "repayAmount": 1018.50,
                  "repayPrincipal": 1000.00,
                  "repayInterest": 18.50,
                  "repayPenaltyInt": 0,
                  "discount": 26.50
                }
                """);
        when(yunkaGatewayClient.proxy(any()))
                .thenReturn(new YunkaGatewayClient.YunkaGatewayResponse(0, "SUCCESS", yunkaData));

        mockMvc.perform(get("/api/repayment/info/2026041309")
                        .cookie(authCookie(memberInfo))
                        .header("Accept-Language", "en-US,en;q=0.9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.loanId").value(2026041309))
                .andExpect(jsonPath("$.data.repaymentType").value("Current due"))
                .andExpect(jsonPath("$.data.tip").value("Repayment takes effect immediately, and interest for the remaining terms will no longer be charged. Please make sure your bank card has sufficient balance."))
                .andExpect(header().string("Content-Language", "en-US"));
    }

    private void createApplicationMapping(MemberInfo memberInfo, String applicationId, Integer loanId) {
        LoanApplicationMapping mapping = new LoanApplicationMapping();
        mapping.setApplicationId(applicationId);
        mapping.setMemberId(memberInfo.getMemberId());
        mapping.setChannelCode("KJ");
        mapping.setExternalUserId(memberInfo.getExternalUserId());
        mapping.setPlatformLoanId(loanId);
        mapping.setPurpose("rent");
        mapping.setMappingStatus("ACTIVE");
        mapping.setCreatedTs(LocalDateTime.now());
        mapping.setUpdatedTs(LocalDateTime.now());
        loanApplicationMappingRepository.insert(mapping);
    }

    private MemberInfo createMember(String memberId, String externalUserId) {
        MemberInfo memberInfo = new MemberInfo();
        memberInfo.setMemberId(memberId);
        memberInfo.setTechPlatformUserId(externalUserId);
        memberInfo.setExternalUserId(externalUserId);
        memberInfo.setMobileEncrypted(sensitiveDataCipher.encrypt("13800138000"));
        memberInfo.setMobileHash("hash-mobile-" + UUID.randomUUID());
        memberInfo.setIdCardEncrypted(sensitiveDataCipher.encrypt("110101199003071234"));
        memberInfo.setIdCardHash("hash-id-" + UUID.randomUUID());
        memberInfo.setRealNameEncrypted(sensitiveDataCipher.encrypt("测试用户"));
        memberInfo.setMemberStatus(MemberStatusEnum.ACTIVE.name());
        memberInfo.setCreatedTs(LocalDateTime.now());
        memberInfo.setUpdatedTs(LocalDateTime.now());
        memberInfoRepository.insert(memberInfo);
        return memberInfo;
    }

    private Cookie authCookie(MemberInfo memberInfo) {
        return new Cookie(
                "NEXUSFIN_AUTH",
                jwtUtil.generateToken(memberInfo.getMemberId(), memberInfo.getExternalUserId())
        );
    }

    private void insertReceivingAccount(String memberId, String accountId, String bankName, String lastFour) {
        MemberReceivingAccount account = new MemberReceivingAccount();
        account.setMemberId(memberId);
        account.setAccountId(accountId);
        account.setBankName(bankName);
        account.setLastFour(lastFour);
        account.setAccountStatus("ACTIVE");
        account.setIsDefault(1);
        account.setSource("TEST");
        account.setSourceIndex(0);
        account.setCreatedTs(LocalDateTime.now());
        account.setUpdatedTs(LocalDateTime.now());
        memberReceivingAccountRepository.insert(account);
    }
}
