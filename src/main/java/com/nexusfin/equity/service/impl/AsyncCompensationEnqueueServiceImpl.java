package com.nexusfin.equity.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
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
        String requestPayload = serializePayload(command.requestPayload());
        task.setRequestPayload(requestPayload);
        String logRequestPayload = sanitizePayloadForLog(requestPayload);
        task.setRetryCount(0);
        task.setMaxRetryCount(properties.getMaxRetryCount());
        task.setCreatedTs(now);
        task.setUpdatedTs(now);
        try {
            taskRepository.insert(task);
            log.info("traceId={} bizOrderNo={} taskId={} taskType={} bizKey={} partitionNo={} targetCode={} "
                            + "requestPath={} requestPayload={} async compensation task enqueued",
                    TraceIdUtil.getTraceId(),
                    task.getBizOrderNo(),
                    task.getTaskId(),
                    task.getTaskType(),
                    task.getBizKey(),
                    task.getPartitionNo(),
                    task.getTargetCode(),
                    task.getRequestPath(),
                    logRequestPayload);
        } catch (DuplicateKeyException exception) {
            log.warn("traceId={} bizOrderNo={} taskId={} taskType={} bizKey={} partitionNo={} targetCode={} "
                            + "requestPath={} requestPayload={} errorNo={} errorMsg={} async compensation task duplicated, ignored",
                    TraceIdUtil.getTraceId(),
                    task.getBizOrderNo(),
                    task.getTaskId(),
                    task.getTaskType(),
                    task.getBizKey(),
                    task.getPartitionNo(),
                    task.getTargetCode(),
                    task.getRequestPath(),
                    logRequestPayload,
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

    private String sanitizePayloadForLog(String requestPayload) {
        if (requestPayload == null || requestPayload.isBlank()) {
            return requestPayload;
        }
        try {
            JsonNode root = objectMapper.readTree(requestPayload);
            redactImageInfo(root.path("imageInfo"));
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException exception) {
            return requestPayload;
        }
    }

    private void redactImageInfo(JsonNode imageInfo) {
        if (imageInfo == null || imageInfo.isMissingNode() || imageInfo.isNull()) {
            return;
        }
        if (imageInfo.isArray()) {
            for (JsonNode item : imageInfo) {
                redactImageFields(item);
            }
            return;
        }
        redactImageFields(imageInfo);
    }

    private void redactImageFields(JsonNode node) {
        if (!(node instanceof ObjectNode objectNode)) {
            return;
        }
        redactImageField(objectNode, "back");
        redactImageField(objectNode, "front");
        redactImageField(objectNode, "nature");
    }

    private void redactImageField(ObjectNode objectNode, String fieldName) {
        JsonNode value = objectNode.path(fieldName);
        if (value.isMissingNode() || value.isNull()) {
            return;
        }
        String text = value.asText("");
        ObjectNode summary = JsonNodeFactory.instance.objectNode();
        summary.put("redacted", "IMAGE_BASE64");
        summary.put("length", text.length());
        summary.put("sha256Prefix", sha256Prefix(text));
        objectNode.set(fieldName, summary);
    }

    private String sha256Prefix(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < Math.min(6, hash.length); i++) {
                builder.append(String.format("%02x", hash[i]));
            }
            return builder.toString();
        } catch (Exception exception) {
            return "HASH_FAILED";
        }
    }
}
