package com.nexusfin.equity.service;

import com.nexusfin.equity.dto.request.CreateBenefitOrderRequest;
import com.nexusfin.equity.dto.response.CreateBenefitOrderResponse;
import com.nexusfin.equity.dto.response.ProductPageResponse;
import com.nexusfin.equity.config.QwProperties;
import com.nexusfin.equity.entity.BenefitOrder;
import com.nexusfin.equity.entity.BenefitProduct;
import com.nexusfin.equity.entity.MemberChannel;
import com.nexusfin.equity.entity.MemberInfo;
import com.nexusfin.equity.enums.BenefitOrderStatusEnum;
import com.nexusfin.equity.exception.BizException;
import com.nexusfin.equity.exception.UpstreamTimeoutException;
import com.nexusfin.equity.repository.BenefitOrderRepository;
import com.nexusfin.equity.repository.BenefitProductRepository;
import com.nexusfin.equity.repository.MemberChannelRepository;
import com.nexusfin.equity.repository.MemberInfoRepository;
import com.nexusfin.equity.service.impl.BenefitOrderServiceImpl;
import com.nexusfin.equity.thirdparty.qw.QwBenefitClient;
import com.nexusfin.equity.thirdparty.qw.QwExerciseUrlResponse;
import com.nexusfin.equity.thirdparty.qw.QwMemberSyncResponse;
import com.nexusfin.equity.thirdparty.qw.QwMemberSyncRequest;
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
import static org.mockito.Mockito.never;

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

    @Mock
    private PaymentProtocolService paymentProtocolService;

    @Mock
    private AsyncCompensationEnqueueService asyncCompensationEnqueueService;

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
        product.setProductName("权益产品");
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
        when(paymentProtocolService.resolveForBenefitOrder(any(BenefitOrder.class)))
                .thenReturn(new PaymentProtocolService.ResolvedPaymentProtocol("AIP-REAL-001", "ALLINPAY"));
        when(qwBenefitClient.syncMemberOrder(any())).thenReturn(new QwMemberSyncResponse(
                "qw-order-1", "card-1", "1710000000000", 0, "P-2", "权益产品", "independence",
                "2026-03-26 12:00:00", "2027-03-26 12:00:00"
        ));

        CreateBenefitOrderResponse response = benefitOrderService.createOrder(
                "mem-2",
                new CreateBenefitOrderRequest("req-order-1", "P-2", 680000L, true)
        );

        ArgumentCaptor<BenefitOrder> captor = ArgumentCaptor.forClass(BenefitOrder.class);
        ArgumentCaptor<QwMemberSyncRequest> syncRequestCaptor = ArgumentCaptor.forClass(QwMemberSyncRequest.class);
        verify(benefitOrderRepository).insert(captor.capture());
        verify(agreementService).ensureAgreementArtifacts(captor.getValue());
        verify(qwBenefitClient).syncMemberOrder(syncRequestCaptor.capture());

        assertThat(response.orderStatus()).isEqualTo("FIRST_DEDUCT_PENDING");
        assertThat(captor.getValue().getProductCode()).isEqualTo("P-2");
        assertThat(captor.getValue().getMemberId()).isEqualTo("mem-2");
        assertThat(captor.getValue().getRequestId()).isEqualTo("req-order-1");
        assertThat(captor.getValue().getPayProtocolNoSnapshot()).isEqualTo("AIP-REAL-001");
        assertThat(captor.getValue().getPayProtocolSource()).isEqualTo("ALLINPAY");
        assertThat(syncRequestCaptor.getValue().payProtocolNo()).isEqualTo("AIP-REAL-001");
        verify(idempotencyService).markProcessed("req-order-1", "CREATE_ORDER", captor.getValue().getBenefitOrderNo(), "FIRST_DEDUCT_PENDING");
    }

    @Test
    void shouldUseResolvedTestOverrideProtocolForQwSync() {
        BenefitProduct product = new BenefitProduct();
        product.setProductCode("P-2");
        product.setProductName("权益产品");
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
        when(idempotencyService.isProcessed("req-order-override")).thenReturn(false);
        when(sensitiveDataCipher.decrypt("mobile-cipher")).thenReturn("13800138000");
        when(sensitiveDataCipher.decrypt("name-cipher")).thenReturn("张三");
        when(paymentProtocolService.resolveForBenefitOrder(any(BenefitOrder.class)))
                .thenReturn(new PaymentProtocolService.ResolvedPaymentProtocol("AIP211926033187CF73483", "TEST_OVERRIDE"));
        when(qwBenefitClient.syncMemberOrder(any())).thenReturn(new QwMemberSyncResponse(
                "qw-order-1", "card-1", "1710000000000", 0, "P-2", "权益产品", "independence",
                "2026-03-26 12:00:00", "2027-03-26 12:00:00"
        ));

        benefitOrderService.createOrder(
                "mem-2",
                new CreateBenefitOrderRequest("req-order-override", "P-2", 680000L, true)
        );

        ArgumentCaptor<BenefitOrder> orderCaptor = ArgumentCaptor.forClass(BenefitOrder.class);
        ArgumentCaptor<QwMemberSyncRequest> requestCaptor = ArgumentCaptor.forClass(QwMemberSyncRequest.class);
        verify(benefitOrderRepository).insert(orderCaptor.capture());
        verify(qwBenefitClient).syncMemberOrder(requestCaptor.capture());
        assertThat(orderCaptor.getValue().getPayProtocolNoSnapshot()).isEqualTo("AIP211926033187CF73483");
        assertThat(orderCaptor.getValue().getPayProtocolSource()).isEqualTo("TEST_OVERRIDE");
        assertThat(requestCaptor.getValue().payProtocolNo()).isEqualTo("AIP211926033187CF73483");
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
    void shouldReplayExistingOrderWhenRequestIdExistsButIdempotencyRecordMissing() {
        BenefitOrder existingOrder = new BenefitOrder();
        existingOrder.setBenefitOrderNo("ord-timeout-existing");
        existingOrder.setOrderStatus("FIRST_DEDUCT_PENDING");
        existingOrder.setSyncStatus(BenefitOrderStatusEnum.SYNC_FAIL.name());
        when(benefitOrderRepository.selectOne(any())).thenReturn(existingOrder);

        CreateBenefitOrderResponse response = benefitOrderService.createOrder(
                "mem-2",
                new CreateBenefitOrderRequest("req-order-timeout-retry", "P-2", 680000L, true)
        );

        assertThat(response.benefitOrderNo()).isEqualTo("ord-timeout-existing");
        assertThat(response.orderStatus()).isEqualTo("FIRST_DEDUCT_PENDING");
        verify(benefitOrderRepository, never()).insert(any());
        verify(qwBenefitClient, never()).syncMemberOrder(any());
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

    @Test
    void shouldEnqueueCompensationWhenQwMemberSyncTimesOut() {
        BenefitProduct product = new BenefitProduct();
        product.setProductCode("P-4");
        product.setProductName("权益产品");
        product.setStatus("ACTIVE");
        MemberInfo memberInfo = new MemberInfo();
        memberInfo.setMemberId("mem-4");
        memberInfo.setMobileEncrypted("mobile-cipher");
        memberInfo.setRealNameEncrypted("name-cipher");
        MemberChannel memberChannel = new MemberChannel();
        memberChannel.setChannelCode("KJ");
        memberChannel.setExternalUserId("user-4");
        when(benefitProductRepository.selectById("P-4")).thenReturn(product);
        when(memberInfoRepository.selectById("mem-4")).thenReturn(memberInfo);
        when(memberChannelRepository.selectOne(any())).thenReturn(memberChannel);
        when(idempotencyService.isProcessed("req-order-timeout")).thenReturn(false);
        when(sensitiveDataCipher.decrypt("mobile-cipher")).thenReturn("13800138000");
        when(sensitiveDataCipher.decrypt("name-cipher")).thenReturn("张三");
        when(paymentProtocolService.resolveForBenefitOrder(any(BenefitOrder.class)))
                .thenReturn(new PaymentProtocolService.ResolvedPaymentProtocol("AIP-REAL-004", "ALLINPAY"));
        when(qwBenefitClient.syncMemberOrder(any()))
                .thenThrow(new UpstreamTimeoutException("QW member sync timeout"));

        assertThatThrownBy(() -> benefitOrderService.createOrder(
                "mem-4",
                new CreateBenefitOrderRequest("req-order-timeout", "P-4", 680000L, true)
        )).isInstanceOf(BizException.class)
                .hasMessageContaining("QW_SYNC_TIMEOUT");

        verify(asyncCompensationEnqueueService).enqueue(any());
    }
}
