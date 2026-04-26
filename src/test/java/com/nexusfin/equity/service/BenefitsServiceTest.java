package com.nexusfin.equity.service;

import com.nexusfin.equity.config.H5BenefitsProperties;
import com.nexusfin.equity.config.H5LoanProperties;
import com.nexusfin.equity.dto.request.BenefitsActivateRequest;
import com.nexusfin.equity.dto.request.CreateBenefitOrderRequest;
import com.nexusfin.equity.dto.response.BenefitsActivateResponse;
import com.nexusfin.equity.dto.response.BenefitsCardDetailResponse;
import com.nexusfin.equity.dto.response.CreateBenefitOrderResponse;
import com.nexusfin.equity.entity.BenefitProduct;
import com.nexusfin.equity.entity.MemberPaymentProtocol;
import com.nexusfin.equity.exception.BizException;
import com.nexusfin.equity.repository.BenefitProductRepository;
import com.nexusfin.equity.repository.MemberPaymentProtocolRepository;
import com.nexusfin.equity.service.impl.BenefitsServiceImpl;
import com.nexusfin.equity.thirdparty.yunka.BenefitOrderSyncResponse;
import com.nexusfin.equity.thirdparty.yunka.ProtocolLink;
import com.nexusfin.equity.thirdparty.yunka.ProtocolQueryResponse;
import com.nexusfin.equity.thirdparty.yunka.UserCardListResponse;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BenefitsServiceTest {

    @Mock
    private BenefitProductRepository benefitProductRepository;

    @Mock
    private MemberPaymentProtocolRepository memberPaymentProtocolRepository;

    @Mock
    private BenefitOrderService benefitOrderService;

    @Mock
    private H5I18nService h5I18nService;

    @Mock
    private XiaohuaGatewayService xiaohuaGatewayService;

    private BenefitsService benefitsService;

    @BeforeEach
    void setUp() {
        when(h5I18nService.text(any(), any())).thenAnswer(invocation -> invocation.getArgument(1));
        benefitsService = new BenefitsServiceImpl(
                h5BenefitsProperties(),
                h5LoanProperties(),
                benefitProductRepository,
                memberPaymentProtocolRepository,
                benefitOrderService,
                h5I18nService,
                xiaohuaGatewayService
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
        when(memberPaymentProtocolRepository.selectOne(any())).thenReturn(activeProtocol());

        BenefitsCardDetailResponse response = benefitsService.getCardDetail("mem-001", "user-001");

        assertThat(response.protocols()).extracting(BenefitsCardDetailResponse.ProtocolLink::url)
                .contains("https://agreements/loan");
        assertThat(response.userCards()).hasSize(1);
        assertThat(response.userCards().get(0).bankName()).isEqualTo("招商银行");
        assertThat(response.protocolReady()).isTrue();
    }

    @Test
    void shouldBlockActivationWhenProtocolIsNotReady() {
        when(benefitProductRepository.selectById("HUXUAN_CARD")).thenReturn(activeProduct());
        when(xiaohuaGatewayService.queryProtocols(any(), eq("benefits-card-detail"), any()))
                .thenReturn(new ProtocolQueryResponse(List.of()));
        when(xiaohuaGatewayService.queryUserCards(any(), eq("benefits-card-detail"), any()))
                .thenReturn(new UserCardListResponse(List.of()));

        assertThatThrownBy(() -> benefitsService.activate(
                "mem-001",
                "user-001",
                new BenefitsActivateRequest("APP-001", "huixuan_card")
        )).isInstanceOf(BizException.class)
                .extracting(ex -> ((BizException) ex).getErrorNo())
                .isEqualTo("BENEFITS_PROTOCOL_NOT_READY");
    }

    @Test
    void shouldSyncBenefitOrderAfterActivation() {
        when(benefitProductRepository.selectById("HUXUAN_CARD")).thenReturn(activeProduct());
        when(xiaohuaGatewayService.queryProtocols(any(), eq("benefits-card-detail"), any()))
                .thenReturn(new ProtocolQueryResponse(List.of(
                        new ProtocolLink("借款协议", 1, "https://agreements/loan")
                )));
        when(xiaohuaGatewayService.queryUserCards(any(), eq("benefits-card-detail"), any()))
                .thenReturn(new UserCardListResponse(List.of(
                        new UserCardSummary("card-001", "招商银行", "8648", 1)
                )));
        when(memberPaymentProtocolRepository.selectOne(any())).thenReturn(activeProtocol());
        when(benefitOrderService.createOrder(eq("mem-001"), any(CreateBenefitOrderRequest.class)))
                .thenReturn(new CreateBenefitOrderResponse("ord-001", "FIRST_DEDUCT_PENDING", "/h5/equity/orders/ord-001"));
        when(xiaohuaGatewayService.syncBenefitOrder(any(), eq("ord-001"), any()))
                .thenReturn(new BenefitOrderSyncResponse("SUCCESS", "ok"));

        BenefitsActivateResponse response = benefitsService.activate(
                "mem-001",
                "user-001",
                new BenefitsActivateRequest("APP-001", "huixuan_card")
        );

        assertThat(response.activationId()).isEqualTo("ord-001");
        ArgumentCaptor<CreateBenefitOrderRequest> orderCaptor = ArgumentCaptor.forClass(CreateBenefitOrderRequest.class);
        verify(benefitOrderService).createOrder(eq("mem-001"), orderCaptor.capture());
        assertThat(orderCaptor.getValue().requestId()).isEqualTo("activate-APP-001");
        verify(xiaohuaGatewayService).syncBenefitOrder(any(), eq("ord-001"), any());
    }

    private BenefitProduct activeProduct() {
        BenefitProduct product = new BenefitProduct();
        product.setProductCode("HUXUAN_CARD");
        product.setStatus("ACTIVE");
        return product;
    }

    private MemberPaymentProtocol activeProtocol() {
        MemberPaymentProtocol protocol = new MemberPaymentProtocol();
        protocol.setProtocolNo("AIP-001");
        protocol.setProtocolStatus("ACTIVE");
        return protocol;
    }

    private H5BenefitsProperties h5BenefitsProperties() {
        return new H5BenefitsProperties(
                "HUXUAN_CARD",
                new H5BenefitsProperties.Activate(30000L, "huixuan_card", "惠选卡开通成功"),
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

    private H5LoanProperties h5LoanProperties() {
        return new H5LoanProperties(
                new H5LoanProperties.AmountRange(100L, 5000L, 100L, 3000L),
                List.of(new H5LoanProperties.TermOption("3期", 3)),
                BigDecimal.valueOf(0.18),
                "XX商业银行",
                new H5LoanProperties.ReceivingAccount("招商银行", "8648", "acc_001")
        );
    }
}
