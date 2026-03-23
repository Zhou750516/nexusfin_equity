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
import com.nexusfin.equity.exception.BizException;
import com.nexusfin.equity.repository.BenefitOrderRepository;
import com.nexusfin.equity.repository.BenefitProductRepository;
import com.nexusfin.equity.repository.MemberChannelRepository;
import com.nexusfin.equity.repository.MemberInfoRepository;
import com.nexusfin.equity.service.AgreementService;
import com.nexusfin.equity.service.BenefitOrderService;
import com.nexusfin.equity.service.IdempotencyService;
import com.nexusfin.equity.util.OrderStateMachine;
import com.nexusfin.equity.util.RequestIdUtil;
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

    public BenefitOrderServiceImpl(
            BenefitProductRepository benefitProductRepository,
            BenefitOrderRepository benefitOrderRepository,
            MemberInfoRepository memberInfoRepository,
            MemberChannelRepository memberChannelRepository,
            AgreementService agreementService,
            IdempotencyService idempotencyService
    ) {
        this.benefitProductRepository = benefitProductRepository;
        this.benefitOrderRepository = benefitOrderRepository;
        this.memberInfoRepository = memberInfoRepository;
        this.memberChannelRepository = memberChannelRepository;
        this.agreementService = agreementService;
        this.idempotencyService = idempotencyService;
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
    @Transactional
    public CreateBenefitOrderResponse createOrder(CreateBenefitOrderRequest request) {
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
        MemberInfo memberInfo = memberInfoRepository.selectById(request.memberId());
        if (memberInfo == null) {
            throw new BizException("MEMBER_NOT_FOUND", "Member not found");
        }
        MemberChannel link = memberChannelRepository.selectOne(Wrappers.<MemberChannel>lambdaQuery()
                .eq(MemberChannel::getMemberId, request.memberId())
                .orderByDesc(MemberChannel::getCreatedTs)
                .last("limit 1"));
        if (link == null) {
            throw new BizException("CHANNEL_LINK_NOT_FOUND", "Channel link not found");
        }
        // 订单始终以 benefitOrderNo 作为对外主键，后续与云卡、回调日志、支付记录都通过它串联。
        BenefitOrder benefitOrder = new BenefitOrder();
        benefitOrder.setBenefitOrderNo(RequestIdUtil.nextId("ord"));
        benefitOrder.setMemberId(memberInfo.getMemberId());
        benefitOrder.setChannelCode(link.getChannelCode());
        benefitOrder.setExternalUserId(link.getExternalUserId());
        benefitOrder.setProductCode(product.getProductCode());
        benefitOrder.setAgreementNo(RequestIdUtil.nextId("agr"));
        benefitOrder.setLoanAmount(request.loanAmount());
        benefitOrder.setOrderStatus(BenefitOrderStatusEnum.FIRST_DEDUCT_PENDING.name());
        benefitOrder.setQwFirstDeductStatus(PaymentStatusEnum.PENDING.name());
        benefitOrder.setQwFallbackDeductStatus(PaymentStatusEnum.NONE.name());
        benefitOrder.setQwExerciseStatus(PaymentStatusEnum.NONE.name());
        benefitOrder.setRefundStatus(PaymentStatusEnum.NONE.name());
        benefitOrder.setGrantStatus("PENDING");
        benefitOrder.setSyncStatus(BenefitOrderStatusEnum.SYNC_PENDING.name());
        benefitOrder.setRequestId(request.requestId());
        benefitOrder.setCreatedTs(LocalDateTime.now());
        benefitOrder.setUpdatedTs(LocalDateTime.now());
        benefitOrderRepository.insert(benefitOrder);
        // 创建订单后立即补齐协议任务与归档，确保后续支付和审计链路都有完整基础数据。
        agreementService.ensureAgreementArtifacts(benefitOrder);
        idempotencyService.markProcessed(
                request.requestId(),
                "CREATE_ORDER",
                benefitOrder.getBenefitOrderNo(),
                benefitOrder.getOrderStatus()
        );
        log.info("traceId={} bizOrderNo={} order created", com.nexusfin.equity.util.TraceIdUtil.getTraceId(), benefitOrder.getBenefitOrderNo());
        return buildCreateOrderResponse(benefitOrder);
    }

    @Override
    public BenefitOrderStatusResponse getOrderStatus(String benefitOrderNo) {
        BenefitOrder order = getOrder(benefitOrderNo);
        return new BenefitOrderStatusResponse(
                order.getBenefitOrderNo(),
                order.getOrderStatus(),
                order.getQwFirstDeductStatus(),
                order.getQwFallbackDeductStatus(),
                order.getQwExerciseStatus(),
                order.getGrantStatus()
        );
    }

    @Override
    public ExerciseUrlResponse getExerciseUrl(String benefitOrderNo) {
        BenefitOrder order = getOrder(benefitOrderNo);
        return new ExerciseUrlResponse(
                "https://abs.example.com/exercise/" + order.getBenefitOrderNo(),
                LocalDateTime.now().plusDays(1).toString()
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
}
