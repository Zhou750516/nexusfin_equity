package com.nexusfin.equity.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusfin.equity.config.AsyncCompensationProperties;
import com.nexusfin.equity.entity.AsyncCompensationTask;
import com.nexusfin.equity.enums.AsyncCompensationTaskStatusEnum;
import com.nexusfin.equity.exception.BizException;
import com.nexusfin.equity.exception.ErrorCodes;
import com.nexusfin.equity.repository.AsyncCompensationTaskRepository;
import com.nexusfin.equity.service.AsyncCompensationEnqueuePayload;
import com.nexusfin.equity.service.AsyncCompensationEnqueueService;
import com.nexusfin.equity.util.AsyncCompensationPartitioner;
import com.nexusfin.equity.util.RequestIdUtil;
import com.nexusfin.equity.util.TraceIdUtil;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.dao.DuplicateKeyException;

@Service
public class AsyncCompensationEnqueueServiceImpl implements AsyncCompensationEnqueueService {

    private static final Logger log = LoggerFactory.getLogger(AsyncCompensationEnqueueServiceImpl.class);

    private final AsyncCompensationTaskRepository taskRepository;
    private final AsyncCompensationPartitioner partitioner;
    private final AsyncCompensationProperties properties;
    private final ObjectMapper objectMapper;

    public AsyncCompensationEnqueueServiceImpl(
            AsyncCompensationTaskRepository taskRepository,
            AsyncCompensationPartitioner partitioner,
            AsyncCompensationProperties properties,
            ObjectMapper objectMapper
    ) {
        this.taskRepository = taskRepository;
        this.partitioner = partitioner;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public void enqueue(EnqueueCommand command) {
        LocalDateTime now = LocalDateTime.now();
        AsyncCompensationTask task = new AsyncCompensationTask();
        task.setTaskId(RequestIdUtil.nextId("ac"));
        task.setTaskType(command.taskType());
        task.setBizKey(command.bizKey());
        task.setBizOrderNo(command.bizOrderNo());
        task.setPartitionNo(partitioner.partitionOf(command.bizKey()));
        task.setTaskStatus(AsyncCompensationTaskStatusEnum.INIT.name());
        task.setTargetCode(command.targetCode());
        task.setRequestPath(command.requestPath());
        task.setHttpMethod(command.httpMethod());
        task.setRequestHeaders(command.requestHeaders());
        task.setRequestPayload(serializePayload(command.requestPayload()));
        task.setRetryCount(0);
        task.setMaxRetryCount(properties.getMaxRetryCount());
        task.setCreatedTs(now);
        task.setUpdatedTs(now);
        try {
            taskRepository.insert(task);
            log.info("traceId={} bizOrderNo={} taskId={} taskType={} bizKey={} partitionNo={} targetCode={} "
                            + "requestPath={} async compensation task enqueued",
                    TraceIdUtil.getTraceId(),
                    task.getBizOrderNo(),
                    task.getTaskId(),
                    task.getTaskType(),
                    task.getBizKey(),
                    task.getPartitionNo(),
                    task.getTargetCode(),
                    task.getRequestPath());
        } catch (DuplicateKeyException exception) {
            log.warn("traceId={} bizOrderNo={} taskId={} taskType={} bizKey={} partitionNo={} targetCode={} "
                            + "requestPath={} errorNo={} errorMsg={} async compensation task duplicated, ignored",
                    TraceIdUtil.getTraceId(),
                    task.getBizOrderNo(),
                    task.getTaskId(),
                    task.getTaskType(),
                    task.getBizKey(),
                    task.getPartitionNo(),
                    task.getTargetCode(),
                    task.getRequestPath(),
                    ErrorCodes.ASYNC_COMPENSATION_DUPLICATED,
                    "Async compensation task duplicated");
        }
    }

    private String serializePayload(AsyncCompensationEnqueuePayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new BizException(
                    "ASYNC_COMPENSATION_PAYLOAD_SERIALIZE_FAILED",
                    "Failed to serialize async compensation payload"
            );
        }
    }
}
