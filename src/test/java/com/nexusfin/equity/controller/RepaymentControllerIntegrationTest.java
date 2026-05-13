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
        createApplicationMapping(memberInfo, "APP-REPAY-001", "LN-REPAY-001");
        JsonNode yunkaData = objectMapper.readTree("""
                {"repayAmount":101850}
                """);
        when(yunkaGatewayClient.proxy(any()))
                .thenReturn(new YunkaGatewayClient.YunkaGatewayResponse(0, "SUCCESS", yunkaData));
        when(xiaohuaGatewayService.queryUserCards(any(), eq("LN-REPAY-001"), any()))
                .thenReturn(new UserCardListResponse(List.of(
                        new UserCardSummary("card-001", "招商银行", "8648", 1),
                        new UserCardSummary("card-002", "建设银行", "1234", 0)
                )));

        mockMvc.perform(get("/api/repayment/info/LN-REPAY-001")
                        .cookie(authCookie(memberInfo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.bankCard.accountId").value("card-001"))
                .andExpect(jsonPath("$.data.bankCards[1].bankName").value("建设银行"))
                .andExpect(jsonPath("$.data.smsRequired").value(true));
    }

    @Test
    void shouldReturnControlledErrorWhenLoanIdIsUnknown() throws Exception {
        MemberInfo memberInfo = createMember("mem-repay-unknown-loan", "user-repay-unknown-loan");

        mockMvc.perform(get("/api/repayment/info/LN-FAKE-20260429-001")
                        .cookie(authCookie(memberInfo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("repayment loan reference not found"));
    }

    @Test
    void shouldSupportRepaymentSmsSendAndConfirm() throws Exception {
        MemberInfo memberInfo = createMember("mem-repay-sms", "user-repay-sms");
        when(xiaohuaGatewayService.queryUserCards(any(), eq("LN-REPAY-002"), any()))
                .thenReturn(new UserCardListResponse(List.of(
                        new UserCardSummary("card-001", "招商银行", "8648", 1)
                )));
        when(xiaohuaGatewayService.sendCardSms(any(), eq("LN-REPAY-002"), any()))
                .thenReturn(new CardSmsSendResponse("sms-001", "11001", "发送成功"));
        when(xiaohuaGatewayService.confirmCardSms(any(), eq("LN-REPAY-002"), any()))
                .thenReturn(new CardSmsConfirmResponse("11002", "验证成功"));

        mockMvc.perform(post("/api/repayment/sms-send")
                        .cookie(authCookie(memberInfo))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "loanId": "LN-REPAY-002",
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
                                  "loanId": "LN-REPAY-002",
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
        createApplicationMapping(memberInfo, "APP-REPAY-003", "LN-REPAY-003");
        JsonNode yunkaData = objectMapper.readTree("""
                {
                  "status": "8004",
                  "amount": 1018.50,
                  "swiftNumber": "RP-LN-REPAY-003",
                  "discount": 26.50
                }
                """);
        when(yunkaGatewayClient.proxy(any()))
                .thenReturn(new YunkaGatewayClient.YunkaGatewayResponse(0, "SUCCESS", yunkaData));
        when(xiaohuaGatewayService.queryUserCards(any(), eq("RP-LN-REPAY-003"), any()))
                .thenReturn(new UserCardListResponse(List.of(
                        new UserCardSummary("card-001", "招商银行", "8648", 1)
                )));

        mockMvc.perform(get("/api/repayment/result/RP-LN-REPAY-003")
                        .cookie(authCookie(memberInfo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("processing"))
                .andExpect(jsonPath("$.data.swiftNumber").value("RP-LN-REPAY-003"))
                .andExpect(jsonPath("$.data.interestSaved").value(26.5));
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
        createApplicationMapping(memberInfo, "APP-REPAY-TIMEOUT-001", "LN-REPAY-TIMEOUT-001");
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
                                  "loanId": "LN-REPAY-TIMEOUT-001",
                                  "amount": 1018.50,
                                  "bankCardId": "acc-mem-repay-timeout",
                                  "repaymentType": "early"
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
        createApplicationMapping(memberInfo, "APP-REPAY-OVERPAY-001", "LN-REPAY-OVERPAY-001");
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
                                  "loanId": "LN-REPAY-OVERPAY-001",
                                  "amount": 2000.00,
                                  "bankCardId": "acc-mem-repay-overpay",
                                  "repaymentType": "early"
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
        createApplicationMapping(memberInfo, "APP-REPAY-DUP-001", "LN-REPAY-DUP-001");
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
                                    {"swiftNumber":"RP-LN-REPAY-DUP-001","status":"8004","remark":"processing"}
                                    """)
                    );
                });

        String payload = """
                {
                  "loanId": "LN-REPAY-DUP-001",
                  "amount": 1018.50,
                  "bankCardId": "acc-mem-repay-duplicate",
                  "repaymentType": "early"
                }
                """;

        mockMvc.perform(post("/api/repayment/submit")
                        .cookie(authCookie(memberInfo))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.repaymentId").value("RP-LN-REPAY-DUP-001"));

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

    private void createApplicationMapping(MemberInfo memberInfo, String applicationId, String loanId) {
        LoanApplicationMapping mapping = new LoanApplicationMapping();
        mapping.setApplicationId(applicationId);
        mapping.setMemberId(memberInfo.getMemberId());
        mapping.setChannelCode("KJ");
        mapping.setExternalUserId(memberInfo.getExternalUserId());
        mapping.setUpstreamQueryType("loanId");
        mapping.setUpstreamQueryValue(loanId);
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
        account.setCreatedTs(LocalDateTime.now());
        account.setUpdatedTs(LocalDateTime.now());
        memberReceivingAccountRepository.insert(account);
    }
}
