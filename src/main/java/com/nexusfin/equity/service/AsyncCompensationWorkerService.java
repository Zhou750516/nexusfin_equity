package com.nexusfin.equity.service;

public interface AsyncCompensationWorkerService {

    WorkerProcessResult processNext(String workerId, int partitionNo, WorkerLifecycleListener lifecycleListener);

    default boolean processNext(String workerId, int partitionNo) {
        return processNext(workerId, partitionNo, taskId -> {
        }).handled();
    }

    @FunctionalInterface
    interface WorkerLifecycleListener {

        void onTaskClaimed(String taskId);
    }

    record WorkerProcessResult(boolean handled, String taskId) {

        public static WorkerProcessResult none() {
            return new WorkerProcessResult(false, null);
        }

        public static WorkerProcessResult handled(String taskId) {
            return new WorkerProcessResult(true, taskId);
        }
    }
}
