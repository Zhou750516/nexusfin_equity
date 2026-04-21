package com.nexusfin.equity.service;

import com.nexusfin.equity.entity.AsyncCompensationTask;

public interface AsyncCompensationExecutor {

    String taskType();

    ExecutionResult execute(AsyncCompensationTask task);

    record ExecutionResult(String responsePayload) {
    }
}
