package com.nexusfin.equity.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nexusfin.equity.dto.request.CreateBenefitOrderRequest;
import com.nexusfin.equity.dto.response.BenefitOrderStatusResponse;
import com.nexusfin.equity.dto.response.CreateBenefitOrderResponse;
import com.nexusfin.equity.dto.response.ExerciseUrlResponse;
import com.nexusfin.equity.dto.response.ProductPageResponse;
import com.nexusfin.equity.entity.BenefitOrder;
import com.nexusfin.equity.entity.BenefitProduct;
import com.nexusfin.equity.entity.MemberChannel;
import com.nexusfin.equity.entity.MemberInfo;
import com.nexusfin.equity.enums.BenefitOrderStatusEnum;
import com.nexusfin.equity.enums.PaymentStatusEnum;
import com.nexusfin.equity.exception.BenefitPurchaseSyncTimeoutCompensationException;
import com.nexusfin.equity.exception.BizException;
import com.nexusfin.equity.exception.UpstreamTimeoutException;
import com.nexusfin.equity.repository.BenefitOrderRepository;
import com.nexusfin.equity.repository.BenefitProductRepository;
import com.nexusfin.equity.repository.MemberChannelRepository;
import com.nexusfin.equity.repository.MemberInfoRepository;
import com.nexusfin.equity.service.AgreementService;
import com.nexusfin.equity.service.AsyncCompensationEnqueueService;
import com.nexusfin.equity.service.AsyncCompensationEnqueuePayload;
import com.nexusfin.equity.service.BenefitOrderService;
import com.nexusfin.equity.service.IdempotencyService;
import com.nexusfin.equity.service.PaymentProtocolService;
import com.nexusfin.equity.thirdparty.qw.QwBenefitClient;
import com.nexusfin.equity.thirdparty.qw.QwExerciseUrlRequest;
import com.nexusfin.equity.thirdparty.qw.QwExerciseUrlResponse;
import com.nexusfin.equity.thirdparty.qw.QwMemberSyncRequest;
import com.nexusfin.equity.thirdparty.qw.QwMemberSyncResponse;
import com.nexusfin.equity.util.OrderStateMachine;
import com.nexusfin.equity.util.RequestIdUtil;
import com.nexusfin.equity.util.SensitiveDataCipher;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BenefitOrderServiceImpl implements BenefitOrderService {

    private static final Logger log = LoggerFactory.getLogger(BenefitOrderServiceImpl.class);

    private final BenefitProductRepository benefitProductRepository;
    private final BenefitOrderRepository benefitOrderRepository;
    private final MemberInfoRepository memberInfoRepository;
    private final MemberChannelRepository memberChannelRepository;
    private final AgreementService agreementService;
    private final IdempotencyService idempotencyService;
    private final PaymentProtocolService paymentProtocolService;
    private final SensitiveDataCipher sensitiveDataCipher;
    private final QwBenefitClient qwBenefitClient;
    private final AsyncCompensationEnqueueService asyncCompensationEnqueueService;

    public BenefitOrderServiceImpl(
            BenefitProductRepository benefitProductRepository,
            BenefitOrderRepository benefitOrderRepository,
            MemberInfoRepository memberInfoRepository,
            MemberChannelRepository memberChannelRepository,
            AgreementService agreementService,
            IdempotencyService idempotencyService,
            PaymentProtocolService paymentProtocolService,
            SensitiveDataCipher sensitiveDataCipher,
            QwBenefitClient qwBenefitClient,
            AsyncCompensationEnqueueService asyncCompensationEnqueueService
    ) {
        this.benefitProductRepository = benefitProductRepository;
        this.benefitOrderRepository = benefitOrderRepository;
        this.memberInfoRepository = memberInfoRepository;
        this.memberChannelRepository = memberChannelRepository;
        this.agreementService = agreementService;
        this.idempotencyService = idempotencyService;
        this.paymentProtocolService = paymentProtocolService;
        this.sensitiveDataCipher = sensitiveDataCipher;
        this.qwBenefitClient = qwBenefitClient;
        this.asyncCompensationEnqueueService = asyncCompensationEnqueueService;
    }

    @Override
    public ProductPageResponse getProductPage(String productCode, String memberId) {
        BenefitProduct product = benefitProductRepository.selectById(productCode);
        if (product == null || !"ACTIVE".equals(product.getStatus())) {
            throw new BizException("PRODUCT_NOT_FOUND", "Benefit product not found");
        }
        // 产品页本身只做展示聚合，不在 Controller 层拼接业务数据。
        MemberInfo memberInfo = memberId == null ? null : memberInfoRepository.selectById(memberId);
        return new ProductPageResponse(
                product.getProductCode(),
                product.getProductName(),
                product.getFeeRate(),
                null,
                null,
                List.of("EQUITY_AGREEMENT", "DEFERRED_AGREEMENT"),
                memberInfo == null ? null : memberInfo.getMemberId(),
                memberInfo == null ? null : memberInfo.getExternalUserId()
        );
    }

    @Override
    @Transactional(noRollbackFor = BenefitPurchaseSyncTimeoutCompensationException.class)
    public CreateBenefitOrderResponse createOrder(String memberId, CreateBenefitOrderRequest request) {
        BenefitOrder requestReplayOrder = benefitOrderRepository.selectOne(Wrappers.<BenefitOrder>lambdaQuery()
                .eq(BenefitOrder::getRequestId, request.requestId())
                .last("limit 1"));
        if (requestReplayOrder != null) {
            return buildCreateOrderResponse(requestReplayOrder);
        }
        if (idempotencyService.isProcessed(request.requestId())) {
            String benefitOrderNo = idempotencyService.getByRequestId(request.requestId()).getBizKey();
            BenefitOrder existingOrder = benefitOrderRepository.selectById(benefitOrderNo);
            if (existingOrder == null) {
                throw new BizException("ORDER_NOT_FOUND", "Benefit order already processed but record missing");
            }
            return buildCreateOrderResponse(existingOrder);
        }
        OrderStateMachine.ensureCanCreateOrder(Boolean.TRUE.equals(request.agreementSigned()));
        BenefitProduct product = benefitProductRepository.selectById(request.productCode());
        if (product == null || !"ACTIVE".equals(product.getStatus())) {
            throw new BizException("PRODUCT_NOT_FOUND", "Benefit product not found");
        }
        MemberInfo memberInfo = memberInfoRepository.selectById(memberId);
        if (memberInfo == null) {
            throw new BizException("MEMBER_NOT_FOUND", "Member not found");
        }
        MemberChannel link = memberChannelRepository.selectOne(Wrappers.<MemberChannel>lambdaQuery()
                .eq(MemberChannel::getMemberId, memberId)
                .orderByDesc(MemberChannel::getCreatedTs)
                .last("limit 1"));
        if (link == null) {
            throw new BizException("CHANNEL_LINK_NOT_FOUND", "Channel link not found");
        }
        // 订单始终以 benefitOrderNo 作为对外主键，后续与云卡、回调日志、支付记录都通过它串联。
        BenefitOrder benefitOrder = new BenefitOrder();
        benefitOrder.setBenefitOrderNo(RequestIdUtil.nextId("ord"));
        benefitOrder.setMemberId(memberInfo.getMemberId());
        benefitOrder.setSourceChannelCode(link.getChannelCode());
        benefitOrder.setExternalUserId(link.getExternalUserId());
        benefitOrder.setProductCode(product.getProductCode());
        benefitOrder.setAgreementNo(RequestIdUtil.nextId("agr"));
        benefitOrder.setLoanAmount(request.loanAmount());
        benefitOrder.setOrderStatus(BenefitOrderStatusEnum.FIRST_DEDUCT_PENDING.name());
        benefitOrder.setFirstDeductStatus(PaymentStatusEnum.PENDING.name());
        benefitOrder.setFallbackDeductStatus(PaymentStatusEnum.NONE.name());
        benefitOrder.setExerciseStatus(PaymentStatusEnum.NONE.name());
        benefitOrder.setRefundStatus(PaymentStatusEnum.NONE.name());
        benefitOrder.setGrantStatus("PENDING");
        benefitOrder.setSyncStatus(BenefitOrderStatusEnum.SYNC_PENDING.name());
        benefitOrder.setRequestId(request.requestId());
        PaymentProtocolService.ResolvedPaymentProtocol resolvedPaymentProtocol =
                paymentProtocolService.resolveForBenefitOrder(benefitOrder);
        benefitOrder.setPayProtocolNoSnapshot(resolvedPaymentProtocol.protocolNo());
        benefitOrder.setPayProtocolSource(resolvedPaymentProtocol.source());
        benefitOrder.setCreatedTs(LocalDateTime.now());
        benefitOrder.setUpdatedTs(LocalDateTime.now());
        benefitOrderRepository.insert(benefitOrder);
        // 创建订单后立即补齐协议任务与归档，确保后续支付和审计链路都有完整基础数据。
        agreementService.ensureAgreementArtifacts(benefitOrder);
        QwMemberSyncResponse syncResponse;
        try {
            syncResponse = qwBenefitClient.syncMemberOrder(buildQwMemberSyncRequest(benefitOrder, product, memberInfo));
            benefitOrder.setSyncStatus(BenefitOrderStatusEnum.SYNC_SUCCESS.name());
            benefitOrder.setUpdatedTs(LocalDateTime.now());
            benefitOrderRepository.updateById(benefitOrder);
        } catch (UpstreamTimeoutException exception) {
            benefitOrder.setSyncStatus(BenefitOrderStatusEnum.SYNC_FAIL.name());
            benefitOrder.setUpdatedTs(LocalDateTime.now());
            benefitOrderRepository.updateById(benefitOrder);
            asyncCompensationEnqueueService.enqueue(new AsyncCompensationEnqueueService.EnqueueCommand(
                    "QW_BENEFIT_PURCHASE_RETRY",
                    "BENEFIT_PURCHASE:" + benefitOrder.getBenefitOrderNo(),
                    benefitOrder.getBenefitOrderNo(),
                    "QW",
                    "/api/abs/method",
                    "POST",
                    null,
                    new AsyncCompensationEnqueuePayload.QwBenefitPurchaseRetry(
                            benefitOrder.getExternalUserId(),
                            benefitOrder.getBenefitOrderNo(),
                            product.getProductCode(),
                            benefitOrder.getLoanAmount()
                    )
            ));
            throw new BenefitPurchaseSyncTimeoutCompensationException("QW_SYNC_TIMEOUT:" + exception.getMessage());
        }
        idempotencyService.markProcessed(
                request.requestId(),
                "CREATE_ORDER",
                benefitOrder.getBenefitOrderNo(),
                benefitOrder.getOrderStatus()
        );
        log.info("traceId={} bizOrderNo={} order created and synced qwOrderNo={}",
                com.nexusfin.equity.util.TraceIdUtil.getTraceId(),
                benefitOrder.getBenefitOrderNo(),
                syncResponse.orderNo());
        return buildCreateOrderResponse(benefitOrder);
    }

    @Override
    public BenefitOrderStatusResponse getOrderStatus(String benefitOrderNo) {
        BenefitOrder order = getOrder(benefitOrderNo);
        return new BenefitOrderStatusResponse(
                order.getBenefitOrderNo(),
                order.getOrderStatus(),
                order.getFirstDeductStatus(),
                order.getFallbackDeductStatus(),
                order.getExerciseStatus(),
                order.getGrantStatus()
        );
    }

    @Override
    public ExerciseUrlResponse getExerciseUrl(String benefitOrderNo) {
        BenefitOrder order = getOrder(benefitOrderNo);
        QwExerciseUrlResponse response = qwBenefitClient.getExerciseUrl(new QwExerciseUrlRequest(
                order.getExternalUserId(),
                order.getBenefitOrderNo()
        ));
        return new ExerciseUrlResponse(
                response.redirectUrl(),
                response.cardExpiryDate()
        );
    }

    private BenefitOrder getOrder(String benefitOrderNo) {
        BenefitOrder order = benefitOrderRepository.selectById(benefitOrderNo);
        if (order == null) {
            throw new BizException("ORDER_NOT_FOUND", "Benefit order not found");
        }
        return order;
    }

    private CreateBenefitOrderResponse buildCreateOrderResponse(BenefitOrder order) {
        return new CreateBenefitOrderResponse(
                order.getBenefitOrderNo(),
                order.getOrderStatus(),
                "/h5/equity/orders/" + order.getBenefitOrderNo()
        );
    }

    private QwMemberSyncRequest buildQwMemberSyncRequest(BenefitOrder order, BenefitProduct product, MemberInfo memberInfo) {
        String realName = sensitiveDataCipher.decrypt(memberInfo.getRealNameEncrypted());
        String mobile = memberInfo.getMobileEncrypted() == null ? null : sensitiveDataCipher.decrypt(memberInfo.getMobileEncrypted());
        return new QwMemberSyncRequest(
                order.getExternalUserId(),
                order.getBenefitOrderNo(),
                order.getLoanAmount(),
                product.getProductCode(),
                product.getProductName(),
                mobile,
                realName,
                order.getPayProtocolNoSnapshot(),
                null,
                0,
                null,
                null,
                null,
                null
        );
    }
}
