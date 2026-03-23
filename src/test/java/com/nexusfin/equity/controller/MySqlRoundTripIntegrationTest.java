package com.nexusfin.equity.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusfin.equity.entity.BenefitOrder;
import com.nexusfin.equity.entity.BenefitProduct;
import com.nexusfin.equity.entity.ContractArchive;
import com.nexusfin.equity.entity.IdempotencyRecord;
import com.nexusfin.equity.entity.MemberChannel;
import com.nexusfin.equity.entity.MemberInfo;
import com.nexusfin.equity.entity.SignTask;
import com.nexusfin.equity.repository.BenefitOrderRepository;
import com.nexusfin.equity.repository.BenefitProductRepository;
import com.nexusfin.equity.repository.ContractArchiveRepository;
import com.nexusfin.equity.repository.IdempotencyRecordRepository;
import com.nexusfin.equity.repository.MemberChannelRepository;
import com.nexusfin.equity.repository.MemberInfoRepository;
import com.nexusfin.equity.repository.SignTaskRepository;
import com.nexusfin.equity.util.SignatureUtil;
import java.time.LocalDateTime;
import java.time.Instant;
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
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("mysql-it")
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "MYSQL_IT_ENABLED", matches = "true")
class MySqlRoundTripIntegrationTest {

    private static final String EXTERNAL_USER_PREFIX = "mysql-it-user-";
    private static final String REQUEST_ID_PREFIX = "req-mysql-it-";
    private static final String PRODUCT_CODE_PREFIX = "MYSQL-IT-PROD-";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BenefitProductRepository benefitProductRepository;

    @Autowired
    private BenefitOrderRepository benefitOrderRepository;

    @Autowired
    private MemberInfoRepository memberInfoRepository;

    @Autowired
    private MemberChannelRepository memberChannelRepository;

    @Autowired
    private SignTaskRepository signTaskRepository;

    @Autowired
    private ContractArchiveRepository contractArchiveRepository;

