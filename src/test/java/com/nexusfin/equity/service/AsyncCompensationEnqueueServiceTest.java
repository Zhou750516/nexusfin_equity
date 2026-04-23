package com.nexusfin.equity.service;

import com.nexusfin.equity.config.AsyncCompensationProperties;
import com.nexusfin.equity.entity.AsyncCompensationTask;
import com.nexusfin.equity.repository.AsyncCompensationTaskRepository;
import com.nexusfin.equity.service.impl.AsyncCompensationEnqueueServiceImpl;
import com.nexusfin.equity.util.AsyncCompensationPartitioner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.dao.DuplicateKeyException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class AsyncCompensationEnqueueServiceTest {

    @Mock
    private AsyncCompensationTaskRepository taskRepository;

    @Test
    void shouldLogTraceableFieldsWhenTaskEnqueued(CapturedOutput output) {
        AsyncCompensationProperties properties = new AsyncCompensationProperties();
        properties.setPartitionCount(8);
        properties.setMaxRetryCount(5);
        AsyncCompensationEnqueueService service = new AsyncCompensationEnqueueServiceImpl(
                taskRepository,
                new AsyncCompensationPartitioner(properties),
                properties
        );

        service.enqueue(new AsyncCompensationEnqueueService.EnqueueCommand(
                "YUNKA_LOAN_APPLY_RETRY",
                "LOAN_APPLY:APP-20260418-001",
                "APP-20260418-001",
                "YUNKA",
                "/api/gateway/proxy",
                "POST",
                null,
                "{\"path\":\"/loan/apply\"}"
        ));

        ArgumentCaptor<AsyncCompensationTask> captor = ArgumentCaptor.forClass(AsyncCompensationTask.class);
        verify(taskRepository).insert(captor.capture());
        assertThat(captor.getValue().getTaskStatus()).isEqualTo("INIT");
        assertThat(captor.getValue().getRetryCount()).isEqualTo(0);
        assertThat(captor.getValue().getMaxRetryCount()).isEqualTo(5);
        assertThat(captor.getValue().getPartitionNo()).isBetween(0, 7);
        assertThat(captor.getValue().getBizKey()).isEqualTo("LOAN_APPLY:APP-20260418-001");
        assertThat(output)
                .contains("traceId=")
                .contains("bizOrderNo=APP-20260418-001")
                .contains("taskId=")
                .contains("taskType=YUNKA_LOAN_APPLY_RETRY")
                .contains("bizKey=LOAN_APPLY:APP-20260418-001")
                .contains("targetCode=YUNKA")
                .contains("requestPath=/api/gateway/proxy")
                .contains("async compensation task enqueued");
    }

    @Test
    void shouldLogTraceableFieldsWhenDuplicateTaskIgnored(CapturedOutput output) {
        AsyncCompensationProperties properties = new AsyncCompensationProperties();
        properties.setPartitionCount(8);
        properties.setMaxRetryCount(5);
        AsyncCompensationEnqueueService service = new AsyncCompensationEnqueueServiceImpl(
                taskRepository,
                new AsyncCompensationPartitioner(properties),
                properties
        );
        doThrow(new DuplicateKeyException("duplicate task")).when(taskRepository).insert(any());

        assertThatCode(() -> service.enqueue(new AsyncCompensationEnqueueService.EnqueueCommand(
                "YUNKA_LOAN_APPLY_RETRY",
                "LOAN_APPLY:APP-20260418-duplicate",
                "APP-20260418-duplicate",
                "YUNKA",
                "/api/gateway/proxy",
                "POST",
                null,
                "{\"path\":\"/loan/apply\"}"
        ))).doesNotThrowAnyException();

        verify(taskRepository).insert(any());
        assertThat(output)
                .contains("traceId=")
                .contains("bizOrderNo=APP-20260418-duplicate")
                .contains("taskType=YUNKA_LOAN_APPLY_RETRY")
                .contains("bizKey=LOAN_APPLY:APP-20260418-duplicate")
                .contains("targetCode=YUNKA")
                .contains("requestPath=/api/gateway/proxy")
                .contains("errorNo=ASYNC_COMPENSATION_DUPLICATED")
                .contains("errorMsg=Async compensation task duplicated")
                .contains("async compensation task duplicated, ignored");
    }
}
