package com.nexusfin.equity.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nexusfin.equity.entity.BenefitOrder;
import com.nexusfin.equity.entity.BenefitProduct;
import com.nexusfin.equity.entity.MemberChannel;
import com.nexusfin.equity.entity.MemberInfo;
import com.nexusfin.equity.entity.MemberPaymentProtocol;
import com.nexusfin.equity.enums.MemberStatusEnum;
import com.nexusfin.equity.repository.BenefitOrderRepository;
import com.nexusfin.equity.repository.BenefitProductRepository;
import com.nexusfin.equity.repository.ContractArchiveRepository;
import com.nexusfin.equity.repository.IdempotencyRecordRepository;
import com.nexusfin.equity.repository.MemberChannelRepository;
import com.nexusfin.equity.repository.MemberInfoRepository;
import com.nexusfin.equity.repository.MemberPaymentProtocolRepository;
import com.nexusfin.equity.repository.SignTaskRepository;
import com.nexusfin.equity.service.XiaohuaGatewayService;
import com.nexusfin.equity.thirdparty.yunka.ProtocolLink;
import com.nexusfin.equity.thirdparty.yunka.ProtocolQueryResponse;
import com.nexusfin.equity.thirdparty.yunka.UserCardListResponse;
import com.nexusfin.equity.util.JwtUtil;
import com.nexusfin.equity.util.SensitiveDataCipher;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class Phase9TaskGroupAIntegrationTest {

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
    private JwtUtil jwtUtil;

    @Autowired
    private SensitiveDataCipher sensitiveDataCipher;

    @MockBean
    private XiaohuaGatewayService xiaohuaGatewayService;

    @BeforeEach
    void setUp() {
        contractArchiveRepository.delete(null);
        signTaskRepository.delete(null);
        benefitOrderRepository.delete(null);
        idempotencyRecordRepository.delete(null);
        memberPaymentProtocolRepository.delete(null);
        memberChannelRepository.delete(null);
        memberInfoRepository.delete(null);
        benefitProductRepository.delete(null);

        // Safe defaults: match what mock-mode RestYunkaGatewayClient returns (empty lists),
        // so card-detail tests keep working. The activate test overrides queryProtocols below.
        lenient().when(xiaohuaGatewayService.queryProtocols(any(), any(), any()))
                .thenReturn(new ProtocolQueryResponse(List.of()));
        lenient().when(xiaohuaGatewayService.queryUserCards(any(), any(), any()))
                .thenReturn(new UserCardListResponse(List.of()));
    }

    @Test
    void shouldRequireAuthenticationForTaskGroupAInterfaces() throws Exception {
        mockMvc.perform(get("/api/loan/calculator-config"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        mockMvc.perform(get("/api/benefits/card-detail"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        mockMvc.perform(post("/api/benefits/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "applicationId": "APP-auth-check",
                                  "cardType": "huixuan_card"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void shouldReturnCalculatorConfigForAuthenticatedUser() throws Exception {
        MemberInfo memberInfo = createMember("mem-loan-config", "user-loan-config");

        mockMvc.perform(get("/api/loan/calculator-config")
                        .cookie(authCookie(memberInfo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.amountRange.min").value(100))
                .andExpect(jsonPath("$.data.amountRange.max").value(5000))
                .andExpect(jsonPath("$.data.amountRange.step").value(100))
                .andExpect(jsonPath("$.data.amountRange.default").value(3000))
                .andExpect(jsonPath("$.data.termOptions[0].label").value("3期"))
                .andExpect(jsonPath("$.data.termOptions[0].value").value(3))
                .andExpect(jsonPath("$.data.lender").value("XX商业银行"))
                .andExpect(jsonPath("$.data.receivingAccount.bankName").value("招商银行"))
                .andExpect(jsonPath("$.data.receivingAccount.lastFour").value("8648"))
                .andExpect(jsonPath("$.data.receivingAccount.accountId").value("acc_001"));
    }

    @Test
    void shouldReturnBenefitsCardDetailForAuthenticatedUser() throws Exception {
        MemberInfo memberInfo = createMember("mem-benefit-detail", "user-benefit-detail");
        createProduct("HUXUAN_CARD");

        mockMvc.perform(get("/api/benefits/card-detail")
                        .cookie(authCookie(memberInfo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.cardName").value("惠选卡"))
                .andExpect(jsonPath("$.data.price").value(300))
                .andExpect(jsonPath("$.data.totalSaving").value(448))
                .andExpect(jsonPath("$.data.features[0].title").isNotEmpty())
                .andExpect(jsonPath("$.data.categories[0].name").value("影音会员"))
                .andExpect(jsonPath("$.data.categories[0].benefits[0].title").isNotEmpty())
                .andExpect(jsonPath("$.data.tips[0]").isNotEmpty())
                .andExpect(jsonPath("$.data.protocols[0].name").value("用户服务协议"));
    }

    @Test
    void shouldReturnBenefitsCardDetailInEnglishWhenAcceptLanguageProvided() throws Exception {
        MemberInfo memberInfo = createMember("mem-benefit-detail-en", "user-benefit-detail-en");
        createProduct("HUXUAN_CARD");

        mockMvc.perform(get("/api/benefits/card-detail")
                        .cookie(authCookie(memberInfo))
                        .header("Accept-Language", "en-US,en;q=0.9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.cardName").value("Benefit Card"))
                .andExpect(jsonPath("$.data.categories[0].name").value("Streaming Membership"))
                .andExpect(jsonPath("$.data.protocols[0].name").value("User Service Agreement"))
                .andExpect(header().string("Content-Language", "en-US"));
    }

    @Test
    void shouldActivateBenefitsCardByCreatingBenefitOrder() throws Exception {
        BenefitProduct product = createProduct("HUXUAN_CARD");
        MemberInfo memberInfo = createMember("mem-benefit-activate", "user-benefit-activate");
        createChannel(memberInfo.getMemberId(), memberInfo.getExternalUserId());
        createActiveProtocol(memberInfo.getMemberId(), memberInfo.getExternalUserId());

        // Override default empty protocols: activate flow checks protocolReady which requires
        // a non-empty dynamic protocol list (in addition to the active payment protocol seeded above).
        when(xiaohuaGatewayService.queryProtocols(any(), eq("benefits-card-detail"), any()))
                .thenReturn(new ProtocolQueryResponse(List.of(
                        new ProtocolLink("借款协议", 1, "https://agreements/loan")
                )));

        mockMvc.perform(post("/api/benefits/activate")
                        .cookie(authCookie(memberInfo))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "applicationId": "APP202604130001",
                                  "cardType": "huixuan_card"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.activationId").isNotEmpty())
                .andExpect(jsonPath("$.data.status").value("activated"))
                .andExpect(jsonPath("$.data.message").value("惠选卡开通成功"));

        BenefitOrder savedOrder = benefitOrderRepository.selectOne(Wrappers.<BenefitOrder>lambdaQuery()
                .eq(BenefitOrder::getMemberId, memberInfo.getMemberId())
                .eq(BenefitOrder::getProductCode, product.getProductCode())
                .last("limit 1"));
        assertThat(savedOrder).isNotNull();
        assertThat(savedOrder.getOrderStatus()).isEqualTo("FIRST_DEDUCT_PENDING");
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
