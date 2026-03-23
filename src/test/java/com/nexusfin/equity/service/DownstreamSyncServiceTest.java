package com.nexusfin.equity.service;

import com.nexusfin.equity.entity.BenefitOrder;
import com.nexusfin.equity.exception.BizException;
import com.nexusfin.equity.repository.BenefitOrderRepository;
import com.nexusfin.equity.service.impl.DownstreamSyncServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DownstreamSyncServiceTest {

    @Mock
    private BenefitOrderRepository benefitOrderRepository;

    @Mock
    private IdempotencyService idempotencyService;

    @InjectMocks
    private DownstreamSyncServiceImpl downstreamSyncService;

    @Test
    void shouldSyncDirectContinuationOrder() {
        BenefitOrder order = order("ord-sync-direct", "FIRST_DEDUCT_SUCCESS");
        when(idempotencyService.isProcessed(argThat(requestId -> requestId != null && requestId.startsWith("sync-"))))
                .thenReturn(false);

        downstreamSyncService.syncOrder(order);

        assertThat(order.getSyncStatus()).isEqualTo("SYNC_SUCCESS");
        verify(benefitOrderRepository).updateById(order);
        verify(idempotencyService).markProcessed(
                argThat(requestId -> requestId != null && requestId.startsWith("sync-") && requestId.length() <= 29),
                argThat("DOWNSTREAM_SYNC"::equals),
                argThat("ord-sync-direct"::equals),
                contains("\"route\":\"DIRECT_CONTINUE\"")
        );
    }

    @Test
    void shouldSyncFallbackEligibleOrder() {
        BenefitOrder order = order("ord-sync-fallback", "FIRST_DEDUCT_FAIL");
        when(idempotencyService.isProcessed(argThat(requestId -> requestId != null && requestId.startsWith("sync-"))))
                .thenReturn(false);

        downstreamSyncService.syncOrder(order);

        assertThat(order.getSyncStatus()).isEqualTo("SYNC_SUCCESS");
        verify(idempotencyService).markProcessed(
                argThat(requestId -> requestId != null && requestId.startsWith("sync-") && requestId.length() <= 29),
                argThat("DOWNSTREAM_SYNC"::equals),
                argThat("ord-sync-fallback"::equals),
                contains("\"route\":\"FALLBACK_ELIGIBLE\"")
        );
    }

    @Test
    void shouldRejectUnsupportedOrderStatus() {
        BenefitOrder order = order("ord-sync-illegal", "EXERCISE_SUCCESS");
        when(idempotencyService.isProcessed(argThat(requestId -> requestId != null && requestId.startsWith("sync-"))))
                .thenReturn(false);

        assertThatThrownBy(() -> downstreamSyncService.syncOrder(order))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("SYNC_ILLEGAL_STATE");

        assertThat(order.getSyncStatus()).isEqualTo("SYNC_FAIL");
        verify(benefitOrderRepository).updateById(order);
        verify(idempotencyService, never()).markProcessed(any(), any(), any(), any());
    }

    private BenefitOrder order(String benefitOrderNo, String orderStatus) {
        BenefitOrder order = new BenefitOrder();
        order.setBenefitOrderNo(benefitOrderNo);
        order.setMemberId("mem-" + benefitOrderNo);
        order.setProductCode("PROD-" + benefitOrderNo);
        order.setOrderStatus(orderStatus);
        order.setSyncStatus("SYNC_PENDING");
        return order;
    }
}
