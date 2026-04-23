package com.nexusfin.equity.service;

import com.nexusfin.equity.config.AsyncCompensationProperties;
import com.nexusfin.equity.entity.AsyncCompensationAttempt;
import com.nexusfin.equity.entity.AsyncCompensationTask;
import com.nexusfin.equity.exception.BizException;
import com.nexusfin.equity.repository.AsyncCompensationAttemptRepository;
import com.nexusfin.equity.repository.AsyncCompensationTaskRepository;
import com.nexusfin.equity.service.impl.AsyncCompensationRouterExecutor;
import com.nexusfin.equity.service.impl.AsyncCompensationWorkerServiceImpl;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class AsyncCompensationWorkerServiceTest {

    @Mock
    private AsyncCompensationTaskRepository taskRepository;

    @Mock
    private AsyncCompensationAttemptRepository attemptRepository;

    @Mock
    private AsyncCompensationRouterExecutor routerExecutor;

    @Test
    void shouldReturnFalseWhenNoTaskAvailable() {
        AsyncCompensationWorkerService service = buildService();
        when(taskRepository.selectOne(any())).thenReturn(null);

        boolean handled = service.processNext("worker-0", 0);

        assertThat(handled).isFalse();
        verify(taskRepository).selectOne(any());
        verify(taskRepository, never()).updateById(any());
        verify(attemptRepository, never()).insert(any());
    }

    @Test
    void shouldLogTraceableFieldsWhenTaskSucceeded(CapturedOutput output) {
        AsyncCompensationWorkerService service = buildService();
        AsyncCompensationTask task = buildTask("task-success", "INIT", 0, 5);
        when(taskRepository.selectOne(any())).thenReturn(task);
        when(routerExecutor.execute(any()))
                .thenReturn(new AsyncCompensationExecutor.ExecutionResult("{\"code\":0,\"message\":\"SUCCESS\"}"));

        boolean handled = service.processNext("worker-3", 3);

        assertThat(handled).isTrue();
        verify(routerExecutor).execute(task);

        ArgumentCaptor<AsyncCompensationTask> taskCaptor = ArgumentCaptor.forClass(AsyncCompensationTask.class);
        verify(taskRepository, times(2)).updateById(taskCaptor.capture());
        List<AsyncCompensationTask> updates = taskCaptor.getAllValues();
        assertThat(updates.get(0).getTaskStatus()).isEqualTo("PROCESSING");
        assertThat(updates.get(0).getLeaseOwner()).isEqualTo("worker-3");
        assertThat(updates.get(0).getLeaseExpireTs()).isNotNull();
        assertThat(updates.get(1).getTaskStatus()).isEqualTo("SUCCESS");
        assertThat(updates.get(1).getLeaseOwner()).isNull();
        assertThat(updates.get(1).getLeaseExpireTs()).isNull();
        assertThat(updates.get(1).getSuccessTs()).isNotNull();
        assertThat(updates.get(1).getResponsePayload()).contains("SUCCESS");

        ArgumentCaptor<AsyncCompensationAttempt> attemptCaptor = ArgumentCaptor.forClass(AsyncCompensationAttempt.class);
        verify(attemptRepository).insert(attemptCaptor.capture());
        assertThat(attemptCaptor.getValue().getTaskId()).isEqualTo("task-success");
        assertThat(attemptCaptor.getValue().getAttemptNo()).isEqualTo(1);
        assertThat(attemptCaptor.getValue().getResultStatus()).isEqualTo("SUCCESS");
        assertThat(attemptCaptor.getValue().getResponsePayload()).contains("SUCCESS");
        assertThat(output)
                .contains("traceId=")
                .contains("bizOrderNo=APP-task-success")
                .contains("taskId=task-success")
                .contains("taskType=YUNKA_LOAN_APPLY_RETRY")
                .contains("partitionNo=3")
                .contains("workerId=worker-3")
                .contains("requestPath=/api/gateway/proxy")
                .contains("async compensation task claimed")
                .contains("async compensation task succeeded");
    }

    @Test
    void shouldLogTraceableFieldsWhenTaskFailsBeforeMaxRetry(CapturedOutput output) {
        AsyncCompensationWorkerService service = buildService();
        AsyncCompensationTask task = buildTask("task-retry", "INIT", 0, 2);
        when(taskRepository.selectOne(any())).thenReturn(task);
        when(routerExecutor.execute(any()))
                .thenThrow(new BizException("YUNKA_RETRY_FAILED", "gateway unavailable"));

        boolean handled = service.processNext("worker-5", 5);

        assertThat(handled).isTrue();
        ArgumentCaptor<AsyncCompensationTask> taskCaptor = ArgumentCaptor.forClass(AsyncCompensationTask.class);
        verify(taskRepository, times(2)).updateById(taskCaptor.capture());
        AsyncCompensationTask retryUpdate = taskCaptor.getAllValues().get(1);
        assertThat(retryUpdate.getTaskStatus()).isEqualTo("RETRY_WAIT");
        assertThat(retryUpdate.getRetryCount()).isEqualTo(1);
        assertThat(retryUpdate.getNextRetryTs()).isNotNull();
        assertThat(retryUpdate.getLeaseOwner()).isNull();
        assertThat(retryUpdate.getLeaseExpireTs()).isNull();
        assertThat(retryUpdate.getLastErrorMessage()).contains("gateway unavailable");

        ArgumentCaptor<AsyncCompensationAttempt> attemptCaptor = ArgumentCaptor.forClass(AsyncCompensationAttempt.class);
        verify(attemptRepository).insert(attemptCaptor.capture());
        assertThat(attemptCaptor.getValue().getAttemptNo()).isEqualTo(1);
        assertThat(attemptCaptor.getValue().getResultStatus()).isEqualTo("FAILED");
        assertThat(attemptCaptor.getValue().getErrorMessage()).contains("gateway unavailable");
        assertThat(output)
                .contains("traceId=")
                .contains("bizOrderNo=APP-task-retry")
                .contains("taskId=task-retry")
                .contains("taskType=YUNKA_LOAN_APPLY_RETRY")
                .contains("partitionNo=3")
                .contains("workerId=worker-5")
                .contains("requestPath=/api/gateway/proxy")
                .contains("errorNo=YUNKA_RETRY_FAILED")
                .contains("errorMsg=gateway unavailable")
                .contains("dead=false")
                .contains("async compensation task failed");
    }

    @Test
    void shouldCapRetryDelayWhenExponentialBackoffExceedsMaxInterval() {
        AsyncCompensationProperties properties = new AsyncCompensationProperties();
        properties.setLeaseSeconds(60);
        properties.setRetryInitialDelaySeconds(10);
        properties.setRetryMaxDelaySeconds(30);
        AsyncCompensationWorkerService service = buildService(properties);
        AsyncCompensationTask task = buildTask("task-backoff", "RETRY_WAIT", 2, 5);
        when(taskRepository.selectOne(any())).thenReturn(task);
        when(routerExecutor.execute(any()))
                .thenThrow(new IllegalStateException("temporary upstream failure"));
        LocalDateTime lowerBound = LocalDateTime.now().plusSeconds(25);
        LocalDateTime upperBound = LocalDateTime.now().plusSeconds(35);

        boolean handled = service.processNext("worker-7", 7);

        assertThat(handled).isTrue();
        ArgumentCaptor<AsyncCompensationTask> taskCaptor = ArgumentCaptor.forClass(AsyncCompensationTask.class);
        verify(taskRepository, times(2)).updateById(taskCaptor.capture());
        AsyncCompensationTask retryUpdate = taskCaptor.getAllValues().get(1);
        assertThat(retryUpdate.getTaskStatus()).isEqualTo("RETRY_WAIT");
        assertThat(retryUpdate.getRetryCount()).isEqualTo(3);
        assertThat(retryUpdate.getNextRetryTs()).isAfterOrEqualTo(lowerBound);
        assertThat(retryUpdate.getNextRetryTs()).isBeforeOrEqualTo(upperBound);
    }

    @Test
    void shouldLogTraceableFieldsWhenTaskReachesDead(CapturedOutput output) {
        AsyncCompensationWorkerService service = buildService();
        AsyncCompensationTask task = buildTask("task-dead", "RETRY_WAIT", 1, 2);
        when(taskRepository.selectOne(any())).thenReturn(task);
        when(routerExecutor.execute(any()))
                .thenThrow(new IllegalStateException("upstream still failing"));

        boolean handled = service.processNext("worker-6", 6);

        assertThat(handled).isTrue();
        ArgumentCaptor<AsyncCompensationTask> taskCaptor = ArgumentCaptor.forClass(AsyncCompensationTask.class);
        verify(taskRepository, times(2)).updateById(taskCaptor.capture());
        AsyncCompensationTask deadUpdate = taskCaptor.getAllValues().get(1);
        assertThat(deadUpdate.getTaskStatus()).isEqualTo("DEAD");
        assertThat(deadUpdate.getRetryCount()).isEqualTo(2);
        assertThat(deadUpdate.getNextRetryTs()).isNull();
        assertThat(deadUpdate.getLeaseOwner()).isNull();
        assertThat(deadUpdate.getLastErrorMessage()).contains("upstream still failing");
        assertThat(output)
                .contains("traceId=")
                .contains("bizOrderNo=APP-task-dead")
                .contains("taskId=task-dead")
                .contains("taskType=YUNKA_LOAN_APPLY_RETRY")
                .contains("partitionNo=3")
                .contains("workerId=worker-6")
                .contains("requestPath=/api/gateway/proxy")
                .contains("errorNo=IllegalStateException")
                .contains("errorMsg=upstream still failing")
                .contains("dead=true")
                .contains("async compensation task failed");
    }

    private AsyncCompensationWorkerService buildService() {
        AsyncCompensationProperties properties = new AsyncCompensationProperties();
        properties.setLeaseSeconds(60);
        properties.setRetryInitialDelaySeconds(60);
        properties.setRetryMaxDelaySeconds(900);
        return buildService(properties);
    }

    private AsyncCompensationWorkerService buildService(AsyncCompensationProperties properties) {
        return new AsyncCompensationWorkerServiceImpl(
                taskRepository,
                attemptRepository,
                routerExecutor,
                properties
        );
    }

    private AsyncCompensationTask buildTask(String taskId, String status, int retryCount, int maxRetryCount) {
        AsyncCompensationTask task = new AsyncCompensationTask();
        task.setTaskId(taskId);
        task.setTaskType("YUNKA_LOAN_APPLY_RETRY");
        task.setBizKey("LOAN_APPLY:" + taskId);
        task.setBizOrderNo("APP-" + taskId);
        task.setPartitionNo(3);
        task.setTaskStatus(status);
        task.setTargetCode("YUNKA");
        task.setRequestPath("/api/gateway/proxy");
        task.setHttpMethod("POST");
        task.setRequestPayload("{\"path\":\"/loan/apply\"}");
        task.setRetryCount(retryCount);
        task.setMaxRetryCount(maxRetryCount);
        task.setCreatedTs(LocalDateTime.now().minusMinutes(2));
        task.setUpdatedTs(LocalDateTime.now().minusMinutes(2));
        return task;
    }
}
