package com.nexusfin.equity.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nexusfin.equity.entity.BenefitOrder;
import com.nexusfin.equity.entity.IdempotencyRecord;
import com.nexusfin.equity.entity.PaymentRecord;
import com.nexusfin.equity.repository.BenefitOrderRepository;
import com.nexusfin.equity.repository.IdempotencyRecordRepository;
import com.nexusfin.equity.repository.PaymentRecordRepository;
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
class PaymentCallbackControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BenefitOrderRepository benefitOrderRepository;

    @Autowired
    private PaymentRecordRepository paymentRecordRepository;

    @Autowired
    private IdempotencyRecordRepository idempotencyRecordRepository;

    @BeforeEach
    void setUp() {
        paymentRecordRepository.delete(null);
        idempotencyRecordRepository.delete(null);
        benefitOrderRepository.delete(null);
    }

    @Test
    void shouldPersistFirstDeductSuccessAndUpdateOrder() throws Exception {
        BenefitOrder order = createOrder("ord-first-success", "user-first-success");
        String requestId = "req-first-success-" + UUID.randomUUID().toString().replace("-", "");

        mockMvc.perform(post("/api/callbacks/first-deduction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .headers(signatureHeaders("nonce-first-success"))
                        .content(deductionRequest(requestId, order.getBenefitOrderNo(), "SUCCESS", 680000L, null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.paymentType").value("FIRST_DEDUCT"))
                .andExpect(jsonPath("$.data.paymentStatus").value("SUCCESS"));

        BenefitOrder savedOrder = benefitOrderRepository.selectById(order.getBenefitOrderNo());
        assertThat(savedOrder.getOrderStatus()).isEqualTo("FIRST_DEDUCT_SUCCESS");
        assertThat(savedOrder.getQwFirstDeductStatus()).isEqualTo("SUCCESS");
        assertThat(savedOrder.getSyncStatus()).isEqualTo("SYNC_SUCCESS");

        PaymentRecord paymentRecord = paymentRecordRepository.selectOne(Wrappers.<PaymentRecord>lambdaQuery()
                .eq(PaymentRecord::getRequestId, requestId)
                .last("limit 1"));
        assertThat(paymentRecord).isNotNull();
        assertThat(paymentRecord.getPaymentType()).isEqualTo("FIRST_DEDUCT");
        assertThat(paymentRecord.getPaymentStatus()).isEqualTo("SUCCESS");

        IdempotencyRecord idempotencyRecord = idempotencyRecordRepository.selectById(requestId);
        assertThat(idempotencyRecord).isNotNull();
        assertThat(idempotencyRecord.getBizType()).isEqualTo("FIRST_DEDUCT");
    }

    @Test
    void shouldReplayDuplicateFirstDeductFailureWithoutCreatingSecondPayment() throws Exception {
        BenefitOrder order = createOrder("ord-first-fail", "user-first-fail");
        String requestId = "req-first-fail-" + UUID.randomUUID().toString().replace("-", "");
        String body = deductionRequest(requestId, order.getBenefitOrderNo(), "FAIL", 680000L, "BANK_REJECT");

        mockMvc.perform(post("/api/callbacks/first-deduction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .headers(signatureHeaders("nonce-first-fail-1"))
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paymentStatus").value("FAIL"));

        mockMvc.perform(post("/api/callbacks/first-deduction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .headers(signatureHeaders("nonce-first-fail-2"))
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paymentStatus").value("FAIL"));

        BenefitOrder savedOrder = benefitOrderRepository.selectById(order.getBenefitOrderNo());
        assertThat(savedOrder.getOrderStatus()).isEqualTo("FIRST_DEDUCT_FAIL");
        assertThat(savedOrder.getQwFirstDeductStatus()).isEqualTo("FAIL");

        assertThat(paymentRecordRepository.selectCount(Wrappers.<PaymentRecord>lambdaQuery()
                .eq(PaymentRecord::getRequestId, requestId))).isEqualTo(1);
    }

    @Test
    void shouldPersistFallbackDeductSuccess() throws Exception {
        BenefitOrder order = createOrder("ord-fallback-success", "user-fallback-success");
        order.setOrderStatus("FALLBACK_DEDUCT_PENDING");
        order.setQwFirstDeductStatus("FAIL");
        order.setQwFallbackDeductStatus("PENDING");
        order.setUpdatedTs(LocalDateTime.now());
        benefitOrderRepository.updateById(order);

        String requestId = "req-fallback-success-" + UUID.randomUUID().toString().replace("-", "");
        mockMvc.perform(post("/api/callbacks/fallback-deduction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .headers(signatureHeaders("nonce-fallback-success"))
                        .content(deductionRequest(requestId, order.getBenefitOrderNo(), "SUCCESS", 680000L, null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paymentType").value("FALLBACK_DEDUCT"))
                .andExpect(jsonPath("$.data.paymentStatus").value("SUCCESS"));

        BenefitOrder savedOrder = benefitOrderRepository.selectById(order.getBenefitOrderNo());
        assertThat(savedOrder.getOrderStatus()).isEqualTo("FALLBACK_DEDUCT_SUCCESS");
        assertThat(savedOrder.getQwFallbackDeductStatus()).isEqualTo("SUCCESS");
    }

    private BenefitOrder createOrder(String benefitOrderNo, String externalUserId) {
        BenefitOrder order = new BenefitOrder();
        order.setBenefitOrderNo(benefitOrderNo);
        order.setMemberId("mem-" + externalUserId);
        order.setChannelCode("KJ");
        order.setExternalUserId(externalUserId);
        order.setProductCode("PROD-CALLBACK");
        order.setAgreementNo("agr-" + externalUserId);
        order.setLoanAmount(680000L);
        order.setOrderStatus("FIRST_DEDUCT_PENDING");
        order.setQwFirstDeductStatus("PENDING");
        order.setQwFallbackDeductStatus("NONE");
        order.setQwExerciseStatus("NONE");
        order.setRefundStatus("NONE");
        order.setGrantStatus("PENDING");
        order.setSyncStatus("SYNC_PENDING");
        order.setRequestId("req-order-" + externalUserId);
        order.setCreatedTs(LocalDateTime.now());
        order.setUpdatedTs(LocalDateTime.now());
        benefitOrderRepository.insert(order);
        return order;
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

    private String deductionRequest(
            String requestId,
            String benefitOrderNo,
            String deductStatus,
            long deductAmount,
            String failReason
    ) {
        String failReasonField = failReason == null ? "null" : "\"" + failReason + "\"";
        return """
                {
                  "requestId": "%s",
                  "benefitOrderNo": "%s",
                  "qwTradeNo": "qw-%s",
                  "deductStatus": "%s",
                  "deductAmount": %d,
                  "failReason": %s,
                  "deductTime": "2026-03-23T20:00:00"
                }
                """.formatted(requestId, benefitOrderNo, requestId, deductStatus, deductAmount, failReasonField);
    }
}
