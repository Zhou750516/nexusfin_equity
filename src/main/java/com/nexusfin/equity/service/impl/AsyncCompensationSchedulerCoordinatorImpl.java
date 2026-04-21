package com.nexusfin.equity.service.impl;

import com.nexusfin.equity.config.AsyncCompensationProperties;
import com.nexusfin.equity.service.AsyncCompensationSchedulerCoordinator;
import com.nexusfin.equity.service.AsyncCompensationSupervisorService;
import com.nexusfin.equity.service.AsyncCompensationWorkerService;
import jakarta.annotation.PostConstruct;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AsyncCompensationSchedulerCoordinatorImpl implements AsyncCompensationSchedulerCoordinator {

    private static final Logger log = LoggerFactory.getLogger(AsyncCompensationSchedulerCoordinatorImpl.class);

    private final AsyncCompensationProperties properties;
    private final AsyncCompensationWorkerService workerService;
    private final AsyncCompensationSupervisorService supervisorService;
    private final String applicationName;

    public AsyncCompensationSchedulerCoordinatorImpl(
            AsyncCompensationProperties properties,
            AsyncCompensationWorkerService workerService,
            AsyncCompensationSupervisorService supervisorService,
            @Value("${spring.application.name:nexusfin-equity}") String applicationName
    ) {
        this.properties = properties;
        this.workerService = workerService;
        this.supervisorService = supervisorService;
        this.applicationName = applicationName;
    }

    @PostConstruct
    public void logStartupSummary() {
        log.info("async compensation scheduling configured enabled={} workerEnabled={} supervisorEnabled={} workerId={} ownedPartitions={}",
                isSchedulingEnabled(), isWorkerEnabled(), isSupervisorEnabled(), getWorkerId(), getOwnedPartitions());
    }

    @Override
    public boolean isSchedulingEnabled() {
        return properties.isEnabled();
    }

    @Override
    public boolean isWorkerEnabled() {
        return properties.isWorkerEnabled();
    }

    @Override
    public boolean isSupervisorEnabled() {
        return properties.isSupervisorEnabled();
    }

    @Override
    public String getWorkerId() {
        if (StringUtils.hasText(properties.getWorkerId())) {
            return properties.getWorkerId().trim();
        }
        return applicationName + "-" + resolveRuntimeIdentity();
    }

    @Override
    public List<Integer> getOwnedPartitions() {
        return properties.getOwnedPartitions().stream()
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
    }

    @Override
    public void runWorkerTick(int partitionNo) {
        String workerId = getWorkerId();
        try {
            AsyncCompensationWorkerService.WorkerProcessResult result = workerService.processNext(
                    workerId,
                    partitionNo,
                    taskId -> supervisorService.heartbeat(partitionNo, workerId, "RUNNING", taskId)
            );
            if (result.handled()) {
                log.info("async compensation worker tick handled partitionNo={} workerId={}",
                        partitionNo, workerId);
            }
        } finally {
            supervisorService.heartbeat(partitionNo, workerId, "IDLE", null);
        }
    }

    @Override
    public void runSupervisorTick() {
        int recycledCount = supervisorService.recycleExpiredLeases();
        List<Integer> offlinePartitions = supervisorService.findOfflinePartitions();
        long deadTaskCount = supervisorService.countDeadTasks();
        var backlogByPartition = supervisorService.collectReadyTaskBacklogByPartition();
        if (recycledCount > 0 || !offlinePartitions.isEmpty() || deadTaskCount > 0 || !backlogByPartition.isEmpty()) {
            log.info("async compensation supervisor tick recycledCount={} deadTaskCount={} offlinePartitions={} backlogByPartition={}",
                    recycledCount, deadTaskCount, offlinePartitions, backlogByPartition);
            return;
        }
        log.debug("async compensation supervisor tick idle");
    }

    private String resolveRuntimeIdentity() {
        String runtimeName = ManagementFactory.getRuntimeMXBean().getName();
        if (StringUtils.hasText(runtimeName)) {
            return runtimeName.replace('@', '-');
        }
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException exception) {
            log.warn("resolve async compensation worker host name failed, fallback to unknown-host", exception);
            return "unknown-host";
        }
    }
}
