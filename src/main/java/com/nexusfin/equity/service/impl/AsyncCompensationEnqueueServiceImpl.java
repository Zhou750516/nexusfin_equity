package com.nexusfin.equity.service.impl;

import com.nexusfin.equity.config.AsyncCompensationProperties;
import com.nexusfin.equity.entity.AsyncCompensationTask;
import com.nexusfin.equity.enums.AsyncCompensationTaskStatusEnum;
import com.nexusfin.equity.repository.AsyncCompensationTaskRepository;
import com.nexusfin.equity.service.AsyncCompensationEnqueueService;
import com.nexusfin.equity.util.AsyncCompensationPartitioner;
import com.nexusfin.equity.util.RequestIdUtil;
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

    public AsyncCompensationEnqueueServiceImpl(
            AsyncCompensationTaskRepository taskRepository,
            AsyncCompensationPartitioner partitioner,
            AsyncCompensationProperties properties
    ) {
        this.taskRepository = taskRepository;
        this.partitioner = partitioner;
        this.properties = properties;
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
        task.setRequestPayload(command.requestPayload());
        task.setRetryCount(0);
        task.setMaxRetryCount(properties.getMaxRetryCount());
        task.setCreatedTs(now);
        task.setUpdatedTs(now);
        try {
            taskRepository.insert(task);
        } catch (DuplicateKeyException exception) {
            log.info("Ignore duplicated async compensation task taskType={} bizKey={}",
                    command.taskType(), command.bizKey());
        }
    }
}
