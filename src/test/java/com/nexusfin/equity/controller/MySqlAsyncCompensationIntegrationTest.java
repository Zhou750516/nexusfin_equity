package com.nexusfin.equity.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nexusfin.equity.entity.AsyncCompensationAttempt;
import com.nexusfin.equity.entity.AsyncCompensationPartitionRuntime;
import com.nexusfin.equity.entity.AsyncCompensationTask;
import com.nexusfin.equity.repository.AsyncCompensationAttemptRepository;
import com.nexusfin.equity.repository.AsyncCompensationPartitionRuntimeRepository;
import com.nexusfin.equity.repository.AsyncCompensationTaskRepository;
import com.nexusfin.equity.service.AsyncCompensationEnqueueService;
import com.nexusfin.equity.service.AsyncCompensationExecutor;
import com.nexusfin.equity.service.AsyncCompensationSupervisorService;
import com.nexusfin.equity.service.AsyncCompensationWorkerService;
import com.nexusfin.equity.service.impl.AsyncCompensationRouterExecutor;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("mysql-it")
@EnabledIfEnvironmentVariable(named = "MYSQL_IT_ENABLED", matches = "true")
class MySqlAsyncCompensationIntegrationTest {

    private static final String TASK_BIZ_KEY_PREFIX = "MYSQL_IT_ASYNC:";
    private static final String TASK_BIZ_ORDER_PREFIX = "MYSQL-IT-ASYNC-";
    private static final String WORKER_ID_PREFIX = "mysql-it-worker-";

    @Autowired
    private AsyncCompensationEnqueueService enqueueService;

    @Autowired
    private AsyncCompensationWorkerService workerService;

    @Autowired
    private AsyncCompensationSupervisorService supervisorService;

    @Autowired
    private AsyncCompensationTaskRepository taskRepository;

    @Autowired
    private AsyncCompensationAttemptRepository attemptRepository;

    @Autowired
    private AsyncCompensationPartitionRuntimeRepository runtimeRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private AsyncCompensationRouterExecutor routerExecutor;

    @BeforeEach
    void setUp() {
        alignAsyncCompensationSchema();
        cleanupAsyncTestData();
    }

    @Test
    void shouldPersistAndProcessAsyncCompensationTaskInMySql() {
        String uniqueId = UUID.randomUUID().toString().replace("-", "");
        String bizKey = TASK_BIZ_KEY_PREFIX + uniqueId;
        String bizOrderNo = TASK_BIZ_ORDER_PREFIX + uniqueId;
        enqueueService.enqueue(new AsyncCompensationEnqueueService.EnqueueCommand(
                "YUNKA_LOAN_APPLY_RETRY",
                bizKey,
                bizOrderNo,
                "YUNKA",
                "/api/gateway/proxy",
                "POST",
                null,
                "{\"path\":\"/loan/apply\",\"bizOrderNo\":\"%s\"}".formatted(bizOrderNo)
        ));

        AsyncCompensationTask task = taskRepository.selectOne(Wrappers.<AsyncCompensationTask>lambdaQuery()
                .eq(AsyncCompensationTask::getBizKey, bizKey)
                .last("limit 1"));
        assertThat(task).isNotNull();
        assertThat(task.getTaskStatus()).isEqualTo("INIT");

        when(routerExecutor.execute(any()))
                .thenReturn(new AsyncCompensationExecutor.ExecutionResult(
                        "{\"code\":0,\"message\":\"MYSQL_IT_SUCCESS\"}"
                ));

        boolean handled = workerService.processNext(WORKER_ID_PREFIX + task.getPartitionNo(), task.getPartitionNo());

        assertThat(handled).isTrue();
        AsyncCompensationTask updatedTask = taskRepository.selectById(task.getTaskId());
        assertThat(updatedTask.getTaskStatus()).isEqualTo("SUCCESS");
        assertThat(updatedTask.getSuccessTs()).isNotNull();
        assertThat(updatedTask.getResponsePayload()).contains("MYSQL_IT_SUCCESS");

        List<AsyncCompensationAttempt> attempts = attemptRepository.selectList(
                Wrappers.<AsyncCompensationAttempt>lambdaQuery()
                        .eq(AsyncCompensationAttempt::getTaskId, task.getTaskId())
        );
        assertThat(attempts).hasSize(1);
        assertThat(attempts.get(0).getResultStatus()).isEqualTo("SUCCESS");
        assertThat(attempts.get(0).getWorkerId()).isEqualTo(WORKER_ID_PREFIX + task.getPartitionNo());
    }

