package com.nexusfin.equity.service.impl;

import com.nexusfin.equity.dto.response.BenefitDispatchContextResponse;
import com.nexusfin.equity.dto.response.BenefitDispatchResolveResponse;
import com.nexusfin.equity.entity.BenefitOrder;
import com.nexusfin.equity.exception.BizException;
import com.nexusfin.equity.repository.BenefitOrderRepository;
import com.nexusfin.equity.service.BenefitDispatchService;
import org.springframework.stereotype.Service;

@Service
public class BenefitDispatchServiceImpl implements BenefitDispatchService {

    private final BenefitOrderRepository benefitOrderRepository;

    public BenefitDispatchServiceImpl(BenefitOrderRepository benefitOrderRepository) {
        this.benefitOrderRepository = benefitOrderRepository;
    }

    @Override
    public BenefitDispatchContextResponse getContext(String benefitOrderNo) {
        BenefitOrder benefitOrder = requiredOrder(benefitOrderNo);
        String orderStatus = benefitOrder.getOrderStatus() == null || benefitOrder.getOrderStatus().isBlank()
                ? "UNKNOWN"
                : benefitOrder.getOrderStatus();
        return new BenefitDispatchContextResponse(
                benefitOrder.getBenefitOrderNo(),
                "push",
                orderStatus,
                true,
                "INTERMEDIATE",
                "dispatch context ready"
        );
    }

    @Override
    public BenefitDispatchResolveResponse resolve(String benefitOrderNo) {
        BenefitOrder benefitOrder = requiredOrder(benefitOrderNo);
        return new BenefitDispatchResolveResponse(
                benefitOrder.getBenefitOrderNo(),
                true,
                "DIRECT",
                "https://supplier.example/benefit?benefitOrderNo=" + benefitOrder.getBenefitOrderNo(),
                "dispatch target ready"
        );
    }

    private BenefitOrder requiredOrder(String benefitOrderNo) {
        BenefitOrder benefitOrder = benefitOrderRepository.selectById(benefitOrderNo);
        if (benefitOrder == null) {
            throw new BizException("BENEFIT_ORDER_NOT_FOUND", "Benefit order does not exist");
        }
        return benefitOrder;
    }
}
