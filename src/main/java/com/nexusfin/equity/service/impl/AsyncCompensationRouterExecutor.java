package com.nexusfin.equity.service.impl;

import com.nexusfin.equity.entity.AsyncCompensationTask;
import com.nexusfin.equity.exception.BizException;
import com.nexusfin.equity.service.AsyncCompensationExecutor;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class AsyncCompensationRouterExecutor {

    private final Map<String, AsyncCompensationExecutor> executorMap;

    public AsyncCompensationRouterExecutor(List<AsyncCompensationExecutor> executors) {
        this.executorMap = executors.stream()
                .collect(Collectors.toMap(AsyncCompensationExecutor::taskType, Function.identity()));
    }

    public AsyncCompensationExecutor.ExecutionResult execute(AsyncCompensationTask task) {
        AsyncCompensationExecutor executor = executorMap.get(task.getTaskType());
        if (executor == null) {
            throw new BizException("ASYNC_COMPENSATION_EXECUTOR_MISSING",
                    "No async compensation executor found for taskType=" + task.getTaskType());
        }
        return executor.execute(task);
    }
}
