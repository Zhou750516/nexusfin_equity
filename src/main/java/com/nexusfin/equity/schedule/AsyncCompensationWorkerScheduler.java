package com.nexusfin.equity.schedule;

import com.nexusfin.equity.service.AsyncCompensationSchedulerCoordinator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AsyncCompensationWorkerScheduler {

    private static final Logger log = LoggerFactory.getLogger(AsyncCompensationWorkerScheduler.class);

    private final AsyncCompensationSchedulerCoordinator coordinator;

    public AsyncCompensationWorkerScheduler(AsyncCompensationSchedulerCoordinator coordinator) {
        this.coordinator = coordinator;
    }

    @Scheduled(fixedDelayString = "${nexusfin.async-compensation.worker-poll-interval-ms:3000}")
    public void poll() {
        if (!coordinator.isSchedulingEnabled()) {
            return;
        }
        if (!coordinator.isWorkerEnabled()) {
            return;
        }
        List<Integer> ownedPartitions = coordinator.getOwnedPartitions();
        if (ownedPartitions.isEmpty()) {
            return;
        }
        for (Integer partitionNo : ownedPartitions) {
            try {
                coordinator.runWorkerTick(partitionNo);
            } catch (RuntimeException exception) {
                log.error("async compensation worker scheduler tick failed partitionNo={}",
                        partitionNo, exception);
            }
        }
    }
}
