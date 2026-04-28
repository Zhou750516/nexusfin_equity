package com.nexusfin.equity.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusfin.equity.entity.BenefitOrder;
import com.nexusfin.equity.entity.BenefitProduct;
import com.nexusfin.equity.entity.LoanApplicationMapping;
import com.nexusfin.equity.entity.MemberChannel;
import com.nexusfin.equity.entity.MemberInfo;
import com.nexusfin.equity.entity.MemberPaymentProtocol;
import com.nexusfin.equity.enums.MemberStatusEnum;
import com.nexusfin.equity.exception.UpstreamTimeoutException;
import com.nexusfin.equity.repository.BenefitOrderRepository;
import com.nexusfin.equity.repository.BenefitProductRepository;
import com.nexusfin.equity.repository.ContractArchiveRepository;
import com.nexusfin.equity.repository.IdempotencyRecordRepository;
import com.nexusfin.equity.repository.LoanApplicationMappingRepository;
import com.nexusfin.equity.repository.MemberChannelRepository;
import com.nexusfin.equity.repository.MemberInfoRepository;
import com.nexusfin.equity.repository.MemberPaymentProtocolRepository;
import com.nexusfin.equity.repository.SignTaskRepository;
import com.nexusfin.equity.service.AsyncCompensationEnqueueService;
import com.nexusfin.equity.thirdparty.yunka.YunkaGatewayClient;
import com.nexusfin.equity.util.JwtUtil;
import com.nexusfin.equity.util.SensitiveDataCipher;
import jakarta.servlet.http.Cookie;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class Phase9TaskGroupEIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
    private LoanApplicationMappingRepository loanApplicationMappingRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private SensitiveDataCipher sensitiveDataCipher;

    @MockBean
    private YunkaGatewayClient yunkaGatewayClient;

    @MockBean
    private AsyncCompensationEnqueueService asyncCompensationEnqueueService;

    @BeforeEach
    void setUp() {
        contractArchiveRepository.delete(null);
        signTaskRepository.delete(null);
        benefitOrderRepository.delete(null);
        idempotencyRecordRepository.delete(null);
        memberPaymentProtocolRepository.delete(null);
        memberChannelRepository.delete(null);
        loanApplicationMappingRepository.delete(null);
        memberInfoRepository.delete(null);
        benefitProductRepository.delete(null);
    }

    @Test
    void shouldCreateBenefitsThenForwardLoanApplyAndSaveApplicationMapping(CapturedOutput output) throws Exception {
        MemberInfo memberInfo = createReadyMember("mem-loan-apply", "user-loan-apply");
        createProduct("HUXUAN_CARD");
        JsonNode yunkaData = objectMapper.readTree("""
                {
                  "status": "4001",
                  "loanId": "LOAN202604130101",
                  "remark": "借款申请已受理"
                }
                """);
        when(yunkaGatewayClient.proxy(any()))
                .thenReturn(new YunkaGatewayClient.YunkaGatewayResponse(0, "SUCCESS", yunkaData));

        mockMvc.perform(post("/api/loan/apply")
                        .cookie(authCookie(memberInfo))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount": 3000,
                                  "term": 3,
                                  "receivingAccountId": "acc_001",
                                  "agreedProtocols": ["user_agreement", "loan_agreement", "privacy_policy"],
                                  "purpose": "shopping"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.applicationId").isNotEmpty())
                .andExpect(jsonPath("$.data.status").value("pending"))
                .andExpect(jsonPath("$.data.estimatedTime").value("30分钟"))
                .andExpect(jsonPath("$.data.benefitsActivated").value(true))
                .andExpect(jsonPath("$.data.benefitOrderNo").isNotEmpty());

        BenefitOrder benefitOrder = benefitOrderRepository.selectOne(Wrappers.<BenefitOrder>lambdaQuery()
                .eq(BenefitOrder::getMemberId, memberInfo.getMemberId())
                .last("limit 1"));
        assertThat(benefitOrder).isNotNull();

        LoanApplicationMapping mapping = loanApplicationMappingRepository.selectOne(
                Wrappers.<LoanApplicationMapping>lambdaQuery()
                        .eq(LoanApplicationMapping::getMemberId, memberInfo.getMemberId())
                        .last("limit 1"));
        assertThat(mapping).isNotNull();
        assertThat(mapping.getBenefitOrderNo()).isEqualTo(benefitOrder.getBenefitOrderNo());
        assertThat(mapping.getUpstreamQueryValue()).isEqualTo("LOAN202604130101");
        assertThat(mapping.getMappingStatus()).isEqualTo("ACTIVE");

        ArgumentCaptor<YunkaGatewayClient.YunkaGatewayRequest> requestCaptor =
                ArgumentCaptor.forClass(YunkaGatewayClient.YunkaGatewayRequest.class);
        org.mockito.Mockito.verify(yunkaGatewayClient).proxy(requestCaptor.capture());
        JsonNode data = objectMapper.valueToTree(requestCaptor.getValue().data());
        assertThat(requestCaptor.getValue().path()).isEqualTo("/loan/apply");
        assertThat(data.get("uid").asText()).isEqualTo("user-loan-apply");
        assertThat(data.get("benefitOrderNo").asText()).isEqualTo(benefitOrder.getBenefitOrderNo());
        assertThat(data.get("applyId").asText()).isEqualTo(mapping.getApplicationId());
        assertThat(data.get("loanId").asText()).startsWith("LN-");
        assertThat(data.get("loanAmount").asLong()).isEqualTo(300000L);
        assertThat(data.get("loanPeriod").asInt()).isEqualTo(3);
        assertThat(data.get("bankCardNo").asText()).isEqualTo("acc_001");
        assertThat(output).contains("loan apply yunka request begin");
        assertThat(output).contains("loan apply yunka request success");
        assertThat(output).contains("path=/loan/apply");
        assertThat(output).contains("bizOrderNo=");
    }

    @Test
    void shouldPersistPurposeFromLoanApplyRequest() throws Exception {
        MemberInfo memberInfo = createReadyMember("mem-loan-purpose", "user-loan-purpose");
        createProduct("HUXUAN_CARD");
        JsonNode yunkaData = objectMapper.readTree("""
                {
                  "status": "4001",
                  "loanId": "LOAN202604270001",
                  "remark": "借款申请已受理"
                }
                """);
        when(yunkaGatewayClient.proxy(any()))
                .thenReturn(new YunkaGatewayClient.YunkaGatewayResponse(0, "SUCCESS", yunkaData));

        mockMvc.perform(post("/api/loan/apply")
                        .cookie(authCookie(memberInfo))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount": 3000,
                                  "term": 3,
                                  "receivingAccountId": "acc_001",
                                  "agreedProtocols": ["user_agreement", "loan_agreement", "privacy_policy"],
                                  "purpose": "rent"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        String purpose = jdbcTemplate.queryForObject(
                "select purpose from loan_application_mapping where member_id = ?",
                String.class,
                memberInfo.getMemberId()
        );
        assertThat(purpose).isEqualTo("rent");
    }

    @Test
    void shouldRejectLoanApplyWhenPurposeIsMissing() throws Exception {
        MemberInfo memberInfo = createReadyMember("mem-loan-purpose-missing", "user-loan-purpose-missing");
        createProduct("HUXUAN_CARD");

        mockMvc.perform(post("/api/loan/apply")
                        .cookie(authCookie(memberInfo))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount": 3000,
                                  "term": 3,
                                  "receivingAccountId": "acc_001",
                                  "agreedProtocols": ["user_agreement", "loan_agreement", "privacy_policy"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("purpose")));
    }

    @Test
    void shouldReturnLoanFailedWhenBenefitsCreatedButYunkaLoanApplyFails() throws Exception {
        MemberInfo memberInfo = createReadyMember("mem-loan-apply-partial", "user-loan-apply-partial");
        createProduct("HUXUAN_CARD");
        when(yunkaGatewayClient.proxy(any()))
                .thenReturn(new YunkaGatewayClient.YunkaGatewayResponse(502, "Yunka unavailable", null));

        mockMvc.perform(post("/api/loan/apply")
                        .cookie(authCookie(memberInfo))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount": 3000,
                                  "term": 3,
                                  "receivingAccountId": "acc_001",
                                  "agreedProtocols": ["user_agreement", "loan_agreement", "privacy_policy"],
                                  "purpose": "shopping"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("loan_failed"))
                .andExpect(jsonPath("$.data.applicationId").doesNotExist())
                .andExpect(jsonPath("$.data.benefitsActivated").value(true))
                .andExpect(jsonPath("$.data.message").value("权益购买成功，借款申请失败：Yunka unavailable"));

        BenefitOrder benefitOrder = benefitOrderRepository.selectOne(Wrappers.<BenefitOrder>lambdaQuery()
                .eq(BenefitOrder::getMemberId, memberInfo.getMemberId())
                .last("limit 1"));
        assertThat(benefitOrder).isNotNull();
        assertThat(loanApplicationMappingRepository.selectCount(null)).isZero();
    }

    @Test
    void shouldReturnPendingWhenLoanApplyTimesOut() throws Exception {
        MemberInfo memberInfo = createReadyMember("mem-loan-timeout", "user-loan-timeout");
        createProduct("HUXUAN_CARD");
        when(yunkaGatewayClient.proxy(any()))
                .thenThrow(new UpstreamTimeoutException("Yunka loan apply timeout"));

        mockMvc.perform(post("/api/loan/apply")
                        .cookie(authCookie(memberInfo))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount": 3000,
                                  "term": 3,
                                  "receivingAccountId": "acc_001",
                                  "agreedProtocols": ["user_agreement", "loan_agreement", "privacy_policy"],
                                  "purpose": "shopping"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.applicationId").isNotEmpty())
                .andExpect(jsonPath("$.data.status").value("pending"))
                .andExpect(jsonPath("$.data.benefitsActivated").value(true))
                .andExpect(jsonPath("$.data.message").value("借款申请已提交，正在审核中"));

        LoanApplicationMapping mapping = loanApplicationMappingRepository.selectOne(
                Wrappers.<LoanApplicationMapping>lambdaQuery()
                        .eq(LoanApplicationMapping::getMemberId, memberInfo.getMemberId())
                        .last("limit 1"));
        assertThat(mapping).isNotNull();
        assertThat(mapping.getMappingStatus()).isEqualTo("PENDING_REVIEW");
    }

    private BenefitProduct createProduct(String productCode) {
        BenefitProduct product = new BenefitProduct();
        product.setProductCode(productCode);
        product.setProductName("惠选卡");
        product.setFeeRate(300);
        product.setStatus("ACTIVE");
        product.setCreatedTs(LocalDateTime.now());
        product.setUpdatedTs(LocalDateTime.now());
        benefitProductRepository.insert(product);
        return product;
    }

    private MemberInfo createReadyMember(String memberId, String externalUserId) {
        MemberInfo memberInfo = createMember(memberId, externalUserId);
        createChannel(memberId, externalUserId);
        createActiveProtocol(memberId, externalUserId);
        return memberInfo;
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
        memberInfo.setRealNameEncrypted(sensitiveDataCipher.encrypt("测试用户"));
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

    private void createActiveProtocol(String memberId, String externalUserId) {
        MemberPaymentProtocol protocol = new MemberPaymentProtocol();
        protocol.setMemberId(memberId);
        protocol.setExternalUserId(externalUserId);
        protocol.setProviderCode("ALLINPAY");
        protocol.setProtocolNo("AIP-TEST-" + memberId);
        protocol.setProtocolStatus("ACTIVE");
        protocol.setChannelCode("KJ");
        protocol.setSignedTs(LocalDateTime.now());
        protocol.setLastVerifiedTs(LocalDateTime.now());
        protocol.setCreatedTs(LocalDateTime.now());
        protocol.setUpdatedTs(LocalDateTime.now());
        memberPaymentProtocolRepository.insert(protocol);
    }

    private Cookie authCookie(MemberInfo memberInfo) {
        return new Cookie(
                "NEXUSFIN_AUTH",
                jwtUtil.generateToken(memberInfo.getMemberId(), memberInfo.getExternalUserId())
        );
    }
}
