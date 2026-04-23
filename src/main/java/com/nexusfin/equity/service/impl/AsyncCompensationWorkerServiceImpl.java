package com.nexusfin.equity.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nexusfin.equity.config.AsyncCompensationProperties;
import com.nexusfin.equity.entity.AsyncCompensationAttempt;
import com.nexusfin.equity.entity.AsyncCompensationTask;
import com.nexusfin.equity.enums.AsyncCompensationTaskStatusEnum;
import com.nexusfin.equity.exception.BizException;
import com.nexusfin.equity.repository.AsyncCompensationAttemptRepository;
import com.nexusfin.equity.repository.AsyncCompensationTaskRepository;
import com.nexusfin.equity.service.AsyncCompensationExecutor;
import com.nexusfin.equity.service.AsyncCompensationWorkerService;
import com.nexusfin.equity.util.RequestIdUtil;
import com.nexusfin.equity.util.TraceIdUtil;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AsyncCompensationWorkerServiceImpl implements AsyncCompensationWorkerService {

    private static final Logger log = LoggerFactory.getLogger(AsyncCompensationWorkerServiceImpl.class);

    private final AsyncCompensationTaskRepository taskRepository;
    private final AsyncCompensationAttemptRepository attemptRepository;
    private final AsyncCompensationRouterExecutor routerExecutor;
    private final AsyncCompensationProperties properties;

    public AsyncCompensationWorkerServiceImpl(
            AsyncCompensationTaskRepository taskRepository,
            AsyncCompensationAttemptRepository attemptRepository,
            AsyncCompensationRouterExecutor routerExecutor,
            AsyncCompensationProperties properties
    ) {
        this.taskRepository = taskRepository;
        this.attemptRepository = attemptRepository;
        this.routerExecutor = routerExecutor;
        this.properties = properties;
    }

    @Override
    public WorkerProcessResult processNext(
            String workerId,
            int partitionNo,
            WorkerLifecycleListener lifecycleListener
    ) {
        LocalDateTime now = LocalDateTime.now();
        AsyncCompensationTask task = taskRepository.selectOne(Wrappers.<AsyncCompensationTask>lambdaQuery()
                .eq(AsyncCompensationTask::getPartitionNo, partitionNo)
                .in(AsyncCompensationTask::getTaskStatus,
                        AsyncCompensationTaskStatusEnum.INIT.name(),
                        AsyncCompensationTaskStatusEnum.RETRY_WAIT.name())
                .and(query -> query.isNull(AsyncCompensationTask::getNextRetryTs)
                .or()
                        .le(AsyncCompensationTask::getNextRetryTs, now))
                .orderByAsc(AsyncCompensationTask::getCreatedTs)
                .last("limit 1"));
        if (task == null) {
            return WorkerProcessResult.none();
        }

        LocalDateTime leaseExpireTs = now.plusSeconds(properties.getLeaseSeconds());
        taskRepository.updateById(buildProcessingUpdate(task.getTaskId(), workerId, leaseExpireTs, now));
        lifecycleListener.onTaskClaimed(task.getTaskId());
        log.info("traceId={} bizOrderNo={} taskId={} taskType={} partitionNo={} workerId={} requestPath={} "
                        + "async compensation task claimed",
                TraceIdUtil.getTraceId(),
                task.getBizOrderNo(),
                task.getTaskId(),
                task.getTaskType(),
                task.getPartitionNo(),
                workerId,
                task.getRequestPath());

        int attemptNo = task.getRetryCount() + 1;
        LocalDateTime startedTs = LocalDateTime.now();
        try {
            AsyncCompensationExecutor.ExecutionResult result = routerExecutor.execute(task);
            LocalDateTime finishedTs = LocalDateTime.now();
            taskRepository.updateById(buildSuccessUpdate(task.getTaskId(), result.responsePayload(), finishedTs));
            attemptRepository.insert(buildAttempt(task, workerId, attemptNo, result.responsePayload(),
                    "SUCCESS", null, null, startedTs, finishedTs));
            log.info("traceId={} bizOrderNo={} taskId={} taskType={} partitionNo={} workerId={} requestPath={} "
                            + "async compensation task succeeded",
                    TraceIdUtil.getTraceId(),
                    task.getBizOrderNo(),
                    task.getTaskId(),
                    task.getTaskType(),
                    task.getPartitionNo(),
                    workerId,
                    task.getRequestPath());
            return WorkerProcessResult.handled(task.getTaskId());
        } catch (RuntimeException exception) {
            LocalDateTime finishedTs = LocalDateTime.now();
            int nextRetryCount = task.getRetryCount() + 1;
            boolean dead = nextRetryCount >= task.getMaxRetryCount();
            taskRepository.updateById(buildFailureUpdate(task.getTaskId(), nextRetryCount, dead, finishedTs, exception));
            attemptRepository.insert(buildAttempt(task, workerId, attemptNo, null,
                    "FAILED", resolveErrorNo(exception), resolveErrorMsg(exception), startedTs, finishedTs));
            logFailure(task, workerId, dead, exception);
            return WorkerProcessResult.handled(task.getTaskId());
        }
    }

    private AsyncCompensationTask buildProcessingUpdate(
            String taskId,
            String workerId,
            LocalDateTime leaseExpireTs,
            LocalDateTime now
    ) {
        AsyncCompensationTask update = new AsyncCompensationTask();
        update.setTaskId(taskId);
        update.setTaskStatus(AsyncCompensationTaskStatusEnum.PROCESSING.name());
        update.setLeaseOwner(workerId);
        update.setLeaseExpireTs(leaseExpireTs);
        update.setUpdatedTs(now);
        return update;
    }

    private AsyncCompensationTask buildSuccessUpdate(String taskId, String responsePayload, LocalDateTime now) {
        AsyncCompensationTask update = new AsyncCompensationTask();
        update.setTaskId(taskId);
        update.setTaskStatus(AsyncCompensationTaskStatusEnum.SUCCESS.name());
        update.setResponsePayload(responsePayload);
        update.setLeaseOwner(null);
        update.setLeaseExpireTs(null);
        update.setNextRetryTs(null);
        update.setSuccessTs(now);
        update.setUpdatedTs(now);
        return update;
    }

    private AsyncCompensationTask buildFailureUpdate(
            String taskId,
            int retryCount,
            boolean dead,
            LocalDateTime now,
            RuntimeException exception
    ) {
        AsyncCompensationTask update = new AsyncCompensationTask();
        update.setTaskId(taskId);
        update.setTaskStatus(dead
                ? AsyncCompensationTaskStatusEnum.DEAD.name()
                : AsyncCompensationTaskStatusEnum.RETRY_WAIT.name());
        update.setRetryCount(retryCount);
        update.setNextRetryTs(dead ? null : now.plusSeconds(properties.nextRetryDelaySeconds(retryCount)));
        update.setLeaseOwner(null);
        update.setLeaseExpireTs(null);
        update.setLastErrorCode(resolveErrorNo(exception));
        update.setLastErrorMessage(resolveErrorMsg(exception));
        update.setUpdatedTs(now);
        return update;
    }

    private AsyncCompensationAttempt buildAttempt(
            AsyncCompensationTask task,
            String workerId,
            int attemptNo,
            String responsePayload,
            String resultStatus,
            String errorCode,
            String errorMessage,
            LocalDateTime startedTs,
            LocalDateTime finishedTs
    ) {
        AsyncCompensationAttempt attempt = new AsyncCompensationAttempt();
        attempt.setAttemptId(RequestIdUtil.nextId("aca"));
        attempt.setTaskId(task.getTaskId());
        attempt.setTaskType(task.getTaskType());
        attempt.setPartitionNo(task.getPartitionNo());
        attempt.setWorkerId(workerId);
        attempt.setAttemptNo(attemptNo);
        attempt.setRequestPayload(task.getRequestPayload());
        attempt.setResponsePayload(responsePayload);
        attempt.setResultStatus(resultStatus);
        attempt.setErrorCode(errorCode);
        attempt.setErrorMessage(errorMessage);
        attempt.setStartedTs(startedTs);
        attempt.setFinishedTs(finishedTs);
        return attempt;
    }

    private void logFailure(AsyncCompensationTask task, String workerId, boolean dead, RuntimeException exception) {
        if (dead) {
            log.error("traceId={} bizOrderNo={} taskId={} taskType={} partitionNo={} workerId={} requestPath={} "
                            + "errorNo={} errorMsg={} dead={} async compensation task failed",
                    TraceIdUtil.getTraceId(),
                    task.getBizOrderNo(),
                    task.getTaskId(),
                    task.getTaskType(),
                    task.getPartitionNo(),
                    workerId,
                    task.getRequestPath(),
                    resolveErrorNo(exception),
                    resolveErrorMsg(exception),
                    true);
            return;
        }
        log.warn("traceId={} bizOrderNo={} taskId={} taskType={} partitionNo={} workerId={} requestPath={} "
                        + "errorNo={} errorMsg={} dead={} async compensation task failed",
                TraceIdUtil.getTraceId(),
                task.getBizOrderNo(),
                task.getTaskId(),
                task.getTaskType(),
                task.getPartitionNo(),
                workerId,
                task.getRequestPath(),
                resolveErrorNo(exception),
                resolveErrorMsg(exception),
                false);
    }

    private String resolveErrorNo(RuntimeException exception) {
        if (exception instanceof BizException bizException) {
            return bizException.getErrorNo();
        }
        return exception.getClass().getSimpleName();
    }

    private String resolveErrorMsg(RuntimeException exception) {
        if (exception instanceof BizException bizException) {
            return bizException.getErrorMsg();
        }
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }
}
