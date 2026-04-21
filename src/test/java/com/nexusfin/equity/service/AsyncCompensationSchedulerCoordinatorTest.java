package com.nexusfin.equity.service;

import com.nexusfin.equity.config.AsyncCompensationProperties;
import com.nexusfin.equity.service.impl.AsyncCompensationSchedulerCoordinatorImpl;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AsyncCompensationSchedulerCoordinatorTest {

    @Mock
    private AsyncCompensationWorkerService workerService;

    @Mock
    private AsyncCompensationSupervisorService supervisorService;

    @Test
    void shouldReturnConfiguredWorkerIdWhenPresent() {
        AsyncCompensationProperties properties = new AsyncCompensationProperties();
        properties.setWorkerId(" worker-a ");
        AsyncCompensationSchedulerCoordinator coordinator = buildCoordinator(properties);

        assertThat(coordinator.getWorkerId()).isEqualTo("worker-a");
    }

    @Test
    void shouldBuildWorkerIdFromApplicationNameWhenMissing() {
        AsyncCompensationSchedulerCoordinator coordinator = buildCoordinator(new AsyncCompensationProperties());

        assertThat(coordinator.getWorkerId()).startsWith("nexusfin-equity-");
    }

    @Test
    void shouldSortAndDeduplicateOwnedPartitions() {
        AsyncCompensationProperties properties = new AsyncCompensationProperties();
        properties.setOwnedPartitions(List.of(3, 1, 3, 2));
        AsyncCompensationSchedulerCoordinator coordinator = buildCoordinator(properties);

        assertThat(coordinator.getOwnedPartitions()).containsExactly(1, 2, 3);
    }

    @Test
    void shouldHeartbeatRunningWithTaskIdWhenTaskClaimed() {
        AsyncCompensationProperties properties = new AsyncCompensationProperties();
        properties.setWorkerId("worker-3");
        AsyncCompensationSchedulerCoordinator coordinator = buildCoordinator(properties);
        when(workerService.processNext(any(), any(Integer.class), any()))
                .thenAnswer(invocation -> {
                    AsyncCompensationWorkerService.WorkerLifecycleListener listener =
                            invocation.getArgument(2, AsyncCompensationWorkerService.WorkerLifecycleListener.class);
                    listener.onTaskClaimed("task-3");
                    return AsyncCompensationWorkerService.WorkerProcessResult.handled("task-3");
                });

        coordinator.runWorkerTick(3);

        InOrder inOrder = inOrder(supervisorService, workerService);
        inOrder.verify(workerService).processNext(any(), any(Integer.class), any());
        inOrder.verify(supervisorService).heartbeat(3, "worker-3", "RUNNING", "task-3");
        inOrder.verify(supervisorService).heartbeat(3, "worker-3", "IDLE", null);
    }

    @Test
    void shouldOnlyHeartbeatIdleWhenNoTaskHandled() {
        AsyncCompensationProperties properties = new AsyncCompensationProperties();
        properties.setWorkerId("worker-5");
        AsyncCompensationSchedulerCoordinator coordinator = buildCoordinator(properties);
        when(workerService.processNext(any(), any(Integer.class), any()))
                .thenReturn(AsyncCompensationWorkerService.WorkerProcessResult.none());

        coordinator.runWorkerTick(5);

        verify(workerService).processNext(any(), any(Integer.class), any());
        verify(supervisorService).heartbeat(5, "worker-5", "IDLE", null);
        verify(supervisorService, never()).heartbeat(5, "worker-5", "RUNNING", "task-5");
    }

    private AsyncCompensationSchedulerCoordinator buildCoordinator(AsyncCompensationProperties properties) {
        return new AsyncCompensationSchedulerCoordinatorImpl(
                properties,
                workerService,
                supervisorService,
                "nexusfin-equity"
        );
    }
}