    @Test
    void shouldHeartbeatAndRecycleExpiredLeaseInMySql() {
        int partitionNo = Math.abs(UUID.randomUUID().hashCode() % 1000) + 1000;
        String workerId = WORKER_ID_PREFIX + partitionNo;
        supervisorService.heartbeat(partitionNo, workerId, "RUNNING", "task-current-" + partitionNo);

        AsyncCompensationPartitionRuntime runtime = runtimeRepository.selectById(partitionNo);
        assertThat(runtime).isNotNull();
        assertThat(runtime.getWorkerId()).isEqualTo(workerId);
        assertThat(runtime.getCurrentTaskId()).isEqualTo("task-current-" + partitionNo);

        AsyncCompensationTask task = new AsyncCompensationTask();
        task.setTaskId("mysql-it-task-" + partitionNo);
        task.setTaskType("YUNKA_LOAN_APPLY_RETRY");
        task.setBizKey(TASK_BIZ_KEY_PREFIX + "lease-" + partitionNo);
        task.setBizOrderNo(TASK_BIZ_ORDER_PREFIX + "lease-" + partitionNo);
        task.setPartitionNo(partitionNo);
        task.setTaskStatus("PROCESSING");
        task.setTargetCode("YUNKA");
        task.setRequestPath("/api/gateway/proxy");
        task.setHttpMethod("POST");
        task.setRequestPayload("{\"path\":\"/loan/apply\"}");
        task.setRetryCount(0);
        task.setMaxRetryCount(5);
        task.setLeaseOwner(workerId);
        task.setLeaseExpireTs(LocalDateTime.now().minusMinutes(2));
        task.setCreatedTs(LocalDateTime.now().minusMinutes(3));
        task.setUpdatedTs(LocalDateTime.now().minusMinutes(2));
        taskRepository.insert(task);

        int recycled = supervisorService.recycleExpiredLeases();

        assertThat(recycled).isGreaterThanOrEqualTo(1);
        AsyncCompensationTask recycledTask = taskRepository.selectById(task.getTaskId());
        assertThat(recycledTask.getTaskStatus()).isEqualTo("RETRY_WAIT");
        assertThat(recycledTask.getLeaseOwner()).isNull();
        assertThat(recycledTask.getLeaseExpireTs()).isNull();
        assertThat(recycledTask.getNextRetryTs()).isNotNull();
    }

    @Test
    void shouldCollectBacklogAndDeadTasksInMySql() {
        int readyPartition = Math.abs(UUID.randomUUID().hashCode() % 1000) + 2000;
        insertTask("mysql-it-init-" + readyPartition, readyPartition, "INIT", null, null);
        insertTask("mysql-it-retry-ready-" + readyPartition, readyPartition, "RETRY_WAIT", LocalDateTime.now().minusMinutes(1), null);
        insertTask("mysql-it-retry-future-" + (readyPartition + 1), readyPartition + 1, "RETRY_WAIT", LocalDateTime.now().plusMinutes(5), null);
        insertTask("mysql-it-dead-" + (readyPartition + 2), readyPartition + 2, "DEAD", null, null);

        Map<Integer, Long> backlog = supervisorService.collectReadyTaskBacklogByPartition();
        long deadCount = supervisorService.countDeadTasks();

        assertThat(backlog).containsEntry(readyPartition, 2L);
        assertThat(backlog).doesNotContainKey(readyPartition + 1);
        assertThat(deadCount).isGreaterThanOrEqualTo(1L);
    }

    private void insertTask(
            String taskId,
            int partitionNo,
            String status,
            LocalDateTime nextRetryTs,
            LocalDateTime leaseExpireTs
    ) {
        AsyncCompensationTask task = new AsyncCompensationTask();
        task.setTaskId(taskId);
        task.setTaskType("YUNKA_LOAN_APPLY_RETRY");
        task.setBizKey(TASK_BIZ_KEY_PREFIX + taskId);
        task.setBizOrderNo(TASK_BIZ_ORDER_PREFIX + taskId);
        task.setPartitionNo(partitionNo);
        task.setTaskStatus(status);
        task.setTargetCode("YUNKA");
        task.setRequestPath("/api/gateway/proxy");
        task.setHttpMethod("POST");
        task.setRequestPayload("{\"path\":\"/loan/apply\"}");
        task.setRetryCount(0);
        task.setMaxRetryCount(5);
        task.setNextRetryTs(nextRetryTs);
        task.setLeaseExpireTs(leaseExpireTs);
        task.setCreatedTs(LocalDateTime.now().minusMinutes(2));
        task.setUpdatedTs(LocalDateTime.now().minusMinutes(1));
        taskRepository.insert(task);
    }

