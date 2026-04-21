package com.nexusfin.equity.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
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
                .andExpect(jsonPath("$.data.status").value("SIGNED"));
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
                .andExpect(jsonPath("$.data.requestNo").value("mock-sign-apply-1234"))
                .andExpect(jsonPath("$.data.status").value("SMS_SENT"));
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
                                  "accountNo": "6222020202025678",
                                  "verificationCode": "123456"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.requestNo").value("mock-sign-confirm-5678"))
                .andExpect(jsonPath("$.data.signed").value(true))
                .andExpect(jsonPath("$.data.status").value("SIGNED"));

        MemberPaymentProtocol protocol = memberPaymentProtocolRepository.selectOne(Wrappers.<MemberPaymentProtocol>lambdaQuery()
                .eq(MemberPaymentProtocol::getMemberId, memberInfo.getMemberId())
                .eq(MemberPaymentProtocol::getProviderCode, "QW_SIGN")
                .last("limit 1"));
        assertThat(protocol).isNotNull();
        assertThat(protocol.getExternalUserId()).isEqualTo(memberInfo.getExternalUserId());
        assertThat(protocol.getProtocolNo()).isEqualTo("QW-SIGN-mock-sign-confirm-5678");
        assertThat(protocol.getProtocolStatus()).isEqualTo("ACTIVE");
        assertThat(protocol.getSignRequestNo()).isEqualTo("mock-sign-confirm-5678");
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
