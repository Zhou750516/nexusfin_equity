package com.nexusfin.equity.service;

import com.nexusfin.equity.service.impl.QwDeductionServiceImpl;
import com.nexusfin.equity.thirdparty.qw.QwBenefitClient;
import com.nexusfin.equity.thirdparty.qw.QwDeductionQueryRequest;
import com.nexusfin.equity.thirdparty.qw.QwDeductionQueryResponse;
import com.nexusfin.equity.thirdparty.qw.QwOrderCancelRequest;
import com.nexusfin.equity.thirdparty.qw.QwOrderCancelResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QwDeductionServiceTest {

    @Mock
    private QwBenefitClient qwBenefitClient;

    @Test
    void shouldQueryDeductionStatusThroughQwClient() {
        QwDeductionService service = new QwDeductionServiceImpl(qwBenefitClient);
        when(qwBenefitClient.queryDeduction(new QwDeductionQueryRequest("user-1", "ord-1")))
                .thenReturn(new QwDeductionQueryResponse(2));

        QwDeductionQueryResponse response = service.queryDeduction("user-1", "ord-1");

        assertThat(response.status()).isEqualTo(2);
        verify(qwBenefitClient).queryDeduction(new QwDeductionQueryRequest("user-1", "ord-1"));
    }

    @Test
    void shouldCancelOrderThroughQwClient() {
        QwDeductionService service = new QwDeductionServiceImpl(qwBenefitClient);
        when(qwBenefitClient.cancelOrder(new QwOrderCancelRequest("ord-1")))
                .thenReturn(new QwOrderCancelResponse(true));

        QwOrderCancelResponse response = service.cancelOrder("ord-1");

        assertThat(response.success()).isTrue();
        verify(qwBenefitClient).cancelOrder(new QwOrderCancelRequest("ord-1"));
    }
}
