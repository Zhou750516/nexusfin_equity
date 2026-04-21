package com.nexusfin.equity.service;

import com.nexusfin.equity.config.AsyncCompensationProperties;
import com.nexusfin.equity.entity.AsyncCompensationPartitionRuntime;
import com.nexusfin.equity.entity.AsyncCompensationTask;
import com.nexusfin.equity.repository.AsyncCompensationPartitionRuntimeRepository;
import com.nexusfin.equity.repository.AsyncCompensationTaskRepository;
import com.nexusfin.equity.service.impl.AsyncCompensationSupervisorServiceImpl;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AsyncCompensationSupervisorServiceTest {

    @Mock
    private AsyncCompensationTaskRepository taskRepository;

    @Mock
    private AsyncCompensationPartitionRuntimeRepository runtimeRepository;

    @Test
    void shouldInsertPartitionHeartbeatWhenRuntimeMissing() {
        AsyncCompensationSupervisorService service = buildService();
        when(runtimeRepository.selectById(3)).thenReturn(null);

        service.heartbeat(3, "worker-3", "RUNNING", "task-3");

        ArgumentCaptor<AsyncCompensationPartitionRuntime> captor =
                ArgumentCaptor.forClass(AsyncCompensationPartitionRuntime.class);
        verify(runtimeRepository).insert(captor.capture());
        assertThat(captor.getValue().getPartitionNo()).isEqualTo(3);
        assertThat(captor.getValue().getWorkerId()).isEqualTo("worker-3");
        assertThat(captor.getValue().getWorkerStatus()).isEqualTo("RUNNING");
        assertThat(captor.getValue().getCurrentTaskId()).isEqualTo("task-3");
        assertThat(captor.getValue().getLastHeartbeatTs()).isNotNull();
    }

    @Test
    void shouldRecycleExpiredProcessingTasks() {
        AsyncCompensationSupervisorService service = buildService();
        AsyncCompensationTask task = new AsyncCompensationTask();
        task.setTaskId("task-expired-1");
        task.setTaskStatus("PROCESSING");
        task.setRetryCount(1);
        task.setMaxRetryCount(5);
        task.setLeaseOwner("worker-1");
        task.setLeaseExpireTs(LocalDateTime.now().minusMinutes(1));
        when(taskRepository.selectList(any())).thenReturn(List.of(task));

        int recycled = service.recycleExpiredLeases();

        assertThat(recycled).isEqualTo(1);
        ArgumentCaptor<AsyncCompensationTask> captor = ArgumentCaptor.forClass(AsyncCompensationTask.class);
        verify(taskRepository, times(1)).updateById(captor.capture());
        assertThat(captor.getValue().getTaskStatus()).isEqualTo("RETRY_WAIT");
        assertThat(captor.getValue().getLeaseOwner()).isNull();
        assertThat(captor.getValue().getLeaseExpireTs()).isNull();
        assertThat(captor.getValue().getNextRetryTs()).isNotNull();
    }

    @Test
    void shouldFindOfflinePartitionsByHeartbeatTimeout() {
        AsyncCompensationProperties properties = new AsyncCompensationProperties();
        properties.setLeaseSeconds(60);
        properties.setWorkerHeartbeatTimeoutSeconds(120);
        AsyncCompensationSupervisorService service = buildService(properties);
        AsyncCompensationPartitionRuntime expired = new AsyncCompensationPartitionRuntime();
        expired.setPartitionNo(2);
        expired.setLastHeartbeatTs(LocalDateTime.now().minusMinutes(5));
        AsyncCompensationPartitionRuntime active = new AsyncCompensationPartitionRuntime();
        active.setPartitionNo(3);
        active.setLastHeartbeatTs(LocalDateTime.now().minusSeconds(30));
        when(runtimeRepository.selectList(any())).thenReturn(List.of(expired, active));

        List<Integer> offlinePartitions = service.findOfflinePartitions();

        assertThat(offlinePartitions).containsExactly(2);
    }

    @Test
    void shouldCollectReadyTaskBacklogByPartition() {
        AsyncCompensationSupervisorService service = buildService();
        AsyncCompensationTask partitionOneInit = new AsyncCompensationTask();
        partitionOneInit.setPartitionNo(1);
        partitionOneInit.setTaskStatus("INIT");
        AsyncCompensationTask partitionOneRetry = new AsyncCompensationTask();
        partitionOneRetry.setPartitionNo(1);
        partitionOneRetry.setTaskStatus("RETRY_WAIT");
        partitionOneRetry.setNextRetryTs(LocalDateTime.now().minusSeconds(10));
        AsyncCompensationTask partitionTwoFuture = new AsyncCompensationTask();
        partitionTwoFuture.setPartitionNo(2);
        partitionTwoFuture.setTaskStatus("RETRY_WAIT");
        partitionTwoFuture.setNextRetryTs(LocalDateTime.now().plusMinutes(5));
        when(taskRepository.selectList(any())).thenReturn(List.of(partitionOneInit, partitionOneRetry, partitionTwoFuture));

        Map<Integer, Long> backlogs = service.collectReadyTaskBacklogByPartition();

        assertThat(backlogs).containsEntry(1, 2L);
        assertThat(backlogs).doesNotContainKey(2);
    }

    @Test
    void shouldCountDeadTasks() {
        AsyncCompensationSupervisorService service = buildService();
        AsyncCompensationTask deadOne = new AsyncCompensationTask();
        deadOne.setTaskStatus("DEAD");
        AsyncCompensationTask deadTwo = new AsyncCompensationTask();
        deadTwo.setTaskStatus("DEAD");
        AsyncCompensationTask success = new AsyncCompensationTask();
        success.setTaskStatus("SUCCESS");
        when(taskRepository.selectList(any())).thenReturn(List.of(deadOne, deadTwo, success));

        long deadTaskCount = service.countDeadTasks();

        assertThat(deadTaskCount).isEqualTo(2L);
    }

    private AsyncCompensationSupervisorService buildService() {
        AsyncCompensationProperties properties = new AsyncCompensationProperties();
        properties.setLeaseSeconds(60);
        properties.setWorkerHeartbeatTimeoutSeconds(120);
        return buildService(properties);
    }

    private AsyncCompensationSupervisorService buildService(AsyncCompensationProperties properties) {
        return new AsyncCompensationSupervisorServiceImpl(
                taskRepository,
                runtimeRepository,
                properties
        );
    }
}
