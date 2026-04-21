package com.nexusfin.equity.schedule;

import com.nexusfin.equity.service.AsyncCompensationSchedulerCoordinator;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AsyncCompensationWorkerSchedulerTest {

    @Mock
    private AsyncCompensationSchedulerCoordinator coordinator;

    @Test
    void shouldSkipWhenSchedulingDisabled() {
        when(coordinator.isSchedulingEnabled()).thenReturn(false);
        AsyncCompensationWorkerScheduler scheduler = new AsyncCompensationWorkerScheduler(coordinator);

        scheduler.poll();

        verify(coordinator).isSchedulingEnabled();
        verify(coordinator, never()).isWorkerEnabled();
        verify(coordinator, never()).getOwnedPartitions();
        verify(coordinator, never()).runWorkerTick(0);
    }

    @Test
    void shouldSkipWhenWorkerDisabled() {
        when(coordinator.isSchedulingEnabled()).thenReturn(true);
        when(coordinator.isWorkerEnabled()).thenReturn(false);
        AsyncCompensationWorkerScheduler scheduler = new AsyncCompensationWorkerScheduler(coordinator);

        scheduler.poll();

        verify(coordinator).isSchedulingEnabled();
        verify(coordinator).isWorkerEnabled();
        verify(coordinator, never()).getOwnedPartitions();
    }

    @Test
    void shouldSkipWhenOwnedPartitionsEmpty() {
        when(coordinator.isSchedulingEnabled()).thenReturn(true);
        when(coordinator.isWorkerEnabled()).thenReturn(true);
        when(coordinator.getOwnedPartitions()).thenReturn(List.of());
        AsyncCompensationWorkerScheduler scheduler = new AsyncCompensationWorkerScheduler(coordinator);

        scheduler.poll();

        verify(coordinator).getOwnedPartitions();
        verify(coordinator, never()).runWorkerTick(0);
    }

    @Test
    void shouldProcessPartitionsInOrder() {
        when(coordinator.isSchedulingEnabled()).thenReturn(true);
        when(coordinator.isWorkerEnabled()).thenReturn(true);
        when(coordinator.getOwnedPartitions()).thenReturn(List.of(0, 1, 2));
        AsyncCompensationWorkerScheduler scheduler = new AsyncCompensationWorkerScheduler(coordinator);

        scheduler.poll();

        InOrder inOrder = inOrder(coordinator);
        inOrder.verify(coordinator).isSchedulingEnabled();
        inOrder.verify(coordinator).isWorkerEnabled();
        inOrder.verify(coordinator).getOwnedPartitions();
        inOrder.verify(coordinator).runWorkerTick(0);
        inOrder.verify(coordinator).runWorkerTick(1);
        inOrder.verify(coordinator).runWorkerTick(2);
    }

    @Test
    void shouldContinueWhenSinglePartitionFails() {
        when(coordinator.isSchedulingEnabled()).thenReturn(true);
        when(coordinator.isWorkerEnabled()).thenReturn(true);
        when(coordinator.getOwnedPartitions()).thenReturn(List.of(0, 1, 2));
        doAnswer(invocation -> {
            Integer partitionNo = invocation.getArgument(0, Integer.class);
            if (partitionNo == 1) {
                throw new IllegalStateException("partition-1 failed");
            }
            return null;
        }).when(coordinator).runWorkerTick(anyInt());
        AsyncCompensationWorkerScheduler scheduler = new AsyncCompensationWorkerScheduler(coordinator);

        scheduler.poll();

        InOrder inOrder = inOrder(coordinator);
        inOrder.verify(coordinator).isSchedulingEnabled();
        inOrder.verify(coordinator).isWorkerEnabled();
        inOrder.verify(coordinator).getOwnedPartitions();
        inOrder.verify(coordinator).runWorkerTick(0);
        inOrder.verify(coordinator).runWorkerTick(1);
        inOrder.verify(coordinator).runWorkerTick(2);
        verifyNoMoreInteractions(coordinator);
    }
}
