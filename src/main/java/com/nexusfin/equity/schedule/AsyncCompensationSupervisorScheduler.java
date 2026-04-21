package com.nexusfin.equity.schedule;

import com.nexusfin.equity.service.AsyncCompensationSchedulerCoordinator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AsyncCompensationSupervisorScheduler {

    private static final Logger log = LoggerFactory.getLogger(AsyncCompensationSupervisorScheduler.class);

    private final AsyncCompensationSchedulerCoordinator coordinator;

    public AsyncCompensationSupervisorScheduler(AsyncCompensationSchedulerCoordinator coordinator) {
        this.coordinator = coordinator;
    }

    @Scheduled(fixedDelayString = "${nexusfin.async-compensation.supervisor-interval-ms:15000}")
    public void poll() {
        if (!coordinator.isSchedulingEnabled()) {
            return;
        }
        if (!coordinator.isSupervisorEnabled()) {
            return;
        }
        try {
            coordinator.runSupervisorTick();
        } catch (RuntimeException exception) {
            log.error("async compensation supervisor scheduler tick failed", exception);
        }
    }
}
