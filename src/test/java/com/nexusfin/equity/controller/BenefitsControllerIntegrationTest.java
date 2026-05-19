package com.nexusfin.equity.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
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
import com.nexusfin.equity.service.AsyncCompensationEnqueueService;
import com.nexusfin.equity.service.BenefitRedirectUrlService;
import com.nexusfin.equity.service.XiaohuaGatewayService;
import com.nexusfin.equity.thirdparty.yunka.BenefitOrderSyncRequest;
import com.nexusfin.equity.thirdparty.yunka.BenefitOrderSyncResponse;
import com.nexusfin.equity.thirdparty.yunka.ProtocolLink;
import com.nexusfin.equity.thirdparty.yunka.ProtocolQueryResponse;
import com.nexusfin.equity.thirdparty.yunka.UserCardListResponse;
import com.nexusfin.equity.thirdparty.yunka.UserCardSummary;
import com.nexusfin.equity.util.JwtUtil;
import com.nexusfin.equity.util.SensitiveDataCipher;
import jakarta.servlet.http.Cookie;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BenefitsControllerIntegrationTest {

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

    @MockBean
    private AsyncCompensationEnqueueService asyncCompensationEnqueueService;

    @MockBean
    private BenefitRedirectUrlService benefitRedirectUrlService;

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
    }

    @Test
    void shouldReturnDynamicProtocolsAndUserCards() throws Exception {
        MemberInfo memberInfo = createReadyMember("mem-benefits-dynamic", "user-benefits-dynamic");
        createProduct();
        when(xiaohuaGatewayService.queryProtocols(any(), eq("benefits-card-detail"), any()))
                .thenReturn(new ProtocolQueryResponse(List.of(
                        new ProtocolLink("借款协议", 1, "https://agreements/loan")
                )));
        when(xiaohuaGatewayService.queryUserCards(any(), eq("benefits-card-detail"), any()))
                .thenReturn(new UserCardListResponse(List.of(
                        new UserCardSummary("card-001", "招商银行", "8648", 1)
                )));

        mockMvc.perform(get("/api/benefits/card-detail")
                        .cookie(authCookie(memberInfo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.protocols[?(@.url=='https://agreements/loan')]").exists())
                .andExpect(jsonPath("$.data.userCards[0].bankName").value("招商银行"))
                .andExpect(jsonPath("$.data.protocolReady").value(true));
    }

    @Test
    void shouldBlockActivationWhenDynamicProtocolsAreMissing() throws Exception {
        MemberInfo memberInfo = createReadyMember("mem-benefits-block", "user-benefits-block");
        createProduct();
        when(xiaohuaGatewayService.queryProtocols(any(), eq("benefits-card-detail"), any()))
                .thenReturn(new ProtocolQueryResponse(List.of()));
        when(xiaohuaGatewayService.queryUserCards(any(), eq("benefits-card-detail"), any()))
                .thenReturn(new UserCardListResponse(List.of()));

        mockMvc.perform(post("/api/benefits/activate")
                        .cookie(authCookie(memberInfo))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "applicationId": "APP-benefits-block",
                                  "cardType": "huixuan_card",
                                  "token": "joint-token-benefits-block"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(-1))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("BENEFITS_PROTOCOL_NOT_READY")));
    }

    @Test
    void shouldSyncBenefitOrderAfterActivation() throws Exception {
        MemberInfo memberInfo = createReadyMember("mem-benefits-sync", "user-benefits-sync");
        createProduct();
        when(xiaohuaGatewayService.queryProtocols(any(), eq("benefits-card-detail"), any()))
                .thenReturn(new ProtocolQueryResponse(List.of(
                        new ProtocolLink("借款协议", 1, "https://agreements/loan")
                )));
        when(xiaohuaGatewayService.queryUserCards(any(), eq("benefits-card-detail"), any()))
                .thenReturn(new UserCardListResponse(List.of(
                        new UserCardSummary("card-001", "招商银行", "8648", 1)
                )));
        when(xiaohuaGatewayService.syncBenefitOrder(any(), any(), any()))
                .thenReturn(new BenefitOrderSyncResponse("SUCCESS", "ok"));

        mockMvc.perform(post("/api/benefits/activate")
                        .cookie(authCookie(memberInfo))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "applicationId": "APP-benefits-sync",
                                  "cardType": "huixuan_card",
                                  "token": "joint-token-benefits-sync"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("activated"));

        assertThat(benefitOrderRepository.selectCount(Wrappers.emptyWrapper())).isEqualTo(1);
        ArgumentCaptor<BenefitOrderSyncRequest> syncCaptor = ArgumentCaptor.forClass(BenefitOrderSyncRequest.class);
        verify(xiaohuaGatewayService).syncBenefitOrder(any(), any(), syncCaptor.capture());
        assertThat(syncCaptor.getValue().benefitUrl()).isNull();
        verify(benefitRedirectUrlService, never()).generate(any());
    }

    private BenefitProduct createProduct() {
        BenefitProduct product = new BenefitProduct();
        product.setProductCode("HUXUAN_CARD");
        product.setProductName("惠选卡");
        product.setFeeRate(300);
        product.setStatus("ACTIVE");
        product.setCreatedTs(LocalDateTime.now());
        product.setUpdatedTs(LocalDateTime.now());
        benefitProductRepository.insert(product);
        return product;
    }

    private MemberInfo createReadyMember(String memberId, String externalUserId) {
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

        MemberChannel memberChannel = new MemberChannel();
        memberChannel.setMemberId(memberId);
        memberChannel.setChannelCode("KJ");
        memberChannel.setExternalUserId(externalUserId);
        memberChannel.setBindStatus("BOUND");
        memberChannel.setCreatedTs(LocalDateTime.now());
        memberChannel.setUpdatedTs(LocalDateTime.now());
        memberChannelRepository.insert(memberChannel);

        memberPaymentProtocolRepository.insert(activeProtocol(memberId, externalUserId, "ALLINPAY", "AIP-" + memberId, null));
        memberPaymentProtocolRepository.insert(activeProtocol(memberId, externalUserId, "QW_SIGN", "QW-" + memberId, "1234"));
        return memberInfo;
    }

    private MemberPaymentProtocol activeProtocol(
            String memberId,
            String externalUserId,
            String providerCode,
            String protocolNo,
            String signRequestNo
    ) {
        MemberPaymentProtocol protocol = new MemberPaymentProtocol();
        protocol.setMemberId(memberId);
        protocol.setExternalUserId(externalUserId);
        protocol.setProviderCode(providerCode);
        protocol.setProtocolNo(protocolNo);
        protocol.setSignRequestNo(signRequestNo);
        protocol.setProtocolStatus("ACTIVE");
        protocol.setChannelCode("KJ");
        protocol.setSignedTs(LocalDateTime.now());
        protocol.setLastVerifiedTs(LocalDateTime.now());
        protocol.setCreatedTs(LocalDateTime.now());
        protocol.setUpdatedTs(LocalDateTime.now());
        return protocol;
    }

    private Cookie authCookie(MemberInfo memberInfo) {
        return new Cookie("NEXUSFIN_AUTH",
                jwtUtil.generateToken(memberInfo.getMemberId(), memberInfo.getExternalUserId()));
    }
}
