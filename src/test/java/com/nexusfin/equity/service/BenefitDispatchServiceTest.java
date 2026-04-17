package com.nexusfin.equity.service;

import com.nexusfin.equity.dto.response.BenefitDispatchContextResponse;
import com.nexusfin.equity.dto.response.BenefitDispatchResolveResponse;
import com.nexusfin.equity.entity.BenefitOrder;
import com.nexusfin.equity.repository.BenefitOrderRepository;
import com.nexusfin.equity.service.impl.BenefitDispatchServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BenefitDispatchServiceTest {

    @Mock
    private BenefitOrderRepository benefitOrderRepository;

    @Test
    void shouldBuildDispatchContextForExistingBenefitOrder() {
        BenefitDispatchServiceImpl benefitDispatchService = new BenefitDispatchServiceImpl(benefitOrderRepository);
        BenefitOrder benefitOrder = new BenefitOrder();
        benefitOrder.setBenefitOrderNo("BEN-20260418-001");
        benefitOrder.setOrderStatus("ACTIVE");

        when(benefitOrderRepository.selectById("BEN-20260418-001")).thenReturn(benefitOrder);

        BenefitDispatchContextResponse response = benefitDispatchService.getContext("BEN-20260418-001");

        assertThat(response.benefitOrderNo()).isEqualTo("BEN-20260418-001");
        assertThat(response.scene()).isEqualTo("push");
        assertThat(response.orderStatus()).isEqualTo("ACTIVE");
        assertThat(response.allowRedirect()).isTrue();
        assertThat(response.redirectMode()).isEqualTo("INTERMEDIATE");
    }

    @Test
    void shouldResolveDirectRedirectForExistingBenefitOrder() {
        BenefitDispatchServiceImpl benefitDispatchService = new BenefitDispatchServiceImpl(benefitOrderRepository);
        BenefitOrder benefitOrder = new BenefitOrder();
        benefitOrder.setBenefitOrderNo("BEN-20260418-001");
        benefitOrder.setOrderStatus("ACTIVE");

        when(benefitOrderRepository.selectById("BEN-20260418-001")).thenReturn(benefitOrder);

        BenefitDispatchResolveResponse response = benefitDispatchService.resolve("BEN-20260418-001");

        assertThat(response.benefitOrderNo()).isEqualTo("BEN-20260418-001");
        assertThat(response.allowRedirect()).isTrue();
        assertThat(response.redirectMode()).isEqualTo("DIRECT");
        assertThat(response.supplierUrl()).contains("BEN-20260418-001");
    }
}
