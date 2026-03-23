package com.nexusfin.equity.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nexusfin.equity.entity.BenefitOrder;
import com.nexusfin.equity.entity.IdempotencyRecord;
import com.nexusfin.equity.entity.NotificationReceiveLog;
import com.nexusfin.equity.entity.PaymentRecord;
import com.nexusfin.equity.repository.BenefitOrderRepository;
import com.nexusfin.equity.repository.IdempotencyRecordRepository;
import com.nexusfin.equity.repository.NotificationReceiveLogRepository;
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
class NotificationCallbackControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BenefitOrderRepository benefitOrderRepository;

    @Autowired
    private PaymentRecordRepository paymentRecordRepository;

    @Autowired
    private NotificationReceiveLogRepository notificationReceiveLogRepository;

    @Autowired
    private IdempotencyRecordRepository idempotencyRecordRepository;

    @BeforeEach
    void setUp() {
        notificationReceiveLogRepository.delete(null);
        paymentRecordRepository.delete(null);
        idempotencyRecordRepository.delete(null);
        benefitOrderRepository.delete(null);
    }

    @Test
    void shouldTriggerSingleFallbackAfterGrantSuccess() throws Exception {
        BenefitOrder order = createOrder("ord-grant-fallback", "user-grant-fallback");
        order.setOrderStatus("FIRST_DEDUCT_FAIL");
        order.setQwFirstDeductStatus("FAIL");
        order.setUpdatedTs(LocalDateTime.now());
        benefitOrderRepository.updateById(order);

        String requestId = "req-grant-success-" + UUID.randomUUID().toString().replace("-", "");
        String body = """
                {
                  "requestId": "%s",
                  "benefitOrderNo": "%s",
                  "grantStatus": "SUCCESS",
                  "actualAmount": 680000,
                  "loanOrderNo": "loan-%s",
                  "failReason": null,
                  "grantTime": "2026-03-23T20:10:00",
                  "timestamp": 1711195800
                }
                """.formatted(requestId, order.getBenefitOrderNo(), requestId);

        mockMvc.perform(post("/api/callbacks/grant/forward")
                        .contentType(MediaType.APPLICATION_JSON)
                        .headers(signatureHeaders("nonce-grant-1"))
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(post("/api/callbacks/grant/forward")
                        .contentType(MediaType.APPLICATION_JSON)
                        .headers(signatureHeaders("nonce-grant-2"))
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        BenefitOrder savedOrder = benefitOrderRepository.selectById(order.getBenefitOrderNo());
        assertThat(savedOrder.getGrantStatus()).isEqualTo("SUCCESS");
        assertThat(savedOrder.getLoanOrderNo()).startsWith("loan-");
        assertThat(savedOrder.getOrderStatus()).isEqualTo("FALLBACK_DEDUCT_PENDING");
        assertThat(savedOrder.getQwFallbackDeductStatus()).isEqualTo("PENDING");

        assertThat(paymentRecordRepository.selectCount(Wrappers.<PaymentRecord>lambdaQuery()
                .eq(PaymentRecord::getBenefitOrderNo, order.getBenefitOrderNo())
                .eq(PaymentRecord::getPaymentType, "FALLBACK_DEDUCT"))).isEqualTo(1);
        assertThat(notificationReceiveLogRepository.selectCount(Wrappers.<NotificationReceiveLog>lambdaQuery()
                .eq(NotificationReceiveLog::getRequestId, requestId))).isEqualTo(1);
    }

    @Test
    void shouldPersistRepaymentNotification() throws Exception {
        BenefitOrder order = createOrder("ord-repayment", "user-repayment");
        String requestId = "req-repayment-" + UUID.randomUUID().toString().replace("-", "");

        mockMvc.perform(post("/api/callbacks/repayment/forward")
                        .contentType(MediaType.APPLICATION_JSON)
                        .headers(signatureHeaders("nonce-repayment"))
                        .content("""
                                {
                                  "requestId": "%s",
                                  "benefitOrderNo": "%s",
                                  "loanOrderNo": "loan-repayment",
                                  "termNo": 1,
                                  "repaymentStatus": "PAID",
                                  "paidAmount": 680000,
                                  "paidTime": "2026-03-23T20:12:00",
                                  "overdueDays": 0,
                                  "timestamp": 1711195920
                                }
                                """.formatted(requestId, order.getBenefitOrderNo())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        NotificationReceiveLog log = notificationReceiveLogRepository.selectOne(Wrappers.<NotificationReceiveLog>lambdaQuery()
                .eq(NotificationReceiveLog::getRequestId, requestId)
                .last("limit 1"));
        assertThat(log).isNotNull();
        assertThat(log.getNotifyType()).isEqualTo("REPAYMENT_STATUS");

        IdempotencyRecord idempotencyRecord = idempotencyRecordRepository.selectById(requestId);
        assertThat(idempotencyRecord).isNotNull();
        assertThat(idempotencyRecord.getBizType()).isEqualTo("REPAYMENT");
    }

    @Test
    void shouldUpdateOrderForExerciseAndRefundCallbacks() throws Exception {
        BenefitOrder exerciseOrder = createOrder("ord-exercise", "user-exercise");
        BenefitOrder refundOrder = createOrder("ord-refund", "user-refund");

        mockMvc.perform(post("/api/callbacks/exercise-equity")
                        .contentType(MediaType.APPLICATION_JSON)
                        .headers(signatureHeaders("nonce-exercise"))
                        .content("""
                                {
                                  "requestId": "req-exercise-%s",
                                  "benefitOrderNo": "%s",
                                  "exerciseStatus": "SUCCESS",
                                  "exerciseTime": "2026-03-23T20:13:00",
                                  "exerciseDetail": "exercise ok"
                                }
                                """.formatted(UUID.randomUUID().toString().replace("-", ""), exerciseOrder.getBenefitOrderNo())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(post("/api/callbacks/refund")
                        .contentType(MediaType.APPLICATION_JSON)
                        .headers(signatureHeaders("nonce-refund"))
                        .content("""
                                {
                                  "requestId": "req-refund-%s",
                                  "benefitOrderNo": "%s",
                                  "refundStatus": "SUCCESS",
                                  "refundAmount": 680000,
                                  "refundTime": "2026-03-23T20:14:00",
                                  "refundReason": "manual"
                                }
                                """.formatted(UUID.randomUUID().toString().replace("-", ""), refundOrder.getBenefitOrderNo())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        BenefitOrder savedExerciseOrder = benefitOrderRepository.selectById(exerciseOrder.getBenefitOrderNo());
        BenefitOrder savedRefundOrder = benefitOrderRepository.selectById(refundOrder.getBenefitOrderNo());
        assertThat(savedExerciseOrder.getOrderStatus()).isEqualTo("EXERCISE_SUCCESS");
        assertThat(savedExerciseOrder.getQwExerciseStatus()).isEqualTo("SUCCESS");
        assertThat(savedRefundOrder.getOrderStatus()).isEqualTo("REFUND_SUCCESS");
        assertThat(savedRefundOrder.getRefundStatus()).isEqualTo("SUCCESS");
    }

    private BenefitOrder createOrder(String benefitOrderNo, String externalUserId) {
        BenefitOrder order = new BenefitOrder();
        order.setBenefitOrderNo(benefitOrderNo);
        order.setMemberId("mem-" + externalUserId);
        order.setChannelCode("KJ");
        order.setExternalUserId(externalUserId);
        order.setProductCode("PROD-NOTIFY");
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
}