    @Autowired
    private IdempotencyRecordRepository idempotencyRecordRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        cleanupTestData();
    }

    @Test
    void shouldWriteToAndReadFromMySql() throws Exception {
        String uniqueId = UUID.randomUUID().toString().replace("-", "");
        String requestId = REQUEST_ID_PREFIX + uniqueId;
        String externalUserId = EXTERNAL_USER_PREFIX + uniqueId;
        String productCode = PRODUCT_CODE_PREFIX + uniqueId;
        createProduct(productCode);

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .headers(signatureHeaders("mysql-nonce-1"))
                        .content(registerRequest(requestId, externalUserId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.registerStatus").value("SUCCESS"));

        MemberChannel memberChannel = memberChannelRepository.selectOne(Wrappers.<MemberChannel>lambdaQuery()
                .eq(MemberChannel::getChannelCode, "KJ")
                .eq(MemberChannel::getExternalUserId, externalUserId)
                .last("limit 1"));
        assertThat(memberChannel).isNotNull();

        MemberInfo memberInfo = memberInfoRepository.selectById(memberChannel.getMemberId());
        assertThat(memberInfo).isNotNull();

        MvcResult createOrderResult = mockMvc.perform(post("/api/equity/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "memberId": "%s",
                                  "productCode": "%s",
                                  "loanAmount": 680000,
                                  "agreementSigned": true
                                }
                                """.formatted(memberInfo.getMemberId(), productCode)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.benefitOrderNo").isNotEmpty())
                .andReturn();

        JsonNode responseBody = objectMapper.readTree(createOrderResult.getResponse().getContentAsString());
        String benefitOrderNo = responseBody.path("data").path("benefitOrderNo").asText();
        assertThat(benefitOrderNo).isNotBlank();

        BenefitOrder benefitOrder = benefitOrderRepository.selectById(benefitOrderNo);
        assertThat(benefitOrder).isNotNull();
        assertThat(benefitOrder.getLoanAmount()).isEqualTo(680000L);
        assertThat(benefitOrder.getProductCode()).isEqualTo(productCode);

        List<SignTask> signTasks = signTaskRepository.selectList(Wrappers.<SignTask>lambdaQuery()
                .eq(SignTask::getBenefitOrderNo, benefitOrderNo));
        assertThat(signTasks).hasSize(2);
        assertThat(contractArchiveRepository.selectCount(Wrappers.<ContractArchive>lambdaQuery()
                .in(ContractArchive::getTaskNo, signTasks.stream().map(SignTask::getTaskNo).toList()))).isEqualTo(2);

        mockMvc.perform(get("/api/equity/orders/{benefitOrderNo}", benefitOrderNo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.benefitOrderNo").value(benefitOrderNo))
                .andExpect(jsonPath("$.data.orderStatus").value("FIRST_DEDUCT_PENDING"));

        IdempotencyRecord record = idempotencyRecordRepository.selectById(requestId);
        assertThat(record).isNotNull();
        assertThat(record.getBizKey()).isEqualTo(memberInfo.getMemberId());
    }

    private void createProduct(String productCode) {
        BenefitProduct product = new BenefitProduct();
        product.setProductCode(productCode);
        product.setProductName("MySQL权益产品");
        product.setFeeRate(399);
        product.setStatus("ACTIVE");
        product.setCreatedTs(LocalDateTime.now());
        product.setUpdatedTs(LocalDateTime.now());
        benefitProductRepository.insert(product);
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

    private String registerRequest(String requestId, String externalUserId) {
        return """
                {
                  "requestId": "%s",
                  "channelCode": "KJ",
                  "userInfo": {
                    "externalUserId": "%s",
                    "mobileEncrypted": "13600000000",
                    "idCardEncrypted": "310101199001011236",
                    "realNameEncrypted": "李四"
                  }
                }
                """.formatted(requestId, externalUserId);
    }

    private void cleanupTestData() {
        List<BenefitOrder> orders = benefitOrderRepository.selectList(Wrappers.<BenefitOrder>lambdaQuery()
                .likeRight(BenefitOrder::getExternalUserId, EXTERNAL_USER_PREFIX));
        List<String> orderNos = orders.stream().map(BenefitOrder::getBenefitOrderNo).toList();
        List<SignTask> signTasks = orderNos.isEmpty()
                ? List.of()
                : signTaskRepository.selectList(Wrappers.<SignTask>lambdaQuery()
                        .in(SignTask::getBenefitOrderNo, orderNos));
        if (!signTasks.isEmpty()) {
            contractArchiveRepository.delete(Wrappers.<ContractArchive>lambdaQuery()
                    .in(ContractArchive::getTaskNo, signTasks.stream().map(SignTask::getTaskNo).toList()));
            signTaskRepository.delete(Wrappers.<SignTask>lambdaQuery()
                    .in(SignTask::getTaskNo, signTasks.stream().map(SignTask::getTaskNo).toList()));
        }

        benefitOrderRepository.delete(Wrappers.<BenefitOrder>lambdaQuery()
                .likeRight(BenefitOrder::getExternalUserId, EXTERNAL_USER_PREFIX));
        memberChannelRepository.delete(Wrappers.<MemberChannel>lambdaQuery()
                .likeRight(MemberChannel::getExternalUserId, EXTERNAL_USER_PREFIX));
        memberInfoRepository.delete(Wrappers.<MemberInfo>lambdaQuery()
                .likeRight(MemberInfo::getExternalUserId, EXTERNAL_USER_PREFIX));
        idempotencyRecordRepository.delete(Wrappers.<IdempotencyRecord>lambdaQuery()
                .likeRight(IdempotencyRecord::getRequestId, REQUEST_ID_PREFIX));
        benefitProductRepository.delete(Wrappers.<BenefitProduct>lambdaQuery()
                .likeRight(BenefitProduct::getProductCode, PRODUCT_CODE_PREFIX));
    }
}
