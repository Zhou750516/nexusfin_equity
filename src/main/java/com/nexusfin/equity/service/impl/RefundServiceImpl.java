package com.nexusfin.equity.service.impl;

import com.nexusfin.equity.dto.response.RefundApplyResponse;
import com.nexusfin.equity.dto.response.RefundInfoResponse;
import com.nexusfin.equity.dto.response.RefundResultResponse;
import com.nexusfin.equity.entity.BenefitOrder;
import com.nexusfin.equity.exception.BizException;
import com.nexusfin.equity.repository.BenefitOrderRepository;
import com.nexusfin.equity.service.RefundService;
import org.springframework.stereotype.Service;

@Service
public class RefundServiceImpl implements RefundService {

    private final BenefitOrderRepository benefitOrderRepository;

    public RefundServiceImpl(BenefitOrderRepository benefitOrderRepository) {
        this.benefitOrderRepository = benefitOrderRepository;
    }

    @Override
    public RefundInfoResponse getInfo(String benefitOrderNo) {
        BenefitOrder benefitOrder = benefitOrderRepository.selectById(benefitOrderNo);
        if (benefitOrder == null) {
            throw new BizException("BENEFIT_ORDER_NOT_FOUND", "Benefit order does not exist");
        }

        String refundStatus = benefitOrder.getRefundStatus() == null || benefitOrder.getRefundStatus().isBlank()
                ? "NONE"
                : benefitOrder.getRefundStatus();
        boolean refundable = !"SUCCESS".equalsIgnoreCase(refundStatus);

        return new RefundInfoResponse(
                benefitOrder.getBenefitOrderNo(),
                refundable,
                refundStatus,
                29900L,
                refundable ? "refund ready" : "refund not available"
        );
    }

    @Override
    public RefundApplyResponse apply(String benefitOrderNo, String reason) {
        BenefitOrder benefitOrder = benefitOrderRepository.selectById(benefitOrderNo);
        if (benefitOrder == null) {
            throw new BizException("BENEFIT_ORDER_NOT_FOUND", "Benefit order does not exist");
        }
        return new RefundApplyResponse(
                "REFUND-" + benefitOrder.getBenefitOrderNo(),
                "processing",
                "refund submitted"
        );
    }

    @Override
    public RefundResultResponse getResult(String refundId) {
        return new RefundResultResponse(
                refundId,
                "processing",
                "refund still processing"
        );
    }
}
