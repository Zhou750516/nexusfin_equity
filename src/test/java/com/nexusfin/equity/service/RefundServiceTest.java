package com.nexusfin.equity.service;

import com.nexusfin.equity.dto.response.RefundInfoResponse;
import com.nexusfin.equity.dto.response.RefundApplyResponse;
import com.nexusfin.equity.dto.response.RefundResultResponse;
import com.nexusfin.equity.entity.BenefitOrder;
import com.nexusfin.equity.repository.BenefitOrderRepository;
import com.nexusfin.equity.service.RefundClient;
import com.nexusfin.equity.service.impl.SkeletonRefundClient;
import com.nexusfin.equity.service.impl.RefundServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RefundServiceTest {

    @Mock
    private BenefitOrderRepository benefitOrderRepository;

    @Mock
    private RefundClient refundClient;

    @Test
    void shouldBuildRefundInfoForBenefitOrder() {
        RefundServiceImpl refundService = new RefundServiceImpl(benefitOrderRepository, refundClient);
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
    void shouldDelegateRefundApplyToRefundClient() {
        RefundServiceImpl refundService = new RefundServiceImpl(benefitOrderRepository, refundClient);
        BenefitOrder benefitOrder = new BenefitOrder();
        benefitOrder.setBenefitOrderNo("BEN-20260418-001");

        when(benefitOrderRepository.selectById("BEN-20260418-001")).thenReturn(benefitOrder);
        when(refundClient.apply(new RefundClient.RefundApplyCommand(
                "BEN-20260418-001",
                "USER_REQUEST"
        ))).thenReturn(new RefundApplyResponse(
                "REFUND-BEN-20260418-001",
                "processing",
                "refund submitted"
        ));

        RefundApplyResponse response = refundService.apply("BEN-20260418-001", "USER_REQUEST");

        assertThat(response.refundId()).isEqualTo("REFUND-BEN-20260418-001");
        assertThat(response.status()).isEqualTo("processing");
        verify(refundClient).apply(new RefundClient.RefundApplyCommand(
                "BEN-20260418-001",
                "USER_REQUEST"
        ));
    }

    @Test
    void shouldDelegateRefundResultQueryToRefundClient() {
        RefundServiceImpl refundService = new RefundServiceImpl(benefitOrderRepository, refundClient);
        when(refundClient.getResult("REFUND-BEN-20260418-001")).thenReturn(new RefundResultResponse(
                "REFUND-BEN-20260418-001",
                "processing",
                "refund still processing"
        ));

        RefundResultResponse response = refundService.getResult("REFUND-BEN-20260418-001");

        assertThat(response.refundId()).isEqualTo("REFUND-BEN-20260418-001");
        assertThat(response.status()).isEqualTo("processing");
        verify(refundClient).getResult("REFUND-BEN-20260418-001");
    }

    @Test
    void shouldReturnPlaceholderResponsesFromSkeletonRefundClientWhileRealRefundIntegrationIsPending() {
        SkeletonRefundClient refundClient = new SkeletonRefundClient();

        RefundApplyResponse applyResponse = refundClient.apply(new RefundClient.RefundApplyCommand(
                "BEN-20260418-001",
                "USER_REQUEST"
        ));
        RefundResultResponse resultResponse = refundClient.getResult("REFUND-BEN-20260418-001");

        assertThat(applyResponse.refundId()).isEqualTo("REFUND-BEN-20260418-001");
        assertThat(applyResponse.status()).isEqualTo("processing");
        assertThat(applyResponse.message()).isEqualTo("refund submitted");
        assertThat(resultResponse.refundId()).isEqualTo("REFUND-BEN-20260418-001");
        assertThat(resultResponse.status()).isEqualTo("processing");
        assertThat(resultResponse.message()).isEqualTo("refund still processing");
    }
}
