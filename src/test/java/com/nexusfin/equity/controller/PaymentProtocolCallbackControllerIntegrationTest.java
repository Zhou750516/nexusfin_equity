package com.nexusfin.equity.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nexusfin.equity.entity.IdempotencyRecord;
import com.nexusfin.equity.entity.MemberInfo;
import com.nexusfin.equity.entity.MemberPaymentProtocol;
import com.nexusfin.equity.repository.IdempotencyRecordRepository;
import com.nexusfin.equity.repository.MemberInfoRepository;
import com.nexusfin.equity.repository.MemberPaymentProtocolRepository;
import com.nexusfin.equity.util.SensitiveDataCipher;
import com.nexusfin.equity.util.SignatureUtil;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentProtocolCallbackControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberInfoRepository memberInfoRepository;

    @Autowired
    private MemberPaymentProtocolRepository memberPaymentProtocolRepository;

    @Autowired
    private IdempotencyRecordRepository idempotencyRecordRepository;

    @Autowired
    private SensitiveDataCipher sensitiveDataCipher;

    @BeforeEach
    void setUp() {
        idempotencyRecordRepository.delete(null);
        memberPaymentProtocolRepository.delete(null);
        memberInfoRepository.delete(null);
    }

    @Test
    void shouldPersistPaymentProtocolFromCallback() throws Exception {
        String externalUserId = "protocol-user-" + UUID.randomUUID().toString().replace("-", "");
        MemberInfo memberInfo = createMember("mem-" + externalUserId, externalUserId);
        String requestId = "req-protocol-" + UUID.randomUUID().toString().replace("-", "");

        mockMvc.perform(post("/api/callbacks/payment-protocol")
                        .contentType(MediaType.APPLICATION_JSON)
                        .headers(signatureHeaders("nonce-protocol-save"))
                        .content(callbackRequest(requestId, null, externalUserId, "AIP-REAL-001", "ACTIVE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        MemberPaymentProtocol protocol = memberPaymentProtocolRepository.selectOne(Wrappers.<MemberPaymentProtocol>lambdaQuery()
                .eq(MemberPaymentProtocol::getExternalUserId, externalUserId)
                .last("limit 1"));
        assertThat(protocol).isNotNull();
        assertThat(protocol.getMemberId()).isEqualTo(memberInfo.getMemberId());
        assertThat(protocol.getProviderCode()).isEqualTo("ALLINPAY");
        assertThat(protocol.getProtocolNo()).isEqualTo("AIP-REAL-001");
        assertThat(protocol.getProtocolStatus()).isEqualTo("ACTIVE");
        assertThat(protocol.getSignRequestNo()).isEqualTo(requestId);

        IdempotencyRecord record = idempotencyRecordRepository.selectById(requestId);
        assertThat(record).isNotNull();
        assertThat(record.getBizType()).isEqualTo("PAYMENT_PROTOCOL_SYNC");
    }

    @Test
    void shouldReplayDuplicatePaymentProtocolCallbackWithoutCreatingSecondRecord() throws Exception {
        String externalUserId = "protocol-user-" + UUID.randomUUID().toString().replace("-", "");
        createMember("mem-" + externalUserId, externalUserId);
        String requestId = "req-protocol-dup-" + UUID.randomUUID().toString().replace("-", "");
        String body = callbackRequest(requestId, null, externalUserId, "AIP-REAL-002", "ACTIVE");

        mockMvc.perform(post("/api/callbacks/payment-protocol")
                        .contentType(MediaType.APPLICATION_JSON)
                        .headers(signatureHeaders("nonce-protocol-dup-1"))
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(post("/api/callbacks/payment-protocol")
                        .contentType(MediaType.APPLICATION_JSON)
                        .headers(signatureHeaders("nonce-protocol-dup-2"))
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        assertThat(memberPaymentProtocolRepository.selectCount(Wrappers.<MemberPaymentProtocol>lambdaQuery()
                .eq(MemberPaymentProtocol::getExternalUserId, externalUserId))).isEqualTo(1);
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
        memberInfo.setRealNameEncrypted(sensitiveDataCipher.encrypt("协议回写测试用户"));
        memberInfo.setMemberStatus("ACTIVE");
        memberInfo.setCreatedTs(LocalDateTime.now());
        memberInfo.setUpdatedTs(LocalDateTime.now());
        memberInfoRepository.insert(memberInfo);
        return memberInfo;
    }

    private HttpHeaders signatureHeaders(String nonce) {
        String appId = "test-app";
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String signature = SignatureUtil.sign(appId, timestamp, nonce, "test-secret");
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-App-Id", appId);
        headers.add("X-Timestamp", timestamp);
        headers.add("X-Nonce", nonce);
        headers.add("X-Signature", signature);
        return headers;
    }

    private String callbackRequest(
            String requestId,
            String memberId,
            String externalUserId,
            String protocolNo,
            String protocolStatus
    ) {
        String memberIdField = memberId == null ? "null" : "\"" + memberId + "\"";
        return """
                {
                  "requestId": "%s",
                  "memberId": %s,
                  "externalUserId": "%s",
                  "providerCode": "ALLINPAY",
                  "protocolNo": "%s",
                  "protocolStatus": "%s",
                  "signRequestNo": "%s",
                  "channelCode": "KJ",
                  "signedTs": "2026-04-02T14:30:00"
                }
                """.formatted(requestId, memberIdField, externalUserId, protocolNo, protocolStatus, requestId);
    }
}
