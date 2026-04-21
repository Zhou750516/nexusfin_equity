package com.nexusfin.equity.service;

import java.util.List;
import java.util.Map;

public interface AsyncCompensationSupervisorService {

    void heartbeat(int partitionNo, String workerId, String workerStatus, String currentTaskId);

    int recycleExpiredLeases();

    List<Integer> findOfflinePartitions();

    Map<Integer, Long> collectReadyTaskBacklogByPartition();

    long countDeadTasks();
}
