package com.nexusfin.equity.service;

import com.nexusfin.equity.dto.request.CreateBenefitOrderRequest;
import com.nexusfin.equity.config.QwProperties;
import com.nexusfin.equity.entity.BenefitProduct;
import com.nexusfin.equity.exception.BizException;
import com.nexusfin.equity.repository.BenefitOrderRepository;
import com.nexusfin.equity.repository.BenefitProductRepository;
import com.nexusfin.equity.repository.MemberChannelRepository;
import com.nexusfin.equity.repository.MemberInfoRepository;
import com.nexusfin.equity.service.impl.BenefitOrderServiceImpl;
import com.nexusfin.equity.thirdparty.qw.QwBenefitClient;
import com.nexusfin.equity.util.SensitiveDataCipher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BenefitOrderServiceAuthTest {

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
    void shouldRejectAuthenticatedOrderFlowWhenMemberMissing() {
        when(idempotencyService.isProcessed("req-auth-order-1")).thenReturn(false);
        BenefitProduct product = new BenefitProduct();
        product.setProductCode("P-1");
        product.setStatus("ACTIVE");
        when(benefitProductRepository.selectById("P-1")).thenReturn(product);

        assertThatThrownBy(() -> benefitOrderService.createOrder(
                "missing-member",
                new CreateBenefitOrderRequest("req-auth-order-1", "P-1", 680000L, true)
        )).isInstanceOf(BizException.class)
                .hasMessageContaining("MEMBER_NOT_FOUND");
    }
}
