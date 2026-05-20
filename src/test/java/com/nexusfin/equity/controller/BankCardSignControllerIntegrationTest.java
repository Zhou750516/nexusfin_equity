package com.nexusfin.equity.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusfin.equity.entity.MemberChannel;
import com.nexusfin.equity.entity.MemberInfo;
import com.nexusfin.equity.entity.MemberPaymentProtocol;
import com.nexusfin.equity.repository.MemberChannelRepository;
import com.nexusfin.equity.repository.MemberInfoRepository;
import com.nexusfin.equity.repository.MemberPaymentProtocolRepository;
import com.nexusfin.equity.util.JwtUtil;
import com.nexusfin.equity.util.SensitiveDataCipher;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BankCardSignControllerIntegrationTest {

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
    private JwtUtil jwtUtil;

    @Autowired
    private SensitiveDataCipher sensitiveDataCipher;

    @BeforeEach
    void setUp() {
        memberPaymentProtocolRepository.delete(null);
        memberChannelRepository.delete(null);
        memberInfoRepository.delete(null);
    }

    @Test
    void shouldRejectSignStatusWhenAuthCookieMissing() throws Exception {
        mockMvc.perform(get("/api/bank-card/sign-status")
                        .param("accountNo", "6222020202020208"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void shouldReturnMockSignStatusForAuthenticatedMember() throws Exception {
        MemberInfo memberInfo = createMember("mem-sign-status", "tech-user-sign-status");
        createChannel(memberInfo.getMemberId(), memberInfo.getExternalUserId());

        mockMvc.perform(get("/api/bank-card/sign-status")
                        .cookie(authCookie(memberInfo))
                        .param("accountNo", "6222020202020208"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.signed").value(true))
                .andExpect(jsonPath("$.data.status").value("SIGNED"))
                .andExpect(jsonPath("$.data.canApplySign").value(false));
    }

    @Test
    void shouldReturnUnsignedSignStatusForAuthenticatedMember() throws Exception {
        MemberInfo memberInfo = createMember("mem-sign-status-unsigned", "tech-user-sign-status-unsigned");
        createChannel(memberInfo.getMemberId(), memberInfo.getExternalUserId());

        mockMvc.perform(get("/api/bank-card/sign-status")
                        .cookie(authCookie(memberInfo))
                        .param("accountNo", "6222020202020207"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.signed").value(false))
                .andExpect(jsonPath("$.data.status").value("UNSIGNED"))
                .andExpect(jsonPath("$.data.canApplySign").value(true));
    }

    @Test
    void shouldReturnControlledBizErrorWhenMockSignStatusTimesOut() throws Exception {
        MemberInfo memberInfo = createMember("mem-sign-status-timeout", "tech-user-sign-status-timeout");
        createChannel(memberInfo.getMemberId(), memberInfo.getExternalUserId());

        mockMvc.perform(get("/api/bank-card/sign-status")
                        .cookie(authCookie(memberInfo))
                        .param("accountNo", "6222020202021234_FAULT_TIMEOUT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(-1))
                .andExpect(jsonPath("$.message").value("QW_SIGN_UPSTREAM_TIMEOUT:QW sign status temporarily unavailable"));

        assertThat(memberPaymentProtocolRepository.selectCount(null)).isZero();
    }

    @Test
    void shouldApplySignForAuthenticatedMember() throws Exception {
        MemberInfo memberInfo = createMember("mem-sign-apply", "tech-user-sign-apply");
        createChannel(memberInfo.getMemberId(), memberInfo.getExternalUserId());

        mockMvc.perform(post("/api/bank-card/sign-apply")
                        .cookie(authCookie(memberInfo))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "accountNo": "6222020202021234"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.userSignId").value(1234))
                .andExpect(jsonPath("$.data.applyTime").value("2026-04-29 10:00:00"))
                .andExpect(jsonPath("$.data.status").value("SMS_SENT"));
    }

    @Test
    void shouldReturnControlledBizErrorWhenMockApplySignTimesOut() throws Exception {
        MemberInfo memberInfo = createMember("mem-sign-apply-timeout", "tech-user-sign-apply-timeout");
        createChannel(memberInfo.getMemberId(), memberInfo.getExternalUserId());

        mockMvc.perform(post("/api/bank-card/sign-apply")
                        .cookie(authCookie(memberInfo))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "accountNo": "6222020202021234_FAULT_TIMEOUT"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(-1))
                .andExpect(jsonPath("$.message").value("QW_SIGN_UPSTREAM_TIMEOUT:QW sign apply temporarily unavailable"));

        assertThat(memberPaymentProtocolRepository.selectCount(null)).isZero();
    }

    @Test
    void shouldConfirmSignAndPersistLocalProtocol() throws Exception {
        MemberInfo memberInfo = createMember("mem-sign-confirm", "tech-user-sign-confirm");
        createChannel(memberInfo.getMemberId(), memberInfo.getExternalUserId());

        mockMvc.perform(post("/api/bank-card/sign-confirm")
                        .cookie(authCookie(memberInfo))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userSignId": 5678,
                                  "verificationCode": "123456"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.userSignId").value(5678))
                .andExpect(jsonPath("$.data.agreementNo").value(org.hamcrest.Matchers.startsWith("mock-agreement-5678")))
                .andExpect(jsonPath("$.data.signed").value(true))
                .andExpect(jsonPath("$.data.status").value("SIGNED"));

        MemberPaymentProtocol protocol = memberPaymentProtocolRepository.selectOne(Wrappers.<MemberPaymentProtocol>lambdaQuery()
                .eq(MemberPaymentProtocol::getMemberId, memberInfo.getMemberId())
                .eq(MemberPaymentProtocol::getProviderCode, "QW_SIGN")
                .last("limit 1"));
        assertThat(protocol).isNotNull();
        assertThat(protocol.getExternalUserId()).isEqualTo(memberInfo.getExternalUserId());
        assertThat(protocol.getProtocolNo()).startsWith("mock-agreement-5678");
        assertThat(protocol.getProtocolStatus()).isEqualTo("ACTIVE");
        assertThat(protocol.getSignRequestNo()).isEqualTo("5678");
    }

    @Test
    void shouldReturnControlledBizErrorWhenMockConfirmSignTimesOut() throws Exception {
        MemberInfo memberInfo = createMember("mem-sign-confirm-timeout", "tech-user-sign-confirm-timeout");
        createChannel(memberInfo.getMemberId(), memberInfo.getExternalUserId());

        mockMvc.perform(post("/api/bank-card/sign-confirm")
                        .cookie(authCookie(memberInfo))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Trace-Id", "REQ_EX_P0_1_3_FAULT_TIMEOUT")
                        .content("""
                                {
                                  "userSignId": 5678,
                                  "verificationCode": "123456"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(-1))
                .andExpect(jsonPath("$.message").value("QW_SIGN_UPSTREAM_TIMEOUT:QW sign confirm temporarily unavailable"));

        assertThat(memberPaymentProtocolRepository.selectCount(null)).isZero();
        assertThat(memberPaymentProtocolRepository.selectCount(Wrappers.<MemberPaymentProtocol>lambdaQuery()
                .eq(MemberPaymentProtocol::getSignRequestNo, "5678"))).isZero();
    }

    @Test
    void shouldReuseExistingAgreementWhenConfirmSignIsRepeated() throws Exception {
        MemberInfo memberInfo = createMember("mem-sign-confirm-duplicate", "tech-user-sign-confirm-duplicate");
        createChannel(memberInfo.getMemberId(), memberInfo.getExternalUserId());

        MvcResult firstConfirm = mockMvc.perform(post("/api/bank-card/sign-confirm")
                        .cookie(authCookie(memberInfo))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Trace-Id", "TRACE_CASE_EX_C5_CONFIRM_DUP_001")
                        .content("""
                                {
                                  "userSignId": 3579,
                                  "verificationCode": "123456"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.userSignId").value(3579))
                .andReturn();
        JsonNode firstBody = objectMapper.readTree(firstConfirm.getResponse().getContentAsString());
        String firstAgreementNo = firstBody.path("data").path("agreementNo").asText();
        assertThat(firstAgreementNo).startsWith("mock-agreement-3579");

        mockMvc.perform(post("/api/bank-card/sign-confirm")
                        .cookie(authCookie(memberInfo))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Trace-Id", "TRACE_CASE_EX_C5_CONFIRM_DUP_002")
                        .content("""
                                {
                                  "userSignId": 3579,
                                  "verificationCode": "123456"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.userSignId").value(3579))
                .andExpect(jsonPath("$.data.agreementNo").value(firstAgreementNo))
                .andExpect(jsonPath("$.data.signed").value(true))
                .andExpect(jsonPath("$.data.status").value("SIGNED"));

        assertThat(memberPaymentProtocolRepository.selectCount(Wrappers.<MemberPaymentProtocol>lambdaQuery()
                .eq(MemberPaymentProtocol::getMemberId, memberInfo.getMemberId())
                .eq(MemberPaymentProtocol::getProviderCode, "QW_SIGN")
                .eq(MemberPaymentProtocol::getSignRequestNo, "3579")
                .eq(MemberPaymentProtocol::getProtocolStatus, "ACTIVE"))).isEqualTo(1);

        MemberPaymentProtocol protocol = memberPaymentProtocolRepository.selectOne(Wrappers.<MemberPaymentProtocol>lambdaQuery()
                .eq(MemberPaymentProtocol::getMemberId, memberInfo.getMemberId())
                .eq(MemberPaymentProtocol::getProviderCode, "QW_SIGN")
                .eq(MemberPaymentProtocol::getSignRequestNo, "3579")
                .last("limit 1"));
        assertThat(protocol).isNotNull();
        assertThat(protocol.getProtocolNo()).isEqualTo(firstAgreementNo);
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
        memberInfo.setRealNameEncrypted(sensitiveDataCipher.encrypt("签约测试用户"));
        memberInfo.setMemberStatus("ACTIVE");
        memberInfo.setCreatedTs(LocalDateTime.now());
        memberInfo.setUpdatedTs(LocalDateTime.now());
        memberInfoRepository.insert(memberInfo);
        return memberInfo;
    }

    private void createChannel(String memberId, String externalUserId) {
        MemberChannel memberChannel = new MemberChannel();
        memberChannel.setMemberId(memberId);
        memberChannel.setChannelCode("KJ");
        memberChannel.setExternalUserId(externalUserId);
        memberChannel.setBindStatus("BOUND");
        memberChannel.setCreatedTs(LocalDateTime.now());
        memberChannel.setUpdatedTs(LocalDateTime.now());
        memberChannelRepository.insert(memberChannel);
    }

    private jakarta.servlet.http.Cookie authCookie(MemberInfo memberInfo) {
        return new jakarta.servlet.http.Cookie(
                "NEXUSFIN_AUTH",
                jwtUtil.generateToken(memberInfo.getMemberId(), memberInfo.getExternalUserId())
        );
    }
}
