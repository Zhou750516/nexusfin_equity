package com.nexusfin.equity.service;

import com.nexusfin.equity.dto.response.RefundInfoResponse;
import com.nexusfin.equity.dto.response.RefundApplyResponse;
import com.nexusfin.equity.dto.response.RefundResultResponse;
import com.nexusfin.equity.entity.BenefitOrder;
import com.nexusfin.equity.repository.BenefitOrderRepository;
import com.nexusfin.equity.service.impl.RefundServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefundServiceTest {

    @Mock
    private BenefitOrderRepository benefitOrderRepository;

    @Test
    void shouldBuildRefundInfoForBenefitOrder() {
        RefundServiceImpl refundService = new RefundServiceImpl(benefitOrderRepository);
        BenefitOrder benefitOrder = new BenefitOrder();
        benefitOrder.setBenefitOrderNo("BEN-20260418-001");
        benefitOrder.setRefundStatus("NONE");

        when(benefitOrderRepository.selectById("BEN-20260418-001")).thenReturn(benefitOrder);

        RefundInfoResponse response = refundService.getInfo("BEN-20260418-001");

        assertThat(response.benefitOrderNo()).isEqualTo("BEN-20260418-001");
        assertThat(response.refundable()).isTrue();
        assertThat(response.refundStatus()).isEqualTo("NONE");
        assertThat(response.refundableAmount()).isEqualTo(29900L);
    }

    @Test
    void shouldCreateRefundApplyResponseForExistingBenefitOrder() {
        RefundServiceImpl refundService = new RefundServiceImpl(benefitOrderRepository);
        BenefitOrder benefitOrder = new BenefitOrder();
        benefitOrder.setBenefitOrderNo("BEN-20260418-001");

        when(benefitOrderRepository.selectById("BEN-20260418-001")).thenReturn(benefitOrder);

        RefundApplyResponse response = refundService.apply("BEN-20260418-001", "USER_REQUEST");

        assertThat(response.refundId()).isEqualTo("REFUND-BEN-20260418-001");
        assertThat(response.status()).isEqualTo("processing");
    }

    @Test
    void shouldBuildRefundResultForRefundId() {
        RefundServiceImpl refundService = new RefundServiceImpl(benefitOrderRepository);

        RefundResultResponse response = refundService.getResult("REFUND-BEN-20260418-001");

        assertThat(response.refundId()).isEqualTo("REFUND-BEN-20260418-001");
        assertThat(response.status()).isEqualTo("processing");
    }
}