    private void cleanupAsyncTestData() {
        List<AsyncCompensationTask> tasks = taskRepository.selectList(Wrappers.<AsyncCompensationTask>lambdaQuery()
                .likeRight(AsyncCompensationTask::getBizKey, TASK_BIZ_KEY_PREFIX));
        List<String> taskIds = tasks.stream().map(AsyncCompensationTask::getTaskId).toList();
        if (!taskIds.isEmpty()) {
            attemptRepository.delete(Wrappers.<AsyncCompensationAttempt>lambdaQuery()
                    .in(AsyncCompensationAttempt::getTaskId, taskIds));
            taskRepository.delete(Wrappers.<AsyncCompensationTask>lambdaQuery()
                    .in(AsyncCompensationTask::getTaskId, taskIds));
        }
        runtimeRepository.delete(Wrappers.<AsyncCompensationPartitionRuntime>lambdaQuery()
                .likeRight(AsyncCompensationPartitionRuntime::getWorkerId, WORKER_ID_PREFIX));
    }

    private void alignAsyncCompensationSchema() {
        jdbcTemplate.execute((Connection connection) -> {
            DatabaseMetaData metaData = connection.getMetaData();
            if (!hasTable(metaData, "async_compensation_task")) {
                jdbcTemplate.execute("""
                        CREATE TABLE async_compensation_task (
                            task_id VARCHAR(64) PRIMARY KEY,
                            task_type VARCHAR(64) NOT NULL,
                            biz_key VARCHAR(128) NOT NULL,
                            biz_order_no VARCHAR(64) NOT NULL,
                            partition_no INT NOT NULL,
                            task_status VARCHAR(32) NOT NULL,
                            target_code VARCHAR(32) NOT NULL,
                            request_path VARCHAR(256) NOT NULL,
                            http_method VARCHAR(16) NOT NULL,
                            request_headers TEXT NULL,
                            request_payload TEXT NOT NULL,
                            response_payload TEXT NULL,
                            retry_count INT NOT NULL,
                            max_retry_count INT NOT NULL,
                            next_retry_ts TIMESTAMP NULL,
                            last_error_code VARCHAR(64) NULL,
                            last_error_message VARCHAR(512) NULL,
                            lease_owner VARCHAR(64) NULL,
                            lease_expire_ts TIMESTAMP NULL,
                            success_ts TIMESTAMP NULL,
                            created_ts TIMESTAMP NOT NULL,
                            updated_ts TIMESTAMP NOT NULL,
                            UNIQUE KEY uk_task_type_biz_key (task_type, biz_key),
                            KEY idx_partition_status_next_retry (partition_no, task_status, next_retry_ts)
                        )
                        """);
            }
            if (!hasTable(metaData, "async_compensation_attempt")) {
                jdbcTemplate.execute("""
                        CREATE TABLE async_compensation_attempt (
                            attempt_id VARCHAR(64) PRIMARY KEY,
                            task_id VARCHAR(64) NOT NULL,
                            task_type VARCHAR(64) NOT NULL,
                            partition_no INT NOT NULL,
                            worker_id VARCHAR(64) NULL,
                            attempt_no INT NOT NULL,
                            request_payload TEXT NULL,
                            response_payload TEXT NULL,
                            result_status VARCHAR(32) NOT NULL,
                            error_code VARCHAR(64) NULL,
                            error_message VARCHAR(512) NULL,
                            started_ts TIMESTAMP NOT NULL,
                            finished_ts TIMESTAMP NULL,
                            KEY idx_attempt_task_id (task_id)
                        )
                        """);
            }
            if (!hasTable(metaData, "async_compensation_partition_runtime")) {
                jdbcTemplate.execute("""
                        CREATE TABLE async_compensation_partition_runtime (
                            partition_no INT PRIMARY KEY,
                            worker_id VARCHAR(64) NULL,
                            worker_status VARCHAR(32) NULL,
                            current_task_id VARCHAR(64) NULL,
                            current_task_started_ts TIMESTAMP NULL,
                            last_heartbeat_ts TIMESTAMP NULL,
                            updated_ts TIMESTAMP NULL
                        )
                        """);
            }
            return null;
        });
    }

    private boolean hasTable(DatabaseMetaData metaData, String tableName) throws SQLException {
        try (ResultSet tables = metaData.getTables(null, null, tableName, null)) {
            return tables.next();
        }
    }
}
