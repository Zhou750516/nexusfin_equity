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
import com.nexusfin.equity.entity.MemberPaymentProtocol;
import com.nexusfin.equity.entity.SignTask;
import com.nexusfin.equity.repository.BenefitOrderRepository;
import com.nexusfin.equity.repository.BenefitProductRepository;
import com.nexusfin.equity.repository.ContractArchiveRepository;
import com.nexusfin.equity.repository.IdempotencyRecordRepository;
import com.nexusfin.equity.repository.MemberChannelRepository;
import com.nexusfin.equity.repository.MemberInfoRepository;
import com.nexusfin.equity.repository.MemberPaymentProtocolRepository;
import com.nexusfin.equity.repository.SignTaskRepository;
import com.nexusfin.equity.util.JwtUtil;
import com.nexusfin.equity.util.SensitiveDataCipher;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
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
    private MemberPaymentProtocolRepository memberPaymentProtocolRepository;

    @Autowired
    private SignTaskRepository signTaskRepository;

    @Autowired
    private ContractArchiveRepository contractArchiveRepository;

    @Autowired
    private IdempotencyRecordRepository idempotencyRecordRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SensitiveDataCipher sensitiveDataCipher;

    @BeforeEach
    void setUp() {
        alignMemberInfoSchema();
        alignBenefitOrderProtocolSchema();
        cleanupTestData();
    }

    @Test
    void shouldWriteToAndReadFromMySql() throws Exception {
        String uniqueId = UUID.randomUUID().toString().replace("-", "");
        String requestId = REQUEST_ID_PREFIX + uniqueId;
        String externalUserId = EXTERNAL_USER_PREFIX + uniqueId;
        String productCode = PRODUCT_CODE_PREFIX + uniqueId;
        createProduct(productCode);
        MemberInfo memberInfo = createMember("mem-" + uniqueId, externalUserId);
        createChannel(memberInfo.getMemberId(), externalUserId);
        createActiveProtocol(memberInfo.getMemberId(), externalUserId);

        MvcResult createOrderResult = mockMvc.perform(post("/api/equity/orders")
                        .cookie(authCookie(memberInfo))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requestId": "%s-order",
                                  "productCode": "%s",
                                  "loanAmount": 680000,
                                  "agreementSigned": true
                                }
                                """.formatted(requestId, productCode)))
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

        mockMvc.perform(get("/api/equity/orders/{benefitOrderNo}", benefitOrderNo)
                        .cookie(authCookie(memberInfo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.benefitOrderNo").value(benefitOrderNo))
                .andExpect(jsonPath("$.data.orderStatus").value("FIRST_DEDUCT_PENDING"));

        IdempotencyRecord record = idempotencyRecordRepository.selectById(requestId + "-order");
        assertThat(record).isNotNull();
        assertThat(record.getBizKey()).isEqualTo(benefitOrderNo);
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

    private MemberInfo createMember(String memberId, String externalUserId) {
        MemberInfo memberInfo = new MemberInfo();
        memberInfo.setMemberId(memberId);
        memberInfo.setTechPlatformUserId(externalUserId);
        memberInfo.setExternalUserId(externalUserId);
        memberInfo.setMobileEncrypted(sensitiveDataCipher.encrypt("13800138000"));
        memberInfo.setMobileHash("hash-mobile-" + UUID.randomUUID());
        memberInfo.setIdCardEncrypted(sensitiveDataCipher.encrypt("110101199003071234"));
        memberInfo.setIdCardHash("hash-id-" + UUID.randomUUID());
        memberInfo.setRealNameEncrypted(sensitiveDataCipher.encrypt("MySQL测试用户"));
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

    private void alignMemberInfoSchema() {
        jdbcTemplate.execute((Connection connection) -> {
            DatabaseMetaData metaData = connection.getMetaData();
            if (!hasColumn(metaData, "member_info", "tech_platform_user_id")) {
                jdbcTemplate.execute("ALTER TABLE member_info ADD COLUMN tech_platform_user_id VARCHAR(64) NULL");
            }
            if (!hasUniqueIndex(metaData, "member_info", "tech_platform_user_id")) {
                jdbcTemplate.execute(
                        "CREATE UNIQUE INDEX uk_member_info_tech_platform_user_id ON member_info (tech_platform_user_id)"
                );
            }
            return null;
        });
    }

    private void alignBenefitOrderProtocolSchema() {
        jdbcTemplate.execute((Connection connection) -> {
            DatabaseMetaData metaData = connection.getMetaData();
            if (!hasTable(metaData, "member_payment_protocol")) {
                jdbcTemplate.execute("""
                        CREATE TABLE member_payment_protocol (
                            id BIGINT AUTO_INCREMENT PRIMARY KEY,
                            member_id VARCHAR(64) NOT NULL,
                            external_user_id VARCHAR(64) NOT NULL,
                            provider_code VARCHAR(64) NOT NULL,
                            protocol_no VARCHAR(128) NOT NULL,
                            protocol_status VARCHAR(32) NOT NULL,
                            sign_request_no VARCHAR(64) NULL,
                            channel_code VARCHAR(64) NULL,
                            signed_ts TIMESTAMP NULL,
                            expired_ts TIMESTAMP NULL,
                            last_verified_ts TIMESTAMP NULL,
                            created_ts TIMESTAMP NOT NULL,
                            updated_ts TIMESTAMP NOT NULL
                        )
                        """);
            }
            if (!hasColumn(metaData, "benefit_order", "pay_protocol_no_snapshot")) {
                jdbcTemplate.execute("ALTER TABLE benefit_order ADD COLUMN pay_protocol_no_snapshot VARCHAR(128) NULL");
            }
            if (!hasColumn(metaData, "benefit_order", "pay_protocol_source")) {
                jdbcTemplate.execute("ALTER TABLE benefit_order ADD COLUMN pay_protocol_source VARCHAR(32) NULL");
            }
            if (!hasUniqueIndex(metaData, "member_payment_protocol", "protocol_no")) {
                jdbcTemplate.execute(
                        "CREATE UNIQUE INDEX uk_provider_protocol_no ON member_payment_protocol (provider_code, protocol_no)"
                );
            }
            return null;
        });
    }

    private boolean hasColumn(DatabaseMetaData metaData, String tableName, String columnName) throws SQLException {
        try (ResultSet columns = metaData.getColumns(null, null, tableName, columnName)) {
            return columns.next();
        }
    }

    private boolean hasTable(DatabaseMetaData metaData, String tableName) throws SQLException {
        try (ResultSet tables = metaData.getTables(null, null, tableName, null)) {
            return tables.next();
        }
    }

    private boolean hasUniqueIndex(DatabaseMetaData metaData, String tableName, String columnName) throws SQLException {
        try (ResultSet indexes = metaData.getIndexInfo(null, null, tableName, true, false)) {
            while (indexes.next()) {
                String indexedColumn = indexes.getString("COLUMN_NAME");
                if (columnName.equalsIgnoreCase(indexedColumn)) {
                    return true;
                }
            }
        }
        return false;
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
        memberPaymentProtocolRepository.delete(Wrappers.<MemberPaymentProtocol>lambdaQuery()
                .likeRight(MemberPaymentProtocol::getExternalUserId, EXTERNAL_USER_PREFIX));
        memberChannelRepository.delete(Wrappers.<MemberChannel>lambdaQuery()
                .likeRight(MemberChannel::getExternalUserId, EXTERNAL_USER_PREFIX));
        memberInfoRepository.delete(Wrappers.<MemberInfo>lambdaQuery()
                .likeRight(MemberInfo::getExternalUserId, EXTERNAL_USER_PREFIX));
        idempotencyRecordRepository.delete(Wrappers.<IdempotencyRecord>lambdaQuery()
                .likeRight(IdempotencyRecord::getRequestId, REQUEST_ID_PREFIX));
        benefitProductRepository.delete(Wrappers.<BenefitProduct>lambdaQuery()
                .likeRight(BenefitProduct::getProductCode, PRODUCT_CODE_PREFIX));
    }

    private void createActiveProtocol(String memberId, String externalUserId) {
        MemberPaymentProtocol protocol = new MemberPaymentProtocol();
        protocol.setMemberId(memberId);
        protocol.setExternalUserId(externalUserId);
        protocol.setProviderCode("ALLINPAY");
        protocol.setProtocolNo("AIP-MYSQL-" + memberId);
        protocol.setProtocolStatus("ACTIVE");
        protocol.setChannelCode("KJ");
        protocol.setSignedTs(LocalDateTime.now());
        protocol.setLastVerifiedTs(LocalDateTime.now());
        protocol.setCreatedTs(LocalDateTime.now());
        protocol.setUpdatedTs(LocalDateTime.now());
        memberPaymentProtocolRepository.insert(protocol);
    }
}
