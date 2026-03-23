package com.nexusfin.equity.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nexusfin.equity.entity.IdempotencyRecord;
import com.nexusfin.equity.entity.MemberChannel;
import com.nexusfin.equity.entity.MemberInfo;
import com.nexusfin.equity.repository.IdempotencyRecordRepository;
import com.nexusfin.equity.repository.MemberChannelRepository;
import com.nexusfin.equity.repository.MemberInfoRepository;
import com.nexusfin.equity.util.SignatureUtil;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserRegistrationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberInfoRepository memberInfoRepository;

    @Autowired
    private MemberChannelRepository memberChannelRepository;

    @Autowired
    private IdempotencyRecordRepository idempotencyRecordRepository;

    @BeforeEach
    void setUp() {
        memberChannelRepository.delete(null);
        memberInfoRepository.delete(null);
        idempotencyRecordRepository.delete(null);
    }

    @Test
    void shouldRegisterUserSuccessfully() throws Exception {
        String uniqueId = UUID.randomUUID().toString().replace("-", "");
        String requestId = "req-register-" + uniqueId;
        String externalUserId = "user-" + uniqueId;
        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .headers(signatureHeaders("nonce-001"))
                        .content(registerRequest(requestId, externalUserId, "13800000000", "310101199001011234")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data.memberId").isNotEmpty())
                .andExpect(jsonPath("$.data.registerStatus").value("SUCCESS"));

        MemberChannel memberChannel = memberChannelRepository.selectOne(Wrappers.<MemberChannel>lambdaQuery()
                .eq(MemberChannel::getChannelCode, "KJ")
                .eq(MemberChannel::getExternalUserId, externalUserId)
                .last("limit 1"));
        assertThat(memberChannel).isNotNull();

        MemberInfo memberInfo = memberInfoRepository.selectById(memberChannel.getMemberId());
        assertThat(memberInfo).isNotNull();
        assertThat(memberInfo.getExternalUserId()).isEqualTo(externalUserId);
        assertThat(memberInfo.getMobileEncrypted()).isNotEqualTo("13800000000");
        assertThat(memberInfo.getIdCardEncrypted()).isNotEqualTo("310101199001011234");

        IdempotencyRecord record = idempotencyRecordRepository.selectById(requestId);
        assertThat(record).isNotNull();
        assertThat(record.getBizKey()).isEqualTo(memberInfo.getMemberId());
    }

    @Test
    void shouldReturnDuplicateForReplayAndKeepSingleDatabaseRecord() throws Exception {
        String uniqueId = UUID.randomUUID().toString().replace("-", "");
        String requestId = "req-register-" + uniqueId;
        String externalUserId = "user-" + uniqueId;
        String requestBody = registerRequest(requestId, externalUserId, "13900000000", "310101199001011235");

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .headers(signatureHeaders("nonce-101"))
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.registerStatus").value("SUCCESS"));

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .headers(signatureHeaders("nonce-102"))
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.registerStatus").value("DUPLICATE"));

        assertThat(memberInfoRepository.selectCount(null)).isEqualTo(1);
        assertThat(memberChannelRepository.selectCount(null)).isEqualTo(1);
        assertThat(idempotencyRecordRepository.selectCount(null)).isEqualTo(1);
    }

    private org.springframework.http.HttpHeaders signatureHeaders(String nonce) {
        String appId = "test-app";
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String signature = SignatureUtil.sign(appId, timestamp, nonce, "test-secret");
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.add("X-App-Id", appId);
        headers.add("X-Timestamp", timestamp);
        headers.add("X-Nonce", nonce);
        headers.add("X-Signature", signature);
        return headers;
    }

    private String registerRequest(String requestId, String externalUserId, String mobile, String idCard) {
        return """
                {
                  "requestId": "%s",
                  "channelCode": "KJ",
                  "userInfo": {
                    "externalUserId": "%s",
                    "mobileEncrypted": "%s",
                    "idCardEncrypted": "%s",
                    "realNameEncrypted": "张三"
                  }
                }
                """.formatted(requestId, externalUserId, mobile, idCard);
    }
}
