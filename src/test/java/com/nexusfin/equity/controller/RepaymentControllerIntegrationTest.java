package com.nexusfin.equity.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusfin.equity.entity.LoanApplicationMapping;
import com.nexusfin.equity.entity.MemberInfo;
import com.nexusfin.equity.entity.MemberReceivingAccount;
import com.nexusfin.equity.enums.MemberStatusEnum;
import com.nexusfin.equity.exception.UpstreamTimeoutException;
import com.nexusfin.equity.repository.BenefitOrderRepository;
import com.nexusfin.equity.repository.ContractArchiveRepository;
import com.nexusfin.equity.repository.IdempotencyRecordRepository;
import com.nexusfin.equity.repository.LoanApplicationMappingRepository;
import com.nexusfin.equity.repository.MemberChannelRepository;
import com.nexusfin.equity.repository.MemberInfoRepository;
import com.nexusfin.equity.repository.MemberPaymentProtocolRepository;
import com.nexusfin.equity.repository.MemberReceivingAccountRepository;
import com.nexusfin.equity.repository.SignTaskRepository;
import com.nexusfin.equity.support.AbstractYunkaXiaohuaIT;
import com.nexusfin.equity.thirdparty.yunka.CardSmsConfirmResponse;
import com.nexusfin.equity.thirdparty.yunka.CardSmsSendResponse;
import com.nexusfin.equity.thirdparty.yunka.LoanRepayPlanItem;
import com.nexusfin.equity.thirdparty.yunka.LoanRepayPlanResponse;
import com.nexusfin.equity.thirdparty.yunka.UserCardListResponse;
import com.nexusfin.equity.thirdparty.yunka.UserCardSummary;
import com.nexusfin.equity.thirdparty.yunka.YunkaGatewayClient;
import com.nexusfin.equity.util.JwtUtil;
import com.nexusfin.equity.util.SensitiveDataCipher;
import jakarta.servlet.http.Cookie;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RepaymentControllerIntegrationTest extends AbstractYunkaXiaohuaIT {

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
    private ContractArchiveRepository contractArchiveRepository;

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
    void shouldReturnRepaymentCardsAndSelectedCard() throws Exception {
        MemberInfo memberInfo = createMember("mem-repay-cards", "user-repay-cards");
        createApplicationMapping(memberInfo, "APP-REPAY-001", 20260501);
        when(xiaohuaGatewayService.queryLoanRepayPlan(any(), eq("20260501"), any()))
                .thenReturn(new LoanRepayPlanResponse(List.of(
                        new LoanRepayPlanItem(3, 3, 1, "2026-07-07", 100000L, 3000L, 103000L),
                        new LoanRepayPlanItem(1, 1, 2, "2026-05-07", 100000L, 4500L, 104500L),
                        new LoanRepayPlanItem(2, 2, 1, "2026-06-07", 100000L, 3000L, 103000L),
                        new LoanRepayPlanItem(2, 2, 2, "2026-06-07", 100000L, 3000L, 103000L)
                )));
        JsonNode yunkaData = objectMapper.readTree("""
                {
                  "status": 5004,
                  "remark": "试算成功，请确认还款",
                  "repayAmount": 1018.50,
                  "repayPrincipal": 1000.00,
                  "repayInterest": 18.50,
                  "repayPenaltyInt": 0,
                  "repayBreakFee": 0,
                  "repayOtherCharge": 0,
                  "repaySvcFee": 0,
                  "repayGuaranteeFee": 0,
                  "discount": 26.50,
                  "originalRepay": 1045.00
                }
                """);
        when(yunkaGatewayClient.proxy(any()))
                .thenReturn(new YunkaGatewayClient.YunkaGatewayResponse(0, "SUCCESS", yunkaData));
        when(xiaohuaGatewayService.queryUserCards(any(), eq("20260501"), any()))
                .thenReturn(new UserCardListResponse(List.of(
                        new UserCardSummary("card-001", "招商银行", "8648", 1),
                        new UserCardSummary("card-002", "建设银行", "1234", 0)
                )));

        mockMvc.perform(get("/api/repayment/info/20260501")
                        .cookie(authCookie(memberInfo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.repaymentAmount").value(1018.5))
                .andExpect(jsonPath("$.data.remark").value("试算成功，请确认还款"))
                .andExpect(jsonPath("$.data.tip").value("试算成功，请确认还款"))
                .andExpect(jsonPath("$.data.fees.repayPrincipal").value(1000.0))
                .andExpect(jsonPath("$.data.fees.repayInterest").value(18.5))
                .andExpect(jsonPath("$.data.fees.discount").value(26.5))
                .andExpect(jsonPath("$.data.fees.originalRepay").value(1045.0))
                .andExpect(jsonPath("$.data.bankCard.accountId").value("card-001"))
                .andExpect(jsonPath("$.data.bankCards[1].bankName").value("建设银行"))
                .andExpect(jsonPath("$.data.smsRequired").value(false));

        ArgumentCaptor<YunkaGatewayClient.YunkaGatewayRequest> requestCaptor =
                ArgumentCaptor.forClass(YunkaGatewayClient.YunkaGatewayRequest.class);
        verify(yunkaGatewayClient).proxy(requestCaptor.capture());
        JsonNode payload = objectMapper.valueToTree(requestCaptor.getValue().data());
        org.assertj.core.api.Assertions.assertThat(requestCaptor.getValue().path()).isEqualTo("/repay/trial");
        org.assertj.core.api.Assertions.assertThat(payload.path("loanId").isInt()).isTrue();
        org.assertj.core.api.Assertions.assertThat(payload.path("loanId").asInt()).isEqualTo(20260501);
        org.assertj.core.api.Assertions.assertThat(payload.path("repayType").isInt()).isTrue();
        org.assertj.core.api.Assertions.assertThat(payload.path("repayType").asInt()).isEqualTo(2);
        org.assertj.core.api.Assertions.assertThat(payload.path("periods").asText()).isEqualTo("2");
    }

    @Test
    void shouldReturnControlledErrorWhenLoanIdIsUnknown() throws Exception {
        MemberInfo memberInfo = createMember("mem-repay-unknown-loan", "user-repay-unknown-loan");

        mockMvc.perform(get("/api/repayment/info/99999901")
                        .cookie(authCookie(memberInfo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("repayment loan reference not found"));
    }

    @Test
    void shouldSupportRepaymentSmsSendAndConfirm() throws Exception {
        MemberInfo memberInfo = createMember("mem-repay-sms", "user-repay-sms");
        when(xiaohuaGatewayService.queryUserCards(any(), eq("20260502"), any()))
                .thenReturn(new UserCardListResponse(List.of(
                        new UserCardSummary("card-001", "招商银行", "8648", 1)
                )));
        when(xiaohuaGatewayService.sendCardSms(any(), eq("20260502"), any()))
                .thenReturn(new CardSmsSendResponse("sms-001", "11001", "发送成功"));
        when(xiaohuaGatewayService.confirmCardSms(any(), eq("20260502"), any()))
                .thenReturn(new CardSmsConfirmResponse("11002", "验证成功"));

        mockMvc.perform(post("/api/repayment/sms-send")
                        .cookie(authCookie(memberInfo))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "loanId": 20260502,
                                  "bankCardId": "card-001"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.smsSeq").value("sms-001"))
                .andExpect(jsonPath("$.data.status").value("sent"));

        mockMvc.perform(post("/api/repayment/sms-confirm")
                        .cookie(authCookie(memberInfo))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "loanId": 20260502,
                                  "captcha": "123456"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("confirmed"));
    }

    @Test
    void shouldExposeProcessingRepaymentResultWithSwiftNumber() throws Exception {
        MemberInfo memberInfo = createMember("mem-repay-result", "user-repay-result");
        createApplicationMapping(memberInfo, "APP-REPAY-003", 20260503);
        JsonNode yunkaData = objectMapper.readTree("""
                {
                  "status": "8004",
                  "amount": 1018.50,
                  "swiftNumber": "RP-20260503",
                  "discount": 26.50
                }
                """);
        when(yunkaGatewayClient.proxy(any()))
                .thenReturn(new YunkaGatewayClient.YunkaGatewayResponse(0, "SUCCESS", yunkaData));
        when(xiaohuaGatewayService.queryUserCards(any(), eq("RP-20260503"), any()))
                .thenReturn(new UserCardListResponse(List.of(
                        new UserCardSummary("card-001", "招商银行", "8648", 1)
                )));

        mockMvc.perform(get("/api/repayment/result/RP-20260503")
                        .cookie(authCookie(memberInfo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("processing"))
                .andExpect(jsonPath("$.data.swiftNumber").value("RP-20260503"))
                .andExpect(jsonPath("$.data.interestSaved").value(26.5));
    }

    @Test
    void shouldQueryRepaymentResultWithSwiftNumberReturnedBySubmit() throws Exception {
        MemberInfo memberInfo = createMember("mem-repay-swift-result", "user-repay-swift-result");
        insertReceivingAccount(memberInfo.getMemberId(), "acc-mem-repay-swift-result", "招商银行", "8648");
        createApplicationMapping(memberInfo, "APP-REPAY-SWIFT-RESULT-001", 20260509);
        when(xiaohuaGatewayService.queryLoanRepayPlan(any(), eq("20260509"), any()))
                .thenReturn(new LoanRepayPlanResponse(List.of(
                        new LoanRepayPlanItem(1, 1, 2, "2026-05-07", 100000L, 4500L, 104500L),
                        new LoanRepayPlanItem(2, 2, 1, "2026-06-07", 100000L, 3000L, 103000L)
                )));
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
                    if ("/repay/apply".equals(gatewayRequest.path())) {
                        return new YunkaGatewayClient.YunkaGatewayResponse(
                                0,
                                "SUCCESS",
                                objectMapper.readTree("""
                                        {
                                          "status": "5001",
                                          "swiftNumber": "xhqbapi20260529163815470019",
                                          "remark": "还款已受理"
                                        }
                                        """)
                        );
                    }
                    return new YunkaGatewayClient.YunkaGatewayResponse(
                            0,
                            "SUCCESS",
                            objectMapper.readTree("""
                                    {
                                      "status": "8004",
                                      "amount": 1018.50,
                                      "swiftNumber": "xhqbapi20260529163815470019",
                                      "remark": "还款处理中"
                                    }
                                    """)
                    );
                });

        mockMvc.perform(post("/api/repayment/submit")
                        .cookie(authCookie(memberInfo))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "loanId": 20260509,
                                  "amount": 1018.50,
                                  "bankCardId": "acc-mem-repay-swift-result",
                                  "repaymentType": "scheduled"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.repaymentId").value("xhqbapi20260529163815470019"))
                .andExpect(jsonPath("$.data.status").value("processing"));

        mockMvc.perform(get("/api/repayment/result/xhqbapi20260529163815470019")
                        .cookie(authCookie(memberInfo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.repaymentId").value("xhqbapi20260529163815470019"))
                .andExpect(jsonPath("$.data.swiftNumber").value("xhqbapi20260529163815470019"))
                .andExpect(jsonPath("$.data.status").value("processing"))
                .andExpect(jsonPath("$.data.amount").value(1018.5));

        ArgumentCaptor<YunkaGatewayClient.YunkaGatewayRequest> requestCaptor =
                ArgumentCaptor.forClass(YunkaGatewayClient.YunkaGatewayRequest.class);
        verify(yunkaGatewayClient, times(3)).proxy(requestCaptor.capture());
        YunkaGatewayClient.YunkaGatewayRequest queryRequest = requestCaptor.getAllValues().stream()
                .filter(request -> "/repay/query".equals(request.path()))
                .findFirst()
                .orElseThrow();
        JsonNode queryData = objectMapper.valueToTree(queryRequest.data());
        org.assertj.core.api.Assertions.assertThat(queryData.path("userId").asText()).isEqualTo("mem-repay-swift-result");
        org.assertj.core.api.Assertions.assertThat(queryData.path("loanId").asInt()).isEqualTo(20260509);
        org.assertj.core.api.Assertions.assertThat(queryData.path("swiftNumber").asText())
                .isEqualTo("xhqbapi20260529163815470019");
    }

    @Test
    void shouldReturnControlledErrorWhenRepaymentIdIsUnknown() throws Exception {
        MemberInfo memberInfo = createMember("mem-repay-unknown-result", "user-repay-unknown-result");

        mockMvc.perform(get("/api/repayment/result/RP-FAKE-20260429-001")
                        .cookie(authCookie(memberInfo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("repayment reference not found"));
    }

    @Test
    void shouldReturnControlledErrorWhenRepaymentSubmitTimesOut() throws Exception {
        MemberInfo memberInfo = createMember("mem-repay-timeout", "user-repay-timeout");
        insertReceivingAccount(memberInfo.getMemberId(), "acc-mem-repay-timeout", "招商银行", "8648");
        createApplicationMapping(memberInfo, "APP-REPAY-TIMEOUT-001", 20260504);
        when(xiaohuaGatewayService.queryLoanRepayPlan(any(), eq("20260504"), any()))
                .thenReturn(new LoanRepayPlanResponse(List.of(
                        new LoanRepayPlanItem(1, 1, 1, "2026-05-07", 100000L, 4500L, 104500L),
                        new LoanRepayPlanItem(2, 2, 1, "2026-06-07", 100000L, 3000L, 103000L),
                        new LoanRepayPlanItem(3, 3, 1, "2026-07-07", 100000L, 3000L, 103000L)
                )));
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
                    throw new UpstreamTimeoutException("Yunka gateway timeout");
                });

        mockMvc.perform(post("/api/repayment/submit")
                        .cookie(authCookie(memberInfo))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "loanId": 20260504,
                                  "amount": 1018.50,
                                  "bankCardId": "acc-mem-repay-timeout",
                                  "repaymentType": "scheduled"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(-1))
                .andExpect(jsonPath("$.message").value("YUNKA_UPSTREAM_TIMEOUT:Repayment submit temporarily unavailable"));
    }

    @Test
    void shouldReturnControlledErrorWhenRepaymentAmountExceedsCurrentRepayableAmount() throws Exception {
        MemberInfo memberInfo = createMember("mem-repay-overpay", "user-repay-overpay");
        insertReceivingAccount(memberInfo.getMemberId(), "acc-mem-repay-overpay", "招商银行", "8648");
        createApplicationMapping(memberInfo, "APP-REPAY-OVERPAY-001", 20260505);
        when(xiaohuaGatewayService.queryLoanRepayPlan(any(), eq("20260505"), any()))
                .thenReturn(new LoanRepayPlanResponse(List.of(
                        new LoanRepayPlanItem(1, 1, 1, "2026-05-07", 100000L, 4500L, 104500L),
                        new LoanRepayPlanItem(2, 2, 1, "2026-06-07", 100000L, 3000L, 103000L),
                        new LoanRepayPlanItem(3, 3, 1, "2026-07-07", 100000L, 3000L, 103000L)
                )));
        when(yunkaGatewayClient.proxy(any()))
                .thenReturn(new YunkaGatewayClient.YunkaGatewayResponse(
                        0,
                        "SUCCESS",
                        objectMapper.readTree("""
                                {"repayAmount":1018.50}
                                """)
                ));

        mockMvc.perform(post("/api/repayment/submit")
                        .cookie(authCookie(memberInfo))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "loanId": 20260505,
                                  "amount": 2000.00,
                                  "bankCardId": "acc-mem-repay-overpay",
                                  "repaymentType": "scheduled"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(-1))
                .andExpect(jsonPath("$.message").value("REPAYMENT_AMOUNT_EXCEEDED:Repayment amount exceeds current repayable amount"));

        verify(yunkaGatewayClient, times(1)).proxy(any());
    }

    @Test
    void shouldReturnControlledErrorForDuplicateRepaymentSubmitWithoutSecondRepayApplyCall() throws Exception {
        MemberInfo memberInfo = createMember("mem-repay-duplicate", "user-repay-duplicate");
        insertReceivingAccount(memberInfo.getMemberId(), "acc-mem-repay-duplicate", "招商银行", "8648");
        createApplicationMapping(memberInfo, "APP-REPAY-DUP-001", 20260506);
        when(xiaohuaGatewayService.queryLoanRepayPlan(any(), eq("20260506"), any()))
                .thenReturn(new LoanRepayPlanResponse(List.of(
                        new LoanRepayPlanItem(1, 1, 1, "2026-05-07", 100000L, 4500L, 104500L),
                        new LoanRepayPlanItem(2, 2, 1, "2026-06-07", 100000L, 3000L, 103000L),
                        new LoanRepayPlanItem(3, 3, 1, "2026-07-07", 100000L, 3000L, 103000L)
                )));
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
                    return new YunkaGatewayClient.YunkaGatewayResponse(
                            0,
                            "SUCCESS",
                            objectMapper.readTree("""
                                    {"swiftNumber":"RP-20260506","status":"8004","remark":"processing"}
                                    """)
                    );
                });

        String payload = """
                {
                  "loanId": 20260506,
                  "amount": 1018.50,
                  "bankCardId": "acc-mem-repay-duplicate",
                  "repaymentType": "scheduled"
                }
                """;

        mockMvc.perform(post("/api/repayment/submit")
                        .cookie(authCookie(memberInfo))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.repaymentId").value("RP-20260506"));

        mockMvc.perform(post("/api/repayment/submit")
                        .cookie(authCookie(memberInfo))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(-1))
                .andExpect(jsonPath("$.message").value("REPAYMENT_SUBMIT_DUPLICATED:Repayment request is duplicated"));

        ArgumentCaptor<YunkaGatewayClient.YunkaGatewayRequest> captor =
                ArgumentCaptor.forClass(YunkaGatewayClient.YunkaGatewayRequest.class);
        verify(yunkaGatewayClient, times(3)).proxy(captor.capture());
        long repayApplyCalls = captor.getAllValues().stream()
                .filter(request -> "/repay/apply".equals(request.path()))
                .count();
        org.assertj.core.api.Assertions.assertThat(repayApplyCalls).isEqualTo(1);
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
        return new Cookie("NEXUSFIN_AUTH",
                jwtUtil.generateToken(memberInfo.getMemberId(), memberInfo.getExternalUserId()));
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
