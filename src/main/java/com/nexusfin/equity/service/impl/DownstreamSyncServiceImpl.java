package com.nexusfin.equity.service.impl;

import com.nexusfin.equity.entity.BenefitOrder;
import com.nexusfin.equity.enums.BenefitOrderStatusEnum;
import com.nexusfin.equity.exception.BizException;
import com.nexusfin.equity.repository.BenefitOrderRepository;
import com.nexusfin.equity.service.IdempotencyService;
import com.nexusfin.equity.util.SensitiveDataUtil;
import com.nexusfin.equity.service.DownstreamSyncService;
import com.nexusfin.equity.util.TraceIdUtil;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DownstreamSyncServiceImpl implements DownstreamSyncService {

    private static final Logger log = LoggerFactory.getLogger(DownstreamSyncServiceImpl.class);
    private static final String SYNC_BIZ_TYPE = "DOWNSTREAM_SYNC";
    private static final String ROUTE_DIRECT = "DIRECT_CONTINUE";
    private static final String ROUTE_FALLBACK = "FALLBACK_ELIGIBLE";

    private final BenefitOrderRepository benefitOrderRepository;
    private final IdempotencyService idempotencyService;

    public DownstreamSyncServiceImpl(
            BenefitOrderRepository benefitOrderRepository,
            IdempotencyService idempotencyService
    ) {
        this.benefitOrderRepository = benefitOrderRepository;
        this.idempotencyService = idempotencyService;
    }

    @Override
    public void syncOrder(BenefitOrder order) {
        String syncRequestId = buildSyncRequestId(order);
        if (idempotencyService.isProcessed(syncRequestId)) {
            return;
        }
        String route = resolveRoute(order);
        if (order.getMemberId() == null || order.getProductCode() == null) {
            order.setSyncStatus(BenefitOrderStatusEnum.SYNC_FAIL.name());
            order.setUpdatedTs(LocalDateTime.now());
            benefitOrderRepository.updateById(order);
            throw new BizException("SYNC_DATA_MISSING", "Order missing required downstream sync fields");
        }
        order.setSyncStatus(BenefitOrderStatusEnum.SYNC_SUCCESS.name());
        order.setUpdatedTs(LocalDateTime.now());
        benefitOrderRepository.updateById(order);
        String payload = """
                {"benefitOrderNo":"%s","memberId":"%s","productCode":"%s","route":"%s","orderStatus":"%s"}
                """.formatted(
                order.getBenefitOrderNo(),
                order.getMemberId(),
                order.getProductCode(),
                route,
                order.getOrderStatus()
        );
        idempotencyService.markProcessed(syncRequestId, SYNC_BIZ_TYPE, order.getBenefitOrderNo(), payload);
        log.info("traceId={} bizOrderNo={} order synced downstream route={}",
                TraceIdUtil.getTraceId(), order.getBenefitOrderNo(), route);
    }

    private String buildSyncRequestId(BenefitOrder order) {
        String raw = order.getBenefitOrderNo() + ":" + order.getOrderStatus();
        return "sync-" + SensitiveDataUtil.sha256(raw).substring(0, 24);
    }

    private String resolveRoute(BenefitOrder order) {
        if (BenefitOrderStatusEnum.FIRST_DEDUCT_SUCCESS.name().equals(order.getOrderStatus())) {
            return ROUTE_DIRECT;
        }
        if (BenefitOrderStatusEnum.FIRST_DEDUCT_FAIL.name().equals(order.getOrderStatus())
                || BenefitOrderStatusEnum.FALLBACK_DEDUCT_PENDING.name().equals(order.getOrderStatus())) {
            return ROUTE_FALLBACK;
        }
        order.setSyncStatus(BenefitOrderStatusEnum.SYNC_FAIL.name());
        order.setUpdatedTs(LocalDateTime.now());
        benefitOrderRepository.updateById(order);
        throw new BizException("SYNC_ILLEGAL_STATE", "Order is not eligible for downstream sync");
    }
}
