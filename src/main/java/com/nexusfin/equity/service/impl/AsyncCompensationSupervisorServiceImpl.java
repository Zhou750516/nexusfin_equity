package com.nexusfin.equity.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nexusfin.equity.config.AsyncCompensationProperties;
import com.nexusfin.equity.entity.AsyncCompensationPartitionRuntime;
import com.nexusfin.equity.entity.AsyncCompensationTask;
import com.nexusfin.equity.enums.AsyncCompensationTaskStatusEnum;
import com.nexusfin.equity.repository.AsyncCompensationPartitionRuntimeRepository;
import com.nexusfin.equity.repository.AsyncCompensationTaskRepository;
import com.nexusfin.equity.service.AsyncCompensationSupervisorService;
import com.nexusfin.equity.util.TraceIdUtil;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AsyncCompensationSupervisorServiceImpl implements AsyncCompensationSupervisorService {

    private static final Logger log = LoggerFactory.getLogger(AsyncCompensationSupervisorServiceImpl.class);

    private final AsyncCompensationTaskRepository taskRepository;
    private final AsyncCompensationPartitionRuntimeRepository runtimeRepository;
    private final AsyncCompensationProperties properties;

    public AsyncCompensationSupervisorServiceImpl(
            AsyncCompensationTaskRepository taskRepository,
            AsyncCompensationPartitionRuntimeRepository runtimeRepository,
            AsyncCompensationProperties properties
    ) {
        this.taskRepository = taskRepository;
        this.runtimeRepository = runtimeRepository;
        this.properties = properties;
    }

    @Override
    public void heartbeat(int partitionNo, String workerId, String workerStatus, String currentTaskId) {
        LocalDateTime now = LocalDateTime.now();
        AsyncCompensationPartitionRuntime runtime = runtimeRepository.selectById(partitionNo);
        if (runtime == null) {
            AsyncCompensationPartitionRuntime insert = new AsyncCompensationPartitionRuntime();
            insert.setPartitionNo(partitionNo);
            insert.setWorkerId(workerId);
            insert.setWorkerStatus(workerStatus);
            insert.setCurrentTaskId(currentTaskId);
            insert.setCurrentTaskStartedTs(currentTaskId == null ? null : now);
            insert.setLastHeartbeatTs(now);
            insert.setUpdatedTs(now);
            runtimeRepository.insert(insert);
            return;
        }
        runtime.setWorkerId(workerId);
        runtime.setWorkerStatus(workerStatus);
        runtime.setCurrentTaskId(currentTaskId);
        runtime.setCurrentTaskStartedTs(currentTaskId == null ? null : now);
        runtime.setLastHeartbeatTs(now);
        runtime.setUpdatedTs(now);
        runtimeRepository.updateById(runtime);
    }

    @Override
    public int recycleExpiredLeases() {
        LocalDateTime now = LocalDateTime.now();
        List<AsyncCompensationTask> expiredTasks = taskRepository.selectList(Wrappers.<AsyncCompensationTask>lambdaQuery()
                .eq(AsyncCompensationTask::getTaskStatus, AsyncCompensationTaskStatusEnum.PROCESSING.name())
                .isNotNull(AsyncCompensationTask::getLeaseExpireTs)
                .lt(AsyncCompensationTask::getLeaseExpireTs, now));
        for (AsyncCompensationTask task : expiredTasks) {
            AsyncCompensationTask update = new AsyncCompensationTask();
            update.setTaskId(task.getTaskId());
            update.setTaskStatus(AsyncCompensationTaskStatusEnum.RETRY_WAIT.name());
            update.setLeaseOwner(null);
            update.setLeaseExpireTs(null);
            update.setNextRetryTs(now.plusSeconds(properties.getRetryInitialDelaySeconds()));
            update.setUpdatedTs(now);
            taskRepository.updateById(update);
            log.warn("traceId={} bizOrderNo={} recycle expired async compensation lease taskId={}",
                    TraceIdUtil.getTraceId(), task.getBizOrderNo(), task.getTaskId());
        }
        return expiredTasks.size();
    }

    @Override
    public List<Integer> findOfflinePartitions() {
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(properties.getWorkerHeartbeatTimeoutSeconds());
        return runtimeRepository.selectList(Wrappers.<AsyncCompensationPartitionRuntime>lambdaQuery())
                .stream()
                .filter(runtime -> runtime.getLastHeartbeatTs() != null)
                .filter(runtime -> runtime.getLastHeartbeatTs().isBefore(threshold))
                .map(AsyncCompensationPartitionRuntime::getPartitionNo)
                .sorted()
                .toList();
    }

    @Override
    public Map<Integer, Long> collectReadyTaskBacklogByPartition() {
        LocalDateTime now = LocalDateTime.now();
        return taskRepository.selectList(Wrappers.<AsyncCompensationTask>lambdaQuery())
                .stream()
                .filter(task -> AsyncCompensationTaskStatusEnum.INIT.name().equals(task.getTaskStatus())
                        || AsyncCompensationTaskStatusEnum.RETRY_WAIT.name().equals(task.getTaskStatus()))
                .filter(task -> task.getNextRetryTs() == null || !task.getNextRetryTs().isAfter(now))
                .collect(Collectors.groupingBy(
                        AsyncCompensationTask::getPartitionNo,
                        Collectors.counting()
                ));
    }

    @Override
    public long countDeadTasks() {
        return taskRepository.selectList(Wrappers.<AsyncCompensationTask>lambdaQuery())
                .stream()
                .filter(task -> AsyncCompensationTaskStatusEnum.DEAD.name().equals(task.getTaskStatus()))
                .count();
    }
}
