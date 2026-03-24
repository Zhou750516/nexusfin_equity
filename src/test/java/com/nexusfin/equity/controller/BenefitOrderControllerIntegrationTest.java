package com.nexusfin.equity.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nexusfin.equity.entity.BenefitOrder;
import com.nexusfin.equity.entity.BenefitProduct;
import com.nexusfin.equity.entity.ContractArchive;
import com.nexusfin.equity.entity.IdempotencyRecord;
import com.nexusfin.equity.entity.MemberChannel;
import com.nexusfin.equity.entity.MemberInfo;
import com.nexusfin.equity.entity.SignTask;
import com.nexusfin.equity.enums.MemberStatusEnum;
import com.nexusfin.equity.repository.BenefitOrderRepository;
import com.nexusfin.equity.repository.BenefitProductRepository;
import com.nexusfin.equity.repository.ContractArchiveRepository;
import com.nexusfin.equity.repository.IdempotencyRecordRepository;
import com.nexusfin.equity.repository.MemberChannelRepository;
import com.nexusfin.equity.repository.MemberInfoRepository;
import com.nexusfin.equity.repository.SignTaskRepository;
import com.nexusfin.equity.util.JwtUtil;
import java.time.LocalDateTime;
import java.util.List;
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
class BenefitOrderControllerIntegrationTest {

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
    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        contractArchiveRepository.delete(null);
        signTaskRepository.delete(null);
        benefitOrderRepository.delete(null);
        idempotencyRecordRepository.delete(null);
        memberChannelRepository.delete(null);
        memberInfoRepository.delete(null);
        benefitProductRepository.delete(null);
    }

    @Test
    void shouldLoadProductPageFromDatabase() throws Exception {
        BenefitProduct product = createProduct("P-001");
        MemberInfo memberInfo = createMember("mem-001", "user-product");

        mockMvc.perform(get("/api/equity/products/{productCode}", product.getProductCode())
                        .cookie(authCookie(memberInfo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.productCode").value(product.getProductCode()))
                .andExpect(jsonPath("$.data.productName").value(product.getProductName()))
                .andExpect(jsonPath("$.data.memberId").value(memberInfo.getMemberId()))
                .andExpect(jsonPath("$.data.externalUserId").value(memberInfo.getExternalUserId()));
    }

    @Test
    void shouldCreateBenefitOrderAndPersistAgreementArtifacts() throws Exception {
        BenefitProduct product = createProduct("P-002");
        MemberInfo memberInfo = createMember("mem-002", "user-order");
        createChannel(memberInfo.getMemberId(), "user-order");

        mockMvc.perform(post("/api/equity/orders")
                        .cookie(authCookie(memberInfo))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requestId": "req-order-create-001",
                                  "productCode": "%s",
                                  "loanAmount": 800000,
                                  "agreementSigned": true
                                }
                                """.formatted(product.getProductCode())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.benefitOrderNo").isNotEmpty())
                .andExpect(jsonPath("$.data.orderStatus").value("FIRST_DEDUCT_PENDING"));

        BenefitOrder order = benefitOrderRepository.selectOne(Wrappers.<BenefitOrder>lambdaQuery()
                .eq(BenefitOrder::getMemberId, memberInfo.getMemberId())
                .eq(BenefitOrder::getProductCode, product.getProductCode())
                .last("limit 1"));
        assertThat(order).isNotNull();
        assertThat(order.getLoanAmount()).isEqualTo(800000L);
        assertThat(order.getOrderStatus()).isEqualTo("FIRST_DEDUCT_PENDING");

        List<SignTask> signTasks = signTaskRepository.selectList(Wrappers.<SignTask>lambdaQuery()
                .eq(SignTask::getBenefitOrderNo, order.getBenefitOrderNo()));
        List<ContractArchive> archives = contractArchiveRepository.selectList(Wrappers.<ContractArchive>lambdaQuery()
                .in(ContractArchive::getTaskNo, signTasks.stream().map(SignTask::getTaskNo).toList()));
        assertThat(signTasks).hasSize(2);
        assertThat(archives).hasSize(2);
        IdempotencyRecord idempotencyRecord = idempotencyRecordRepository.selectById("req-order-create-001");
        assertThat(idempotencyRecord).isNotNull();
        assertThat(idempotencyRecord.getBizKey()).isEqualTo(order.getBenefitOrderNo());
    }

    @Test
    void shouldReplayDuplicateCreateOrderRequest() throws Exception {
        BenefitProduct product = createProduct("P-002-D");
        MemberInfo memberInfo = createMember("mem-002-d", "user-order-duplicate");
        createChannel(memberInfo.getMemberId(), "user-order-duplicate");

        String body = """
                {
                  "requestId": "req-order-create-duplicate",
                  "productCode": "%s",
                  "loanAmount": 800000,
                  "agreementSigned": true
                }
                """.formatted(product.getProductCode());

        MvcResult firstResponse = mockMvc.perform(post("/api/equity/orders")
                        .cookie(authCookie(memberInfo))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();

        MvcResult secondResponse = mockMvc.perform(post("/api/equity/orders")
                        .cookie(authCookie(memberInfo))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();

        String firstOrderNo = com.jayway.jsonpath.JsonPath.read(firstResponse.getResponse().getContentAsString(), "$.data.benefitOrderNo");
        String secondOrderNo = com.jayway.jsonpath.JsonPath.read(secondResponse.getResponse().getContentAsString(), "$.data.benefitOrderNo");
        assertThat(secondOrderNo).isEqualTo(firstOrderNo);
        assertThat(benefitOrderRepository.selectCount(Wrappers.<BenefitOrder>lambdaQuery()
                .eq(BenefitOrder::getRequestId, "req-order-create-duplicate"))).isEqualTo(1);
    }

    @Test
    void shouldReadOrderStatusFromDatabase() throws Exception {
        BenefitProduct product = createProduct("P-003");
        MemberInfo memberInfo = createMember("mem-003", "user-status");
        createChannel(memberInfo.getMemberId(), "user-status");
        BenefitOrder order = createOrder(memberInfo, product);

        mockMvc.perform(get("/api/equity/orders/{benefitOrderNo}", order.getBenefitOrderNo())
                        .cookie(authCookie(memberInfo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.benefitOrderNo").value(order.getBenefitOrderNo()))
                .andExpect(jsonPath("$.data.orderStatus").value(order.getOrderStatus()))
                .andExpect(jsonPath("$.data.qwFirstDeductStatus").value(order.getQwFirstDeductStatus()))
                .andExpect(jsonPath("$.data.grantStatus").value(order.getGrantStatus()));
    }

    private BenefitProduct createProduct(String productCode) {
        BenefitProduct product = new BenefitProduct();
        product.setProductCode(productCode);
        product.setProductName("权益产品-" + productCode);
        product.setFeeRate(299);
        product.setStatus("ACTIVE");
        product.setCreatedTs(LocalDateTime.now());
        product.setUpdatedTs(LocalDateTime.now());
        benefitProductRepository.insert(product);
        return product;
    }

    private MemberInfo createMember(String memberId, String externalUserId) {
        MemberInfo memberInfo = new MemberInfo();
        memberInfo.setMemberId(memberId);
        memberInfo.setTechPlatformUserId(externalUserId);
        memberInfo.setExternalUserId(externalUserId);
        memberInfo.setMobileEncrypted("enc-mobile-" + UUID.randomUUID());
        memberInfo.setMobileHash("hash-mobile-" + UUID.randomUUID());
        memberInfo.setIdCardEncrypted("enc-id-" + UUID.randomUUID());
        memberInfo.setIdCardHash("hash-id-" + UUID.randomUUID());
        memberInfo.setRealNameEncrypted("enc-name-" + UUID.randomUUID());
        memberInfo.setMemberStatus(MemberStatusEnum.ACTIVE.name());
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

    private BenefitOrder createOrder(MemberInfo memberInfo, BenefitProduct product) {
        BenefitOrder order = new BenefitOrder();
        order.setBenefitOrderNo("ord-" + UUID.randomUUID().toString().replace("-", ""));
        order.setMemberId(memberInfo.getMemberId());
        order.setChannelCode("KJ");
        order.setExternalUserId(memberInfo.getExternalUserId());
        order.setProductCode(product.getProductCode());
        order.setAgreementNo("agr-" + UUID.randomUUID().toString().replace("-", ""));
        order.setLoanAmount(500000L);
        order.setOrderStatus("FIRST_DEDUCT_PENDING");
        order.setQwFirstDeductStatus("PENDING");
        order.setQwFallbackDeductStatus("NONE");
        order.setQwExerciseStatus("NONE");
        order.setRefundStatus("NONE");
        order.setGrantStatus("PENDING");
        order.setSyncStatus("SYNC_PENDING");
        order.setRequestId("req-" + UUID.randomUUID().toString().replace("-", ""));
        order.setCreatedTs(LocalDateTime.now());
        order.setUpdatedTs(LocalDateTime.now());
        benefitOrderRepository.insert(order);
        return order;
    }

    private jakarta.servlet.http.Cookie authCookie(MemberInfo memberInfo) {
        return new jakarta.servlet.http.Cookie(
                "NEXUSFIN_AUTH",
                jwtUtil.generateToken(memberInfo.getMemberId(), memberInfo.getExternalUserId())
        );
    }
}
