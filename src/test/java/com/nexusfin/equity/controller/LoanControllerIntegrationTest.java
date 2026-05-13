package com.nexusfin.equity.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusfin.equity.entity.LoanApplicationMapping;
import com.nexusfin.equity.entity.MemberReceivingAccount;
import com.nexusfin.equity.entity.MemberInfo;
import com.nexusfin.equity.enums.MemberStatusEnum;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LoanControllerIntegrationTest extends AbstractYunkaXiaohuaIT {

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
    void shouldReturnCalculatorConfigReceivingAccountFromDatabase() throws Exception {
        MemberInfo memberInfo = createMember("mem-loan-config-db", "user-loan-config-db");
        insertReceivingAccount(memberInfo.getMemberId(), "acc-db-002", "测试银行", "4321");

        mockMvc.perform(get("/api/loan/calculator-config")
                        .cookie(authCookie(memberInfo))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.receivingAccount.bankName").value("测试银行"))
                .andExpect(jsonPath("$.data.receivingAccount.lastFour").value("4321"))
                .andExpect(jsonPath("$.data.receivingAccount.accountId").value("acc-db-002"));
    }

    @Test
    void shouldReturnControlledErrorWhenReceivingAccountIsMissing() throws Exception {
        MemberInfo memberInfo = createMember("mem-loan-config-empty", "user-loan-config-empty");

        mockMvc.perform(get("/api/loan/calculator-config")
                        .cookie(authCookie(memberInfo))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(-1))
                .andExpect(jsonPath("$.message")
                        .value("MEMBER_RECEIVING_ACCOUNT_NOT_CONFIGURED:Member receiving account is not configured"));
    }

    @Test
    void shouldReturnApprovedResultWithRepaymentPlan() throws Exception {
        MemberInfo memberInfo = createMember("mem-loan-result", "user-loan-result");
        createApplicationMapping(memberInfo, "APP-LOAN-001", "LN-LOAN-001");
        JsonNode loanQueryData = objectMapper.readTree("""
                {
                  "loanId": "LN-LOAN-001",
                  "status": "7001",
                  "loanAmount": 300000,
                  "remark": "放款成功"
                }
                """);
        when(yunkaGatewayClient.proxy(any()))
                .thenReturn(new YunkaGatewayClient.YunkaGatewayResponse(0, "SUCCESS", loanQueryData));
        when(xiaohuaGatewayService.queryLoanRepayPlan(any(), eq("APP-LOAN-001"), any()))
                .thenReturn(new LoanRepayPlanResponse(List.of(
                        new LoanRepayPlanItem(1, "2026-05-07", 100000L, 4500L, 104500L)
                )));

        mockMvc.perform(get("/api/loan/approval-result/APP-LOAN-001")
                        .cookie(authCookie(memberInfo))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("approved"))
                .andExpect(jsonPath("$.data.repaymentPlan[0].repaymentAmount").value(1045))
                .andExpect(jsonPath("$.data.loanId").value("LN-LOAN-001"));
    }

    @Test
    void shouldReturnRejectedResultWhenLatestStatusIsFailure() throws Exception {
        MemberInfo memberInfo = createMember("mem-loan-rejected", "user-loan-rejected");
        createApplicationMapping(memberInfo, "APP-LOAN-002", "LN-LOAN-002");
        JsonNode loanQueryData = objectMapper.readTree("""
                {
                  "loanId": "LN-LOAN-002",
                  "status": "7003",
                  "loanAmount": 300000,
                  "remark": "审核失败"
                }
                """);
        when(yunkaGatewayClient.proxy(any()))
                .thenReturn(new YunkaGatewayClient.YunkaGatewayResponse(0, "SUCCESS", loanQueryData));

        mockMvc.perform(get("/api/loan/approval-result/APP-LOAN-002")
                        .cookie(authCookie(memberInfo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("rejected"))
                .andExpect(jsonPath("$.data.loanId").isEmpty());
    }

    @Test
    void shouldReturnControlledErrorWhenLoanQueryDataIsEmpty() throws Exception {
        MemberInfo memberInfo = createMember("mem-loan-empty", "user-loan-empty");
        createApplicationMapping(memberInfo, "APP-LOAN-EMPTY", "LN-LOAN-EMPTY");

        when(yunkaGatewayClient.proxy(any()))
                .thenReturn(new YunkaGatewayClient.YunkaGatewayResponse(0, "SUCCESS", objectMapper.createObjectNode()));

        mockMvc.perform(get("/api/loan/approval-status/APP-LOAN-EMPTY")
                        .cookie(authCookie(memberInfo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(-1))
                .andExpect(jsonPath("$.message").value("YUNKA_RESPONSE_EMPTY:Yunka loan query response is empty"));
    }

    @Test
    void shouldReturnControlledErrorWhenLoanQueryStatusIsMissing() throws Exception {
        MemberInfo memberInfo = createMember("mem-loan-missing-status", "user-loan-missing-status");
        createApplicationMapping(memberInfo, "APP-LOAN-MISSING-STATUS", "LN-LOAN-MISSING-STATUS");
        JsonNode loanQueryData = objectMapper.readTree("""
                {
                  "loanId": "LN-LOAN-MISSING-STATUS",
                  "loanAmount": 300000,
                  "remark": "missing status"
                }
                """);

        when(yunkaGatewayClient.proxy(any()))
                .thenReturn(new YunkaGatewayClient.YunkaGatewayResponse(0, "SUCCESS", loanQueryData));

        mockMvc.perform(get("/api/loan/approval-status/APP-LOAN-MISSING-STATUS")
                        .cookie(authCookie(memberInfo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(-1))
                .andExpect(jsonPath("$.message").value("YUNKA_RESPONSE_INVALID:Yunka loan query response is invalid"));
    }

    @Test
    void shouldReturnControlledErrorWhenApprovedLoanQueryLoanIdIsMissing() throws Exception {
        MemberInfo memberInfo = createMember("mem-loan-missing-loanid", "user-loan-missing-loanid");
        createApplicationMapping(memberInfo, "APP-LOAN-MISSING-LOANID", "LN-LOAN-MISSING-LOANID");
        JsonNode loanQueryData = objectMapper.readTree("""
                {
                  "status": "7001",
                  "loanAmount": 300000,
                  "remark": "放款成功"
                }
                """);

        when(yunkaGatewayClient.proxy(any()))
                .thenReturn(new YunkaGatewayClient.YunkaGatewayResponse(0, "SUCCESS", loanQueryData));

        mockMvc.perform(get("/api/loan/approval-result/APP-LOAN-MISSING-LOANID")
                        .cookie(authCookie(memberInfo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(-1))
                .andExpect(jsonPath("$.message").value("YUNKA_RESPONSE_INVALID:Yunka loan query response is invalid"));
    }

    private void createApplicationMapping(MemberInfo memberInfo, String applicationId, String loanId) {
        LoanApplicationMapping mapping = new LoanApplicationMapping();
        mapping.setApplicationId(applicationId);
        mapping.setMemberId(memberInfo.getMemberId());
        mapping.setChannelCode("KJ");
        mapping.setExternalUserId(memberInfo.getExternalUserId());
        mapping.setUpstreamQueryType("loanId");
        mapping.setUpstreamQueryValue(loanId);
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
