package com.nexusfin.equity.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusfin.equity.entity.LoanApplicationMapping;
import com.nexusfin.equity.entity.MemberInfo;
import com.nexusfin.equity.enums.MemberStatusEnum;
import com.nexusfin.equity.repository.BenefitOrderRepository;
import com.nexusfin.equity.repository.ContractArchiveRepository;
import com.nexusfin.equity.repository.IdempotencyRecordRepository;
import com.nexusfin.equity.repository.LoanApplicationMappingRepository;
import com.nexusfin.equity.repository.MemberChannelRepository;
import com.nexusfin.equity.repository.MemberInfoRepository;
import com.nexusfin.equity.repository.MemberPaymentProtocolRepository;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(OutputCaptureExtension.class)
class Phase9TaskGroupCIntegrationTest extends AbstractYunkaXiaohuaIT {

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
        memberInfoRepository.delete(null);
    }

    @Test
    void shouldForwardLoanCalculateToYunkaTrailPath(CapturedOutput output) throws Exception {
        MemberInfo memberInfo = createMember("mem-loan-calculate", "user-loan-calculate");
        JsonNode yunkaData = objectMapper.readTree("""
                {
                  "receiveAmount": 300000,
                  "repayAmount": 313500,
                  "yearRate": "18.0%",
                  "repayPlan": [
                    {"period": 1, "date": "2026-05-07", "principal": 100000, "interest": 4500, "total": 104500},
                    {"period": 2, "date": "2026-06-07", "principal": 100000, "interest": 4500, "total": 104500},
                    {"period": 3, "date": "2026-07-07", "principal": 100000, "interest": 4500, "total": 104500}
                  ]
                }
                """);
        when(yunkaGatewayClient.proxy(any()))
                .thenReturn(new YunkaGatewayClient.YunkaGatewayResponse(0, "SUCCESS", yunkaData));

        mockMvc.perform(post("/api/loan/calculate")
                        .cookie(authCookie(memberInfo))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount": 3000,
                                  "term": 3
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.totalFee").value(135))
                .andExpect(jsonPath("$.data.annualRate").value("18.0%"))
                .andExpect(jsonPath("$.data.repaymentPlan[0].principal").value(1000))
                .andExpect(jsonPath("$.data.repaymentPlan[0].interest").value(45))
                .andExpect(jsonPath("$.data.repaymentPlan[0].total").value(1045));

        ArgumentCaptor<YunkaGatewayClient.YunkaGatewayRequest> requestCaptor =
                ArgumentCaptor.forClass(YunkaGatewayClient.YunkaGatewayRequest.class);
        org.mockito.Mockito.verify(yunkaGatewayClient).proxy(requestCaptor.capture());
        YunkaGatewayClient.YunkaGatewayRequest request = requestCaptor.getValue();
        JsonNode data = objectMapper.valueToTree(request.data());
        assertThat(request.path()).isEqualTo("/loan/trail");
        assertThat(request.requestId()).startsWith("LC-");
        assertThat(data.get("uid").asText()).isEqualTo("user-loan-calculate");
        assertThat(data.get("applyId").asText()).startsWith("LC-");
        assertThat(data.get("loanAmount").asLong()).isEqualTo(300000L);
        assertThat(data.get("loanPeriod").asInt()).isEqualTo(3);
        assertThat(output).contains("loan calculate yunka request begin");
        assertThat(output).contains("loan calculate yunka request success");
        assertThat(output).contains("path=/loan/trail");
        assertThat(output).contains("bizOrderNo=");
    }

    @Test
    void shouldQueryApprovalStatusFromYunkaLoanQuery() throws Exception {
        MemberInfo memberInfo = createMember("mem-approval-status", "user-approval-status");
        createApplicationMapping(memberInfo, "APP202604130001", "LOAN202604130001");
        JsonNode yunkaData = objectMapper.readTree("""
                {
                  "status": "7002",
                  "loanAmount": 300000,
                  "repayAmount": 313500,
                  "loanDate": "2026-04-13T10:00:00+08:00",
                  "remark": "放款处理中"
                }
                """);
        when(yunkaGatewayClient.proxy(any()))
                .thenReturn(new YunkaGatewayClient.YunkaGatewayResponse(0, "SUCCESS", yunkaData));

        mockMvc.perform(get("/api/loan/approval-status/APP202604130001")
                        .cookie(authCookie(memberInfo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.applicationId").value("APP202604130001"))
                .andExpect(jsonPath("$.data.purpose").value("rent"))
                .andExpect(jsonPath("$.data.status").value("reviewing"))
                .andExpect(jsonPath("$.data.steps[1].status").value("in_progress"))
                .andExpect(jsonPath("$.data.benefitsCard.available").value(true));

        ArgumentCaptor<YunkaGatewayClient.YunkaGatewayRequest> requestCaptor =
                ArgumentCaptor.forClass(YunkaGatewayClient.YunkaGatewayRequest.class);
        org.mockito.Mockito.verify(yunkaGatewayClient).proxy(requestCaptor.capture());
        JsonNode data = objectMapper.valueToTree(requestCaptor.getValue().data());
        assertThat(requestCaptor.getValue().path()).isEqualTo("/loan/query");
        assertThat(data.get("uid").asText()).isEqualTo("user-approval-status");
        assertThat(data.get("loanId").asText()).isEqualTo("LOAN202604130001");
    }

    @Test
    void shouldQueryApprovalResultFromYunkaLoanQuery() throws Exception {
        MemberInfo memberInfo = createMember("mem-approval-result", "user-approval-result");
        createApplicationMapping(memberInfo, "APP202604130002", "LOAN202604130002");
        JsonNode yunkaData = objectMapper.readTree("""
                {
                  "status": "7001",
                  "loanAmount": 300000,
                  "repayAmount": 313500,
                  "loanDate": "2026-04-13T10:00:00+08:00",
                  "remark": "放款成功"
                }
                """);
        when(yunkaGatewayClient.proxy(any()))
                .thenReturn(new YunkaGatewayClient.YunkaGatewayResponse(0, "SUCCESS", yunkaData));
        when(xiaohuaGatewayService.queryLoanRepayPlan(any(), eq("APP202604130002"), any()))
                .thenReturn(new LoanRepayPlanResponse(List.of(
                        new LoanRepayPlanItem(1, "2026-05-07", 100000L, 4500L, 104500L)
                )));

        mockMvc.perform(get("/api/loan/approval-result/APP202604130002")
                        .cookie(authCookie(memberInfo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.applicationId").value("APP202604130002"))
                .andExpect(jsonPath("$.data.purpose").value("rent"))
                .andExpect(jsonPath("$.data.status").value("approved"))
                .andExpect(jsonPath("$.data.approvedAmount").value(3000))
                .andExpect(jsonPath("$.data.benefitsCardActivated").value(true))
                .andExpect(jsonPath("$.data.repaymentPlan[0].repaymentAmount").value(1045))
                .andExpect(jsonPath("$.data.loanId").value("LOAN202604130002"));
    }

    @Test
    void shouldReturnLocalizedApprovalResultTipWhenAcceptLanguageIsEnglish() throws Exception {
        MemberInfo memberInfo = createMember("mem-approval-result-en", "user-approval-result-en");
        createApplicationMapping(memberInfo, "APP202604130003", "LOAN202604130003");
        JsonNode yunkaData = objectMapper.readTree("""
                {
                  "status": "7001",
                  "loanAmount": 300000,
                  "repayAmount": 313500,
                  "loanDate": "2026-04-13T10:00:00+08:00",
                  "remark": "审批通过，预计30分钟内到账"
                }
                """);
        when(yunkaGatewayClient.proxy(any()))
                .thenReturn(new YunkaGatewayClient.YunkaGatewayResponse(0, "SUCCESS", yunkaData));

        mockMvc.perform(get("/api/loan/approval-result/APP202604130003")
                        .cookie(authCookie(memberInfo))
                        .header("Accept-Language", "en-US"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.applicationId").value("APP202604130003"))
                .andExpect(jsonPath("$.data.status").value("approved"))
                .andExpect(jsonPath("$.data.tip").value("Approved. Funds are expected to arrive within 30 minutes."));
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
        return new Cookie(
                "NEXUSFIN_AUTH",
                jwtUtil.generateToken(memberInfo.getMemberId(), memberInfo.getExternalUserId())
        );
    }
}
