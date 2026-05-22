package com.nexusfin.equity.service;

import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldLogTraceableFieldsWhenTaskEnqueued(CapturedOutput output) throws Exception {
        AsyncCompensationProperties properties = new AsyncCompensationProperties();
        properties.setPartitionCount(8);
        properties.setMaxRetryCount(5);
        AsyncCompensationEnqueueService service = new AsyncCompensationEnqueueServiceImpl(
                taskRepository,
                new AsyncCompensationPartitioner(properties),
                properties,
                new ObjectMapper()
        );

        service.enqueue(new AsyncCompensationEnqueueService.EnqueueCommand(
                "YUNKA_LOAN_APPLY_RETRY",
                "LOAN_APPLY:APP-20260418-001",
                "APP-20260418-001",
                "YUNKA",
                "/api/gateway/proxy",
                "POST",
                null,
                loanApplyRetry("LA-001", "APP-20260418-001", 2026041801)
        ));

        ArgumentCaptor<AsyncCompensationTask> captor = ArgumentCaptor.forClass(AsyncCompensationTask.class);
        verify(taskRepository).insert(captor.capture());
        assertThat(captor.getValue().getTaskStatus()).isEqualTo("INIT");
        assertThat(captor.getValue().getRetryCount()).isEqualTo(0);
        assertThat(captor.getValue().getMaxRetryCount()).isEqualTo(5);
        assertThat(captor.getValue().getPartitionNo()).isBetween(0, 7);
        assertThat(captor.getValue().getBizKey()).isEqualTo("LOAN_APPLY:APP-20260418-001");
        assertThat(captor.getValue().getRequestPayload())
                .contains("\"path\":\"/loan/apply\"")
                .contains("\"bizOrderNo\":\"APP-20260418-001\"")
                .contains("\"loanId\":2026041801")
                .contains("\"loanAmount\":300000")
                .contains("\"loanPeriod\":3")
                .contains("\"loanReason\":\"70006\"")
                .contains("\"basicInfo\"")
                .contains("\"imageInfo\"")
                .contains("\"back\":\"BACK\"");
        assertThat(output)
                .contains("traceId=")
                .contains("bizOrderNo=APP-20260418-001")
                .contains("taskId=")
                .contains("taskType=YUNKA_LOAN_APPLY_RETRY")
                .contains("bizKey=LOAN_APPLY:APP-20260418-001")
                .contains("targetCode=YUNKA")
                .contains("requestPath=/api/gateway/proxy")
                .contains("requestPayload={\"requestId\":\"LA-001\"")
                .contains("\"back\":{\"redacted\":\"IMAGE_BASE64\"")
                .doesNotContain("\"back\":\"BACK\"")
                .contains("async compensation task enqueued");
    }

    @Test
    void shouldPreserveLargeImageInfoPayloadWhenTaskEnqueued() throws Exception {
        AsyncCompensationProperties properties = new AsyncCompensationProperties();
        properties.setPartitionCount(8);
        properties.setMaxRetryCount(5);
        AsyncCompensationEnqueueService service = new AsyncCompensationEnqueueServiceImpl(
                taskRepository,
                new AsyncCompensationPartitioner(properties),
                properties,
                new ObjectMapper()
        );
        String largeBack = largeBase64("BACK", 70000);
        String largeFront = largeBase64("FRONT", 70000);
        String largeNature = largeBase64("NATURE", 70000);

        service.enqueue(new AsyncCompensationEnqueueService.EnqueueCommand(
                "YUNKA_LOAN_APPLY_RETRY",
                "LOAN_APPLY:APP-large-image",
                "APP-large-image",
                "YUNKA",
                "/api/gateway/proxy",
                "POST",
                null,
                largeLoanApplyRetry("LA-large-image", "APP-large-image", 2026041810,
                        largeBack, largeFront, largeNature)
        ));

        ArgumentCaptor<AsyncCompensationTask> captor = ArgumentCaptor.forClass(AsyncCompensationTask.class);
        verify(taskRepository).insert(captor.capture());
        String requestPayload = captor.getValue().getRequestPayload();
        assertThat(requestPayload.length()).isGreaterThan(65535);
        assertThat(requestPayload)
                .contains(largeBack)
                .contains(largeFront)
                .contains(largeNature)
                .contains("\"imageInfo\"")
                .contains("\"loanId\":2026041810");
    }

    @Test
    void shouldLogTraceableFieldsWhenDuplicateTaskIgnored(CapturedOutput output) throws Exception {
        AsyncCompensationProperties properties = new AsyncCompensationProperties();
        properties.setPartitionCount(8);
        properties.setMaxRetryCount(5);
        AsyncCompensationEnqueueService service = new AsyncCompensationEnqueueServiceImpl(
                taskRepository,
                new AsyncCompensationPartitioner(properties),
                properties,
                new ObjectMapper()
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
                loanApplyRetry("LA-002", "APP-20260418-duplicate", 2026041802)
        ))).doesNotThrowAnyException();

        verify(taskRepository).insert(any());
        assertThat(output)
                .contains("traceId=")
                .contains("bizOrderNo=APP-20260418-duplicate")
                .contains("taskType=YUNKA_LOAN_APPLY_RETRY")
                .contains("bizKey=LOAN_APPLY:APP-20260418-duplicate")
                .contains("targetCode=YUNKA")
                .contains("requestPath=/api/gateway/proxy")
                .contains("requestPayload={\"requestId\":\"LA-002\"")
                .contains("\"back\":{\"redacted\":\"IMAGE_BASE64\"")
                .doesNotContain("\"back\":\"BACK\"")
                .contains("errorNo=ASYNC_COMPENSATION_DUPLICATED")
                .contains("errorMsg=Async compensation task duplicated")
                .contains("async compensation task duplicated, ignored");
    }

    private AsyncCompensationEnqueuePayload.YunkaLoanApplyRetry loanApplyRetry(
            String requestId,
            String applicationId,
            Integer loanId
    ) throws Exception {
        return new AsyncCompensationEnqueuePayload.YunkaLoanApplyRetry(
                requestId,
                "/loan/apply",
                applicationId,
                "mem-001",
                "user-001",
                "BEN-001",
                applicationId,
                loanId,
                300000L,
                3,
                "acc_001",
                "13800138000",
                "310101199001011111",
                "张三",
                "70006",
                objectMapper.readTree("{\"monthlyIncome\":10000}"),
                objectMapper.readTree("{\"name\":\"张三\",\"idno\":\"310101199001011111\"}"),
                objectMapper.readTree("[{\"name\":\"李四\",\"phone\":\"13900139000\",\"relation\":\"80003\",\"sort\":1}]"),
                objectMapper.readTree("{\"occupation\":\"20001\"}"),
                objectMapper.readTree("{\"maritalStatus\":\"50002\"}"),
                objectMapper.readTree("[{\"back\":\"BACK\",\"front\":\"FRONT\",\"nature\":\"NATURE\",\"type\":\"back,front,nature\"}]")
        );
    }

    private AsyncCompensationEnqueuePayload.YunkaLoanApplyRetry largeLoanApplyRetry(
            String requestId,
            String applicationId,
            Integer loanId,
            String back,
            String front,
            String nature
    ) throws Exception {
        return new AsyncCompensationEnqueuePayload.YunkaLoanApplyRetry(
                requestId,
                "/loan/apply",
                applicationId,
                "mem-001",
                "user-001",
                "BEN-001",
                applicationId,
                loanId,
                300000L,
                3,
                "acc_001",
                "13800138000",
                "310101199001011111",
                "张三",
                "70006",
                objectMapper.readTree("{\"monthlyIncome\":10000}"),
                objectMapper.readTree("{\"name\":\"张三\",\"idno\":\"310101199001011111\"}"),
                objectMapper.readTree("[{\"name\":\"李四\",\"phone\":\"13900139000\",\"relation\":\"80003\",\"sort\":1}]"),
                objectMapper.readTree("{\"occupation\":\"20001\"}"),
                objectMapper.readTree("{\"maritalStatus\":\"50002\"}"),
                objectMapper.readTree("[{\"back\":\"" + back + "\",\"front\":\"" + front
                        + "\",\"nature\":\"" + nature + "\",\"type\":\"back,front,nature\"}]")
        );
    }

    private String largeBase64(String prefix, int size) {
        return prefix + "-" + "A".repeat(size) + "-" + prefix + "-END";
    }
}
