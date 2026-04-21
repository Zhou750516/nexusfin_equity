package com.nexusfin.equity.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.nexusfin.equity.thirdparty.yunka.YunkaGatewayClient;
import com.nexusfin.equity.util.JwtUtil;
import com.nexusfin.equity.util.SensitiveDataCipher;
import jakarta.servlet.http.Cookie;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class Phase9TaskGroupDIntegrationTest {

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

    @MockBean
    private YunkaGatewayClient yunkaGatewayClient;

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
    void shouldForwardRepaymentInfoToYunkaRepayTrial(CapturedOutput output) throws Exception {
        MemberInfo memberInfo = createMember("mem-repay-info", "user-repay-info");
        JsonNode yunkaData = objectMapper.readTree("""
                {
                  "repayAmount": 101850,
                  "repayPrincipal": 100000,
                  "repayInterest": 1850,
                  "repayPenaltyInt": 0,
                  "discount": 2650
                }
                """);
        when(yunkaGatewayClient.proxy(any()))
                .thenReturn(new YunkaGatewayClient.YunkaGatewayResponse(0, "SUCCESS", yunkaData));

        mockMvc.perform(get("/api/repayment/info/LOAN202604130001")
                        .cookie(authCookie(memberInfo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.loanId").value("LOAN202604130001"))
                .andExpect(jsonPath("$.data.repaymentAmount").value(1018.5))
                .andExpect(jsonPath("$.data.repaymentType").value("提前还款"))
                .andExpect(jsonPath("$.data.bankCard.bankName").value("招商银行"))
                .andExpect(jsonPath("$.data.bankCard.lastFour").value("8648"));

        ArgumentCaptor<YunkaGatewayClient.YunkaGatewayRequest> requestCaptor =
                ArgumentCaptor.forClass(YunkaGatewayClient.YunkaGatewayRequest.class);
        org.mockito.Mockito.verify(yunkaGatewayClient).proxy(requestCaptor.capture());
        JsonNode data = objectMapper.valueToTree(requestCaptor.getValue().data());
        assertThat(requestCaptor.getValue().path()).isEqualTo("/repay/trial");
        assertThat(data.get("uid").asText()).isEqualTo("user-repay-info");
        assertThat(data.get("loanId").asText()).isEqualTo("LOAN202604130001");
        assertThat(data.get("repayType").asText()).isEqualTo("EARLY");
        assertThat(output).contains("repayment info yunka request begin");
        assertThat(output).contains("repayment info yunka request success");
        assertThat(output).contains("path=/repay/trial");
        assertThat(output).contains("bizOrderNo=LOAN202604130001");
    }

    @Test
    void shouldForwardRepaymentSubmitToYunkaRepayApply() throws Exception {
        MemberInfo memberInfo = createMember("mem-repay-submit", "user-repay-submit");
        JsonNode yunkaData = objectMapper.readTree("""
                {
                  "status": "PROCESSING",
                  "swiftNumber": "REP202604130001",
                  "remark": "还款请求已提交，正在处理中"
                }
                """);
        when(yunkaGatewayClient.proxy(any()))
                .thenReturn(new YunkaGatewayClient.YunkaGatewayResponse(0, "SUCCESS", yunkaData));

        mockMvc.perform(post("/api/repayment/submit")
                        .cookie(authCookie(memberInfo))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "loanId": "LOAN202604130002",
                                  "amount": 1018.50,
                                  "bankCardId": "acc_001",
                                  "repaymentType": "early"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.repaymentId").value("REP202604130001"))
                .andExpect(jsonPath("$.data.status").value("processing"))
                .andExpect(jsonPath("$.data.message").value("还款请求已提交，正在处理中"));

        ArgumentCaptor<YunkaGatewayClient.YunkaGatewayRequest> requestCaptor =
                ArgumentCaptor.forClass(YunkaGatewayClient.YunkaGatewayRequest.class);
        org.mockito.Mockito.verify(yunkaGatewayClient).proxy(requestCaptor.capture());
        JsonNode data = objectMapper.valueToTree(requestCaptor.getValue().data());
        assertThat(requestCaptor.getValue().path()).isEqualTo("/repay/apply");
        assertThat(data.get("uid").asText()).isEqualTo("user-repay-submit");
        assertThat(data.get("loanId").asText()).isEqualTo("LOAN202604130002");
        assertThat(data.get("bankCardNo").asText()).isEqualTo("acc_001");
        assertThat(data.get("repayAmount").asLong()).isEqualTo(101850L);
    }

    @Test
    void shouldForwardRepaymentResultToYunkaRepayQuery() throws Exception {
        MemberInfo memberInfo = createMember("mem-repay-result", "user-repay-result");
        JsonNode yunkaData = objectMapper.readTree("""
                {
                  "status": "SUCCESS",
                  "amount": 101850,
                  "successTime": "2026-04-13T14:32:00+08:00",
                  "remark": "还款成功"
                }
                """);
        when(yunkaGatewayClient.proxy(any()))
                .thenReturn(new YunkaGatewayClient.YunkaGatewayResponse(0, "SUCCESS", yunkaData));

        mockMvc.perform(get("/api/repayment/result/REP202604130002")
                        .cookie(authCookie(memberInfo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.repaymentId").value("REP202604130002"))
                .andExpect(jsonPath("$.data.status").value("success"))
                .andExpect(jsonPath("$.data.amount").value(1018.5))
                .andExpect(jsonPath("$.data.repaymentTime").value("2026-04-13T14:32:00+08:00"))
                .andExpect(jsonPath("$.data.tips[0]").isNotEmpty());

        ArgumentCaptor<YunkaGatewayClient.YunkaGatewayRequest> requestCaptor =
                ArgumentCaptor.forClass(YunkaGatewayClient.YunkaGatewayRequest.class);
        org.mockito.Mockito.verify(yunkaGatewayClient).proxy(requestCaptor.capture());
        JsonNode data = objectMapper.valueToTree(requestCaptor.getValue().data());
        assertThat(requestCaptor.getValue().path()).isEqualTo("/repay/query");
        assertThat(data.get("uid").asText()).isEqualTo("user-repay-result");
        assertThat(data.get("swiftNumber").asText()).isEqualTo("REP202604130002");
    }

    @Test
    void shouldReturnRepaymentInfoInEnglishWhenAcceptLanguageProvided() throws Exception {
        MemberInfo memberInfo = createMember("mem-repay-info-en", "user-repay-info-en");
        JsonNode yunkaData = objectMapper.readTree("""
                {
                  "repayAmount": 101850,
                  "repayPrincipal": 100000,
                  "repayInterest": 1850,
                  "repayPenaltyInt": 0,
                  "discount": 2650
                }
                """);
        when(yunkaGatewayClient.proxy(any()))
                .thenReturn(new YunkaGatewayClient.YunkaGatewayResponse(0, "SUCCESS", yunkaData));

        mockMvc.perform(get("/api/repayment/info/LOAN202604130009")
                        .cookie(authCookie(memberInfo))
                        .header("Accept-Language", "en-US,en;q=0.9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.loanId").value("LOAN202604130009"))
                .andExpect(jsonPath("$.data.repaymentType").value("Early repayment"))
                .andExpect(jsonPath("$.data.tip").value("Repayment takes effect immediately, and interest for the remaining terms will no longer be charged. Please make sure your bank card has sufficient balance."))
                .andExpect(header().string("Content-Language", "en-US"));
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
