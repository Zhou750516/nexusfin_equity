package com.nexusfin.equity.service;

import com.nexusfin.equity.dto.request.CreateBenefitOrderRequest;
import com.nexusfin.equity.dto.response.CreateBenefitOrderResponse;
import com.nexusfin.equity.dto.response.ProductPageResponse;
import com.nexusfin.equity.config.QwProperties;
import com.nexusfin.equity.entity.BenefitOrder;
import com.nexusfin.equity.entity.BenefitProduct;
import com.nexusfin.equity.entity.MemberChannel;
import com.nexusfin.equity.entity.MemberInfo;
import com.nexusfin.equity.exception.BizException;
import com.nexusfin.equity.repository.BenefitOrderRepository;
import com.nexusfin.equity.repository.BenefitProductRepository;
import com.nexusfin.equity.repository.MemberChannelRepository;
import com.nexusfin.equity.repository.MemberInfoRepository;
import com.nexusfin.equity.service.impl.BenefitOrderServiceImpl;
import com.nexusfin.equity.thirdparty.qw.QwBenefitClient;
import com.nexusfin.equity.thirdparty.qw.QwExerciseUrlResponse;
import com.nexusfin.equity.thirdparty.qw.QwMemberSyncResponse;
import com.nexusfin.equity.util.SensitiveDataCipher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BenefitOrderServiceTest {

    @Mock
    private BenefitProductRepository benefitProductRepository;

    @Mock
    private BenefitOrderRepository benefitOrderRepository;

    @Mock
    private MemberInfoRepository memberInfoRepository;

    @Mock
    private MemberChannelRepository memberChannelRepository;

    @Mock
    private AgreementService agreementService;

    @Mock
    private IdempotencyService idempotencyService;

    @Mock
    private SensitiveDataCipher sensitiveDataCipher;

    @Mock
    private QwBenefitClient qwBenefitClient;

    @Mock
    private QwProperties qwProperties;

    @InjectMocks
    private BenefitOrderServiceImpl benefitOrderService;

    @Test
    void shouldReturnProductPageWithMemberInfo() {
        BenefitProduct product = new BenefitProduct();
        product.setProductCode("P-1");
        product.setProductName("权益产品");
        product.setFeeRate(299);
        product.setStatus("ACTIVE");
        MemberInfo memberInfo = new MemberInfo();
        memberInfo.setMemberId("mem-1");
        memberInfo.setExternalUserId("user-1");
        when(benefitProductRepository.selectById("P-1")).thenReturn(product);
        when(memberInfoRepository.selectById("mem-1")).thenReturn(memberInfo);

        ProductPageResponse response = benefitOrderService.getProductPage("P-1", "mem-1");

        assertThat(response.productCode()).isEqualTo("P-1");
        assertThat(response.memberId()).isEqualTo("mem-1");
        assertThat(response.externalUserId()).isEqualTo("user-1");
    }

    @Test
    void shouldCreateOrderAndEnsureAgreementArtifacts() {
        BenefitProduct product = new BenefitProduct();
        product.setProductCode("P-2");
        product.setStatus("ACTIVE");
        MemberInfo memberInfo = new MemberInfo();
        memberInfo.setMemberId("mem-2");
        memberInfo.setMobileEncrypted("mobile-cipher");
        memberInfo.setRealNameEncrypted("name-cipher");
        MemberChannel memberChannel = new MemberChannel();
        memberChannel.setChannelCode("KJ");
        memberChannel.setExternalUserId("user-2");
        when(benefitProductRepository.selectById("P-2")).thenReturn(product);
        when(memberInfoRepository.selectById("mem-2")).thenReturn(memberInfo);
        when(memberChannelRepository.selectOne(any())).thenReturn(memberChannel);
        when(idempotencyService.isProcessed("req-order-1")).thenReturn(false);
        when(sensitiveDataCipher.decrypt("mobile-cipher")).thenReturn("13800138000");
        when(sensitiveDataCipher.decrypt("name-cipher")).thenReturn("张三");
        when(qwBenefitClient.syncMemberOrder(any())).thenReturn(new QwMemberSyncResponse(
                "qw-order-1", "card-1", "1710000000000", 0, "P-2", "权益产品", "independence",
                "2026-03-26 12:00:00", "2027-03-26 12:00:00"
        ));

        CreateBenefitOrderResponse response = benefitOrderService.createOrder(
                "mem-2",
                new CreateBenefitOrderRequest("req-order-1", "P-2", 680000L, true)
        );

        ArgumentCaptor<BenefitOrder> captor = ArgumentCaptor.forClass(BenefitOrder.class);
        verify(benefitOrderRepository).insert(captor.capture());
        verify(agreementService).ensureAgreementArtifacts(captor.getValue());

        assertThat(response.orderStatus()).isEqualTo("FIRST_DEDUCT_PENDING");
        assertThat(captor.getValue().getProductCode()).isEqualTo("P-2");
        assertThat(captor.getValue().getMemberId()).isEqualTo("mem-2");
        assertThat(captor.getValue().getRequestId()).isEqualTo("req-order-1");
        verify(qwBenefitClient).syncMemberOrder(any());
        verify(idempotencyService).markProcessed("req-order-1", "CREATE_ORDER", captor.getValue().getBenefitOrderNo(), "FIRST_DEDUCT_PENDING");
    }

    @Test
    void shouldReplayExistingOrderForDuplicateCreateRequest() {
        BenefitOrder existingOrder = new BenefitOrder();
        existingOrder.setBenefitOrderNo("ord-duplicate-1");
        existingOrder.setOrderStatus("FIRST_DEDUCT_PENDING");
        com.nexusfin.equity.entity.IdempotencyRecord idempotencyRecord = new com.nexusfin.equity.entity.IdempotencyRecord();
        idempotencyRecord.setBizKey(existingOrder.getBenefitOrderNo());
        when(idempotencyService.isProcessed("req-order-duplicate")).thenReturn(true);
        when(idempotencyService.getByRequestId("req-order-duplicate")).thenReturn(idempotencyRecord);
        when(benefitOrderRepository.selectById(existingOrder.getBenefitOrderNo())).thenReturn(existingOrder);

        CreateBenefitOrderResponse response = benefitOrderService.createOrder(
                "mem-2",
                new CreateBenefitOrderRequest("req-order-duplicate", "P-2", 680000L, true)
        );

        assertThat(response.benefitOrderNo()).isEqualTo(existingOrder.getBenefitOrderNo());
        assertThat(response.orderStatus()).isEqualTo("FIRST_DEDUCT_PENDING");
    }

    @Test
    void shouldRejectOrderCreationWhenProductMissing() {
        when(benefitProductRepository.selectById("P-3")).thenReturn(null);
        when(idempotencyService.isProcessed("req-order-3")).thenReturn(false);

        assertThatThrownBy(() -> benefitOrderService.createOrder(
                "mem-3",
                new CreateBenefitOrderRequest("req-order-3", "P-3", 680000L, true)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("PRODUCT_NOT_FOUND");
    }

    @Test
    void shouldGetExerciseUrlFromQwClient() {
        BenefitOrder order = new BenefitOrder();
        order.setBenefitOrderNo("ord-ex-1");
        order.setExternalUserId("user-ex-1");
        when(benefitOrderRepository.selectById("ord-ex-1")).thenReturn(order);
        when(qwBenefitClient.getExerciseUrl(any())).thenReturn(new QwExerciseUrlResponse(
                0,
                "https://mock-qw.test/exercise?partnerOrderNo=ord-ex-1",
                "token-1",
                "2026-03-26 12:00:00",
                "2027-03-26 12:00:00"
        ));

        com.nexusfin.equity.dto.response.ExerciseUrlResponse response = benefitOrderService.getExerciseUrl("ord-ex-1");

        assertThat(response.exerciseUrl()).contains("partnerOrderNo=ord-ex-1");
        assertThat(response.expireTime()).isEqualTo("2027-03-26 12:00:00");
    }
}
