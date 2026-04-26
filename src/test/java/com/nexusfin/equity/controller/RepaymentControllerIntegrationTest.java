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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    void shouldReturnRepaymentCardsAndSelectedCard() throws Exception {
        MemberInfo memberInfo = createMember("mem-repay-cards", "user-repay-cards");
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
        JsonNode yunkaData = objectMapper.readTree("""
                {
                  "status": "8004",
                  "amount": 101850,
                  "swiftNumber": "RP-REPAY-003",
                  "discount": 2650
                }
                """);
        when(yunkaGatewayClient.proxy(any()))
                .thenReturn(new YunkaGatewayClient.YunkaGatewayResponse(0, "SUCCESS", yunkaData));
        when(xiaohuaGatewayService.queryUserCards(any(), eq("RP-REPAY-003"), any()))
                .thenReturn(new UserCardListResponse(List.of(
                        new UserCardSummary("card-001", "招商银行", "8648", 1)
                )));

        mockMvc.perform(get("/api/repayment/result/RP-REPAY-003")
                        .cookie(authCookie(memberInfo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("processing"))
                .andExpect(jsonPath("$.data.swiftNumber").value("RP-REPAY-003"))
                .andExpect(jsonPath("$.data.interestSaved").value(26.5));
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
}
