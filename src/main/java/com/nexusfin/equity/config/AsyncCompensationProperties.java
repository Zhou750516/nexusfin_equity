package com.nexusfin.equity.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nexusfin.async-compensation")
public class AsyncCompensationProperties {

    private boolean enabled = false;
    private boolean workerEnabled = false;
    private boolean supervisorEnabled = false;
    private long workerPollIntervalMs = 3000L;
    private long supervisorIntervalMs = 15000L;
    private String workerId = "";
    private List<Integer> ownedPartitions = new ArrayList<>();
    private int partitionCount = 8;
    private int maxRetryCount = 5;
    private int leaseSeconds = 60;
    private int retryInitialDelaySeconds = 60;
    private int retryMaxDelaySeconds = 900;
    private int workerHeartbeatTimeoutSeconds = 120;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isWorkerEnabled() {
        return workerEnabled;
    }

    public void setWorkerEnabled(boolean workerEnabled) {
        this.workerEnabled = workerEnabled;
    }

    public boolean isSupervisorEnabled() {
        return supervisorEnabled;
    }

    public void setSupervisorEnabled(boolean supervisorEnabled) {
        this.supervisorEnabled = supervisorEnabled;
    }

    public long getWorkerPollIntervalMs() {
        return workerPollIntervalMs;
    }

    public void setWorkerPollIntervalMs(long workerPollIntervalMs) {
        this.workerPollIntervalMs = workerPollIntervalMs;
    }

    public long getSupervisorIntervalMs() {
        return supervisorIntervalMs;
    }

    public void setSupervisorIntervalMs(long supervisorIntervalMs) {
        this.supervisorIntervalMs = supervisorIntervalMs;
    }

    public String getWorkerId() {
        return workerId;
    }

    public void setWorkerId(String workerId) {
        this.workerId = workerId == null ? "" : workerId;
    }

    public List<Integer> getOwnedPartitions() {
        return ownedPartitions;
    }

    public void setOwnedPartitions(List<Integer> ownedPartitions) {
        this.ownedPartitions = ownedPartitions == null ? new ArrayList<>() : new ArrayList<>(ownedPartitions);
    }

    public int getPartitionCount() {
        return partitionCount;
    }

    public void setPartitionCount(int partitionCount) {
        this.partitionCount = partitionCount;
    }

    public int getMaxRetryCount() {
        return maxRetryCount;
    }

    public void setMaxRetryCount(int maxRetryCount) {
        this.maxRetryCount = maxRetryCount;
    }

    public int getLeaseSeconds() {
        return leaseSeconds;
    }

    public void setLeaseSeconds(int leaseSeconds) {
        this.leaseSeconds = leaseSeconds;
    }

    public int getRetryInitialDelaySeconds() {
        return retryInitialDelaySeconds;
    }

    public void setRetryInitialDelaySeconds(int retryInitialDelaySeconds) {
        this.retryInitialDelaySeconds = retryInitialDelaySeconds;
    }

    public int getRetryMaxDelaySeconds() {
        return retryMaxDelaySeconds;
    }

    public void setRetryMaxDelaySeconds(int retryMaxDelaySeconds) {
        this.retryMaxDelaySeconds = retryMaxDelaySeconds;
    }

    public long nextRetryDelaySeconds(int retryCountAfterFailure) {
        int safeRetryCount = Math.max(1, retryCountAfterFailure);
        long multiplier = 1L << Math.min(safeRetryCount - 1, 20);
        long delaySeconds = Math.max(1L, retryInitialDelaySeconds) * multiplier;
        return Math.min(delaySeconds, Math.max(1L, retryMaxDelaySeconds));
    }

    public int getWorkerHeartbeatTimeoutSeconds() {
        return workerHeartbeatTimeoutSeconds;
    }

    public void setWorkerHeartbeatTimeoutSeconds(int workerHeartbeatTimeoutSeconds) {
        this.workerHeartbeatTimeoutSeconds = workerHeartbeatTimeoutSeconds;
    }
}
