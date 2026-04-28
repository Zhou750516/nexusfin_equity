package com.nexusfin.equity.service;

public interface AsyncCompensationEnqueueService {

    void enqueue(EnqueueCommand command);

    record EnqueueCommand(
            String taskType,
            String bizKey,
            String bizOrderNo,
            String targetCode,
            String requestPath,
            String httpMethod,
            String requestHeaders,
            AsyncCompensationEnqueuePayload requestPayload
    ) {
    }
}
