package com.nexusfin.equity.service;

import java.util.List;

public interface AsyncCompensationSchedulerCoordinator {

    boolean isSchedulingEnabled();

    boolean isWorkerEnabled();

    boolean isSupervisorEnabled();

    String getWorkerId();

    List<Integer> getOwnedPartitions();

    void runWorkerTick(int partitionNo);

    void runSupervisorTick();
}
