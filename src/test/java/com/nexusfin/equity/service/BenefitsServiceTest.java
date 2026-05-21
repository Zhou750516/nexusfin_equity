package com.nexusfin.equity.service;

import com.nexusfin.equity.config.H5BenefitsProperties;
import com.nexusfin.equity.config.H5LoanProperties;
import com.nexusfin.equity.dto.request.BenefitsActivateRequest;
import com.nexusfin.equity.dto.request.CreateBenefitOrderRequest;
import com.nexusfin.equity.dto.response.BenefitsActivateResponse;
import com.nexusfin.equity.dto.response.BenefitsCardDetailResponse;
import com.nexusfin.equity.dto.response.CreateBenefitOrderResponse;
import com.nexusfin.equity.entity.BenefitProduct;
import com.nexusfin.equity.entity.MemberReceivingAccount;
import com.nexusfin.equity.exception.BizException;
import com.nexusfin.equity.repository.BenefitProductRepository;
import com.nexusfin.equity.repository.MemberReceivingAccountRepository;
import com.nexusfin.equity.service.impl.BenefitsServiceImpl;
import com.nexusfin.equity.thirdparty.yunka.BenefitOrderSyncRequest;
import com.nexusfin.equity.thirdparty.yunka.BenefitOrderSyncResponse;
import com.nexusfin.equity.thirdparty.yunka.ProtocolLink;
import com.nexusfin.equity.thirdparty.yunka.ProtocolQueryRequest;
import com.nexusfin.equity.thirdparty.yunka.ProtocolQueryResponse;
import com.nexusfin.equity.thirdparty.yunka.UserCardListResponse;
import com.nexusfin.equity.thirdparty.yunka.UserCardListRequest;
import com.nexusfin.equity.thirdparty.yunka.UserCardSummary;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BenefitsServiceTest {

    @Mock
    private BenefitProductRepository benefitProductRepository;

    @Mock
    private MemberReceivingAccountRepository memberReceivingAccountRepository;

    @Mock
    private BenefitOrderService benefitOrderService;

    @Mock
    private H5I18nService h5I18nService;

    @Mock
    private XiaohuaGatewayService xiaohuaGatewayService;

    @Mock
    private BenefitRedirectUrlService benefitRedirectUrlService;

    private BenefitsService benefitsService;

    @BeforeEach
    void setUp() {
        when(h5I18nService.text(any(), any())).thenAnswer(invocation -> invocation.getArgument(1));
        benefitsService = new BenefitsServiceImpl(
                h5BenefitsProperties(),
                h5LoanProperties(),
                benefitProductRepository,
                memberReceivingAccountRepository,
                benefitOrderService,
                h5I18nService,
                xiaohuaGatewayService,
                benefitRedirectUrlService
        );
    }

    @Test
    void shouldMergeDynamicProtocolsAndUserCardsIntoCardDetail() {
        when(benefitProductRepository.selectById("HUXUAN_CARD")).thenReturn(activeProduct());
        when(xiaohuaGatewayService.queryProtocols(any(), eq("benefits-card-detail"), any()))
                .thenReturn(new ProtocolQueryResponse(List.of(
                        new ProtocolLink("借款协议", 1, "https://agreements/loan")
                )));
        when(xiaohuaGatewayService.queryUserCards(any(), eq("benefits-card-detail"), any()))
                .thenReturn(new UserCardListResponse(List.of(
                        new UserCardSummary("card-001", "招商银行", "8648", 1)
                )));
        BenefitsCardDetailResponse response = benefitsService.getCardDetail("mem-test-001", "cid-test-001");

        assertThat(response.protocols()).extracting(BenefitsCardDetailResponse.ProtocolLink::url)
                .contains("https://agreements/loan");
        assertThat(response.userCards()).hasSize(1);
        assertThat(response.userCards().get(0).bankName()).isEqualTo("招商银行");
        assertThat(response.protocolReady()).isTrue();

        ArgumentCaptor<ProtocolQueryRequest> protocolCaptor = ArgumentCaptor.forClass(ProtocolQueryRequest.class);
        verify(xiaohuaGatewayService).queryProtocols(any(), eq("benefits-card-detail"), protocolCaptor.capture());
        assertThat(protocolCaptor.getValue().userId()).isEqualTo("mem-test-001");
        assertThat(protocolCaptor.getValue().userId()).isNotEqualTo("cid-test-001");

        ArgumentCaptor<UserCardListRequest> cardCaptor = ArgumentCaptor.forClass(UserCardListRequest.class);
        verify(xiaohuaGatewayService).queryUserCards(any(), eq("benefits-card-detail"), cardCaptor.capture());
        assertThat(cardCaptor.getValue().userId()).isEqualTo("mem-test-001");
        assertThat(cardCaptor.getValue().userId()).isNotEqualTo("cid-test-001");
        verify(memberReceivingAccountRepository, never()).selectActiveByMemberId(any());
    }

    @Test
    void shouldUseLocalReceivingAccountsForCardDetailWhenEnabled() {
        BenefitsService localCardService = benefitsServiceWithLocalReceivingAccount(true);
        when(benefitProductRepository.selectById("HUXUAN_CARD")).thenReturn(activeProduct());
        when(xiaohuaGatewayService.queryProtocols(any(), eq("benefits-card-detail"), any()))
                .thenReturn(dynamicProtocolResponse());
        when(memberReceivingAccountRepository.selectActiveByMemberId("mem-local-card"))
                .thenReturn(List.of(
                        receivingAccount("mem-local-card", "old-card-001", "旧用户卡", "7568", 0),
                        receivingAccount("mem-local-card", "622908328976881119", "齐为联调测试卡", "8119", 1)
                ));

        BenefitsCardDetailResponse response = localCardService.getCardDetail("mem-local-card", "cid-local-card");

        assertThat(response.userCards()).hasSize(2);
        assertThat(response.userCards().get(0).cardId()).isEqualTo("622908328976881119");
        assertThat(response.userCards().get(0).bankName()).isEqualTo("齐为联调测试卡");
        assertThat(response.userCards().get(0).cardLastFour()).isEqualTo("8119");
        assertThat(response.userCards().get(0).defaultCard()).isTrue();
        assertThat(response.userCards().get(1).cardLastFour()).isEqualTo("7568");
        verify(xiaohuaGatewayService, never()).queryUserCards(any(), any(), any());
        assertThat(response.protocolReady()).isTrue();
    }

    @Test
    void shouldFallbackToXiaohuaCardsWhenLocalReceivingAccountEnabledButEmpty() {
        BenefitsService localCardService = benefitsServiceWithLocalReceivingAccount(true);
        when(benefitProductRepository.selectById("HUXUAN_CARD")).thenReturn(activeProduct());
        when(xiaohuaGatewayService.queryProtocols(any(), eq("benefits-card-detail"), any()))
                .thenReturn(dynamicProtocolResponse());
        when(memberReceivingAccountRepository.selectActiveByMemberId("mem-local-empty"))
                .thenReturn(List.of());
        when(xiaohuaGatewayService.queryUserCards(any(), eq("benefits-card-detail"), any()))
                .thenReturn(new UserCardListResponse(List.of(
                        new UserCardSummary("card-remote-001", "招商银行", "7568", 1)
                )));

        BenefitsCardDetailResponse response = localCardService.getCardDetail("mem-local-empty", "cid-local-empty");

        assertThat(response.userCards()).hasSize(1);
        assertThat(response.userCards().get(0).cardId()).isEqualTo("card-remote-001");
        assertThat(response.userCards().get(0).cardLastFour()).isEqualTo("7568");
        assertThat(response.userCards().get(0).defaultCard()).isTrue();
        verify(xiaohuaGatewayService).queryUserCards(any(), eq("benefits-card-detail"), any());
    }

    @Test
    void shouldBlockActivationWhenProtocolIsNotReady() {
        when(benefitProductRepository.selectById("HUXUAN_CARD")).thenReturn(activeProduct());
        when(xiaohuaGatewayService.queryProtocols(any(), eq("benefits-card-detail"), any()))
                .thenReturn(new ProtocolQueryResponse(List.of()));
        when(xiaohuaGatewayService.queryUserCards(any(), eq("benefits-card-detail"), any()))
                .thenReturn(new UserCardListResponse(List.of()));

        assertThatThrownBy(() -> benefitsService.activate(
                "mem-test-001",
                "cid-test-001",
                new BenefitsActivateRequest("APP-001", "huixuan_card", "joint-token-benefits-block")
        )).isInstanceOf(BizException.class)
                .extracting(ex -> ((BizException) ex).getErrorNo())
                .isEqualTo("BENEFITS_PROTOCOL_NOT_READY");
    }

    @Test
    void shouldSyncBenefitOrderAfterActivationWithoutRedirectUrl() {
        when(benefitProductRepository.selectById("HUXUAN_CARD")).thenReturn(activeProduct());
        when(xiaohuaGatewayService.queryProtocols(any(), eq("benefits-card-detail"), any()))
                .thenReturn(new ProtocolQueryResponse(List.of(
                        new ProtocolLink("借款协议", 1, "https://agreements/loan")
                )));
        when(xiaohuaGatewayService.queryUserCards(any(), eq("benefits-card-detail"), any()))
                .thenReturn(new UserCardListResponse(List.of(
                        new UserCardSummary("card-001", "招商银行", "8648", 1)
                )));
        when(benefitOrderService.createOrder(eq("mem-test-001"), any(CreateBenefitOrderRequest.class)))
                .thenReturn(new CreateBenefitOrderResponse(
                        "ord-001",
                        "FIRST_DEDUCT_PENDING",
                        "/h5/equity/orders/ord-001",
                        "QW-ORDER-001",
                        1779335976232L,
                        1779335976232L,
                        1781927976232L
                ));
        when(xiaohuaGatewayService.syncBenefitOrder(any(), eq("ord-001"), any()))
                .thenReturn(new BenefitOrderSyncResponse("SUCCESS", "ok"));

        BenefitsActivateResponse response = benefitsService.activate(
                "mem-test-001",
                "cid-test-001",
                new BenefitsActivateRequest("APP-001", "huixuan_card", "joint-token-benefits-001")
        );

        assertThat(response.activationId()).isEqualTo("ord-001");
        ArgumentCaptor<CreateBenefitOrderRequest> orderCaptor = ArgumentCaptor.forClass(CreateBenefitOrderRequest.class);
        verify(benefitOrderService).createOrder(eq("mem-test-001"), orderCaptor.capture());
        assertThat(orderCaptor.getValue().requestId()).isEqualTo("activate-APP-001");
        assertThat(orderCaptor.getValue().loanAmount()).isEqualTo(300000L);
        assertThat(orderCaptor.getValue().benefitAmount()).isEqualTo(30000L);
        ArgumentCaptor<BenefitOrderSyncRequest> syncCaptor = ArgumentCaptor.forClass(BenefitOrderSyncRequest.class);
        verify(xiaohuaGatewayService).syncBenefitOrder(any(), eq("ord-001"), syncCaptor.capture());
        assertThat(syncCaptor.getValue().platformBenefitOrderNo()).isEqualTo("APP-001");
        assertThat(syncCaptor.getValue().benefitOrderNo()).isEqualTo("QW-ORDER-001");
        assertThat(syncCaptor.getValue().orderAmount()).isEqualTo(30000L);
        assertThat(syncCaptor.getValue().status()).isEqualTo(2);
        assertThat(syncCaptor.getValue().createTime()).isEqualTo(1779335976232L);
        assertThat(syncCaptor.getValue().payTime()).isEqualTo(1779335976232L);
        assertThat(syncCaptor.getValue().expireTime()).isEqualTo(1781927976232L);
        assertThat(syncCaptor.getValue().memberPayType()).isEqualTo("QW");
        assertThat(syncCaptor.getValue().paymentNo()).isEqualTo("QW-ORDER-001");
        assertThat(syncCaptor.getValue().benefitServiceProvider()).isEqualTo("齐为");
        assertThat(syncCaptor.getValue().benefitUrl()).isEmpty();
        verify(benefitRedirectUrlService, never()).generate(any());
    }

    @Test
    void shouldNotCallBenefitRedirectUrlGenerationDuringActivation() {
        when(benefitProductRepository.selectById("HUXUAN_CARD")).thenReturn(activeProduct());
        when(xiaohuaGatewayService.queryProtocols(any(), eq("benefits-card-detail"), any()))
                .thenReturn(new ProtocolQueryResponse(List.of(
                        new ProtocolLink("借款协议", 1, "https://agreements/loan")
                )));
        when(xiaohuaGatewayService.queryUserCards(any(), eq("benefits-card-detail"), any()))
                .thenReturn(new UserCardListResponse(List.of(
                        new UserCardSummary("card-001", "招商银行", "8648", 1)
                )));
        when(benefitOrderService.createOrder(eq("mem-test-001"), any(CreateBenefitOrderRequest.class)))
                .thenReturn(new CreateBenefitOrderResponse(
                        "ord-001",
                        "FIRST_DEDUCT_PENDING",
                        "/h5/equity/orders/ord-001",
                        "QW-ORDER-001",
                        1779335976232L,
                        1779335976232L,
                        1781927976232L
                ));
        when(xiaohuaGatewayService.syncBenefitOrder(any(), eq("ord-001"), any()))
                .thenReturn(new BenefitOrderSyncResponse("SUCCESS", "ok"));

        BenefitsActivateResponse response = benefitsService.activate(
                "mem-test-001",
                "cid-test-001",
                new BenefitsActivateRequest("APP-001", "huixuan_card", "joint-token-benefits-002")
        );

        assertThat(response.status()).isEqualTo("activated");
        verify(benefitRedirectUrlService, never()).generate(any());
        verify(xiaohuaGatewayService).syncBenefitOrder(any(), eq("ord-001"), any());
    }

    @Test
    void shouldNotSyncYunkaWhenQwSignIsRequiredBeforeCreateOrder() {
        when(benefitProductRepository.selectById("HUXUAN_CARD")).thenReturn(activeProduct());
        when(xiaohuaGatewayService.queryProtocols(any(), eq("benefits-card-detail"), any()))
                .thenReturn(dynamicProtocolResponse());
        when(xiaohuaGatewayService.queryUserCards(any(), eq("benefits-card-detail"), any()))
                .thenReturn(userCardResponse());
        when(benefitOrderService.createOrder(eq("mem-test-001"), any(CreateBenefitOrderRequest.class)))
                .thenThrow(new BizException("QW_SIGN_REQUIRED", "QW sign confirmation required before benefit order"));

        assertThatThrownBy(() -> benefitsService.activate(
                "mem-test-001",
                "cid-test-001",
                new BenefitsActivateRequest("APP-001", "huixuan_card", "joint-token-benefits-sign-required")
        )).isInstanceOf(BizException.class)
                .extracting(ex -> ((BizException) ex).getErrorNo())
                .isEqualTo("QW_SIGN_REQUIRED");

        verify(benefitRedirectUrlService, never()).generate(any());
        verify(xiaohuaGatewayService, never()).syncBenefitOrder(any(), any(), any());
    }

    @Test
    void shouldNotSyncYunkaWhenQwOrderNoticeDataIsIncomplete() {
        when(benefitProductRepository.selectById("HUXUAN_CARD")).thenReturn(activeProduct());
        when(xiaohuaGatewayService.queryProtocols(any(), eq("benefits-card-detail"), any()))
                .thenReturn(dynamicProtocolResponse());
        when(xiaohuaGatewayService.queryUserCards(any(), eq("benefits-card-detail"), any()))
                .thenReturn(userCardResponse());
        when(benefitOrderService.createOrder(eq("mem-test-001"), any(CreateBenefitOrderRequest.class)))
                .thenReturn(new CreateBenefitOrderResponse(
                        "ord-001",
                        "FIRST_DEDUCT_PENDING",
                        "/h5/equity/orders/ord-001"
                ));

        assertThatThrownBy(() -> benefitsService.activate(
                "mem-test-001",
                "cid-test-001",
                new BenefitsActivateRequest("APP-001", "huixuan_card", "joint-token-benefits-incomplete")
        )).isInstanceOf(BizException.class)
                .extracting(ex -> ((BizException) ex).getErrorNo())
                .isEqualTo("QW_ORDER_NOTICE_DATA_INCOMPLETE");

        verify(xiaohuaGatewayService, never()).syncBenefitOrder(any(), any(), any());
    }

    @Test
    void shouldTreatProtocolLinksAsProtocolReadyBeforeQwSign() {
        when(benefitProductRepository.selectById("HUXUAN_CARD")).thenReturn(activeProduct());
        when(xiaohuaGatewayService.queryProtocols(any(), eq("benefits-card-detail"), any()))
                .thenReturn(dynamicProtocolResponse());
        when(xiaohuaGatewayService.queryUserCards(any(), eq("benefits-card-detail"), any()))
                .thenReturn(userCardResponse());

        BenefitsCardDetailResponse response = benefitsService.getCardDetail("mem-qw-ready", "cid-qw-ready");

        assertThat(response.protocolReady()).isTrue();
    }

    @Test
    void shouldNotRequireAllinpayProtocolForProtocolReady() {
        when(benefitProductRepository.selectById("HUXUAN_CARD")).thenReturn(activeProduct());
        when(xiaohuaGatewayService.queryProtocols(any(), eq("benefits-card-detail"), any()))
                .thenReturn(dynamicProtocolResponse());
        when(xiaohuaGatewayService.queryUserCards(any(), eq("benefits-card-detail"), any()))
                .thenReturn(userCardResponse());

        BenefitsCardDetailResponse response = benefitsService.getCardDetail("mem-allinpay-ready", "cid-allinpay-ready");

        assertThat(response.protocolReady()).isTrue();
    }

    @Test
    void shouldReturnProtocolReadyWhenLinksExistWithoutLocalSignedProtocol() {
        when(benefitProductRepository.selectById("HUXUAN_CARD")).thenReturn(activeProduct());
        when(xiaohuaGatewayService.queryProtocols(any(), eq("benefits-card-detail"), any()))
                .thenReturn(dynamicProtocolResponse());
        when(xiaohuaGatewayService.queryUserCards(any(), eq("benefits-card-detail"), any()))
                .thenReturn(userCardResponse());

        BenefitsCardDetailResponse response = benefitsService.getCardDetail("mem-no-protocol", "cid-no-protocol");

        assertThat(response.protocolReady()).isTrue();
    }

    @Test
    void shouldRequireDynamicProtocolsBeforeLocalProtocolReady() {
        when(benefitProductRepository.selectById("HUXUAN_CARD")).thenReturn(activeProduct());
        when(xiaohuaGatewayService.queryProtocols(any(), eq("benefits-card-detail"), any()))
                .thenReturn(new ProtocolQueryResponse(List.of()));
        when(xiaohuaGatewayService.queryUserCards(any(), eq("benefits-card-detail"), any()))
                .thenReturn(userCardResponse());
        BenefitsCardDetailResponse response = benefitsService.getCardDetail("mem-empty-protocols", "cid-empty-protocols");

        assertThat(response.protocolReady()).isFalse();
    }

    @Test
    void shouldAllowEmptyDynamicProtocolsWhenProtocolLinkRequirementIsDisabled() {
        BenefitsService protocolLinkOptionalService = benefitsServiceWithProtocolLinkRequired(false);
        when(benefitProductRepository.selectById("HUXUAN_CARD")).thenReturn(activeProduct());
        when(xiaohuaGatewayService.queryProtocols(any(), eq("benefits-card-detail"), any()))
                .thenReturn(new ProtocolQueryResponse(List.of()));
        when(xiaohuaGatewayService.queryUserCards(any(), eq("benefits-card-detail"), any()))
                .thenReturn(userCardResponse());
        BenefitsCardDetailResponse response = protocolLinkOptionalService.getCardDetail(
                "mem-optional-qw",
                "cid-optional-qw"
        );

        assertThat(response.protocolReady()).isTrue();
    }

    @Test
    void shouldAllowMissingActivePaymentProtocolWhenProtocolLinkRequirementIsDisabled() {
        BenefitsService protocolLinkOptionalService = benefitsServiceWithProtocolLinkRequired(false);
        when(benefitProductRepository.selectById("HUXUAN_CARD")).thenReturn(activeProduct());
        when(xiaohuaGatewayService.queryProtocols(any(), eq("benefits-card-detail"), any()))
                .thenReturn(new ProtocolQueryResponse(List.of()));
        when(xiaohuaGatewayService.queryUserCards(any(), eq("benefits-card-detail"), any()))
                .thenReturn(userCardResponse());

        BenefitsCardDetailResponse response = protocolLinkOptionalService.getCardDetail(
                "mem-optional-no-sign",
                "cid-optional-no-sign"
        );

        assertThat(response.protocolReady()).isTrue();
    }

    private BenefitsService benefitsServiceWithProtocolLinkRequired(boolean protocolLinkRequired) {
        return new BenefitsServiceImpl(
                h5BenefitsProperties(protocolLinkRequired),
                h5LoanProperties(),
                benefitProductRepository,
                memberReceivingAccountRepository,
                benefitOrderService,
                h5I18nService,
                xiaohuaGatewayService,
                benefitRedirectUrlService
        );
    }

    private BenefitsService benefitsServiceWithLocalReceivingAccount(boolean useLocalReceivingAccount) {
        return new BenefitsServiceImpl(
                h5BenefitsProperties(true, useLocalReceivingAccount),
                h5LoanProperties(),
                benefitProductRepository,
                memberReceivingAccountRepository,
                benefitOrderService,
                h5I18nService,
                xiaohuaGatewayService,
                benefitRedirectUrlService
        );
    }

    private BenefitProduct activeProduct() {
        BenefitProduct product = new BenefitProduct();
        product.setProductCode("HUXUAN_CARD");
        product.setStatus("ACTIVE");
        return product;
    }

    private ProtocolQueryResponse dynamicProtocolResponse() {
        return new ProtocolQueryResponse(List.of(
                new ProtocolLink("借款协议", 1, "https://agreements/loan")
        ));
    }

    private UserCardListResponse userCardResponse() {
        return new UserCardListResponse(List.of(
                new UserCardSummary("card-001", "招商银行", "8648", 1)
        ));
    }

    private H5BenefitsProperties h5BenefitsProperties() {
        return h5BenefitsProperties(true);
    }

    private H5BenefitsProperties h5BenefitsProperties(boolean protocolLinkRequired) {
        return h5BenefitsProperties(protocolLinkRequired, false);
    }

    private H5BenefitsProperties h5BenefitsProperties(
            boolean protocolLinkRequired,
            boolean useLocalReceivingAccount
    ) {
        return new H5BenefitsProperties(
                "HUXUAN_CARD",
                protocolLinkRequired,
                useLocalReceivingAccount,
                new H5BenefitsProperties.Activate(300000L, 30000L, "huixuan_card", "惠选卡开通成功"),
                new H5BenefitsProperties.Detail(
                        "惠选卡",
                        300L,
                        448L,
                        List.of(new H5BenefitsProperties.Feature("f1", "d1")),
                        List.of(new H5BenefitsProperties.Category(
                                "影音会员",
                                "tv",
                                List.of(new H5BenefitsProperties.Item("5折", "权益", "说明", "30天", 30L, 15L))
                        )),
                        List.of("tip"),
                        List.of(new H5BenefitsProperties.ProtocolLink("用户服务协议", "/protocols/user-service"))
                )
        );
    }

    private MemberReceivingAccount receivingAccount(
            String memberId,
            String accountId,
            String bankName,
            String lastFour,
            Integer isDefault
    ) {
        MemberReceivingAccount account = new MemberReceivingAccount();
        account.setMemberId(memberId);
        account.setAccountId(accountId);
        account.setBankName(bankName);
        account.setLastFour(lastFour);
        account.setAccountStatus("ACTIVE");
        account.setIsDefault(isDefault);
        return account;
    }

    private H5LoanProperties h5LoanProperties() {
        return new H5LoanProperties(
                new H5LoanProperties.AmountRange(100L, 5000L, 100L, 3000L),
                List.of(new H5LoanProperties.TermOption("3期", 3)),
                BigDecimal.valueOf(0.18),
                "XX商业银行"
        );
    }
}
