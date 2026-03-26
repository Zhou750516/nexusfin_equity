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
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("mysql-it")
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "MYSQL_IT_ENABLED", matches = "true")
class MySqlCallbackFlowIntegrationTest {

    private static final String EXTERNAL_USER_PREFIX = "mysql-it-callback-user-";
    private static final String REQUEST_ID_PREFIX = "req-mysql-it-callback-";

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
        cleanupTestData();
    }

    @Test
    void shouldPersistPaymentAndGrantFlowInMySql() throws Exception {
        String uniqueId = UUID.randomUUID().toString().replace("-", "");
        BenefitOrder order = createOrder("ord-mysql-callback-" + uniqueId, EXTERNAL_USER_PREFIX + uniqueId);

        String firstDeductRequestId = REQUEST_ID_PREFIX + "first-" + uniqueId;
        mockMvc.perform(post("/api/callbacks/first-deduction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .headers(signatureHeaders("mysql-callback-first"))
                        .content("""
                                {
                                  "requestId": "%s",
                                  "benefitOrderNo": "%s",
                                  "qwTradeNo": "qw-%s",
                                  "deductStatus": "FAIL",
                                  "deductAmount": 680000,
                                  "failReason": "BANK_REJECT",
                                  "deductTime": "2026-03-23T20:20:00"
                                }
                                """.formatted(firstDeductRequestId, order.getBenefitOrderNo(), uniqueId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paymentStatus").value("FAIL"));

        BenefitOrder failedOrder = benefitOrderRepository.selectById(order.getBenefitOrderNo());
        assertThat(failedOrder.getOrderStatus()).isEqualTo("FIRST_DEDUCT_FAIL");

        String grantRequestId = REQUEST_ID_PREFIX + "grant-" + uniqueId;
        mockMvc.perform(post("/api/callbacks/grant/forward")
                        .contentType(MediaType.APPLICATION_JSON)
                        .headers(signatureHeaders("mysql-callback-grant"))
                        .content("""
                                {
                                  "requestId": "%s",
                                  "benefitOrderNo": "%s",
                                  "grantStatus": "SUCCESS",
                                  "actualAmount": 680000,
                                  "loanOrderNo": "loan-%s",
                                  "failReason": null,
                                  "grantTime": "2026-03-23T20:21:00",
                                  "timestamp": 1711196460
                                }
                                """.formatted(grantRequestId, order.getBenefitOrderNo(), uniqueId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        BenefitOrder fallbackOrder = benefitOrderRepository.selectById(order.getBenefitOrderNo());
        assertThat(fallbackOrder.getGrantStatus()).isEqualTo("SUCCESS");
        assertThat(fallbackOrder.getOrderStatus()).isEqualTo("FALLBACK_DEDUCT_PENDING");
        assertThat(fallbackOrder.getFallbackDeductStatus()).isEqualTo("PENDING");

        assertThat(paymentRecordRepository.selectCount(Wrappers.<PaymentRecord>lambdaQuery()
                .eq(PaymentRecord::getBenefitOrderNo, order.getBenefitOrderNo()))).isEqualTo(2);
        assertThat(notificationReceiveLogRepository.selectCount(Wrappers.<NotificationReceiveLog>lambdaQuery()
                .eq(NotificationReceiveLog::getBenefitOrderNo, order.getBenefitOrderNo())
                .eq(NotificationReceiveLog::getNotifyType, "GRANT_RESULT"))).isEqualTo(1);
        assertThat(idempotencyRecordRepository.selectCount(Wrappers.<IdempotencyRecord>lambdaQuery()
                .likeRight(IdempotencyRecord::getRequestId, REQUEST_ID_PREFIX))).isEqualTo(2);
    }

    private BenefitOrder createOrder(String benefitOrderNo, String externalUserId) {
        String compactId = UUID.randomUUID().toString().replace("-", "");
        BenefitOrder order = new BenefitOrder();
        order.setBenefitOrderNo(benefitOrderNo);
        order.setMemberId("mem-" + externalUserId);
        order.setSourceChannelCode("KJ");
        order.setExternalUserId(externalUserId);
        order.setProductCode("MYSQL-CALLBACK-PROD");
        order.setAgreementNo("agr-" + externalUserId);
        order.setLoanAmount(680000L);
        order.setOrderStatus("FIRST_DEDUCT_PENDING");
        order.setFirstDeductStatus("PENDING");
        order.setFallbackDeductStatus("NONE");
        order.setExerciseStatus("NONE");
        order.setRefundStatus("NONE");
        order.setGrantStatus("PENDING");
        order.setSyncStatus("SYNC_PENDING");
        order.setRequestId("req-order-" + compactId);
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

    private void cleanupTestData() {
        List<BenefitOrder> orders = benefitOrderRepository.selectList(Wrappers.<BenefitOrder>lambdaQuery()
                .likeRight(BenefitOrder::getExternalUserId, EXTERNAL_USER_PREFIX));
        List<String> orderNos = orders.stream().map(BenefitOrder::getBenefitOrderNo).toList();
        if (!orderNos.isEmpty()) {
            paymentRecordRepository.delete(Wrappers.<PaymentRecord>lambdaQuery()
                    .in(PaymentRecord::getBenefitOrderNo, orderNos));
            notificationReceiveLogRepository.delete(Wrappers.<NotificationReceiveLog>lambdaQuery()
                    .in(NotificationReceiveLog::getBenefitOrderNo, orderNos));
            benefitOrderRepository.delete(Wrappers.<BenefitOrder>lambdaQuery()
                    .in(BenefitOrder::getBenefitOrderNo, orderNos));
        }
        idempotencyRecordRepository.delete(Wrappers.<IdempotencyRecord>lambdaQuery()
                .likeRight(IdempotencyRecord::getRequestId, REQUEST_ID_PREFIX));
    }
}
