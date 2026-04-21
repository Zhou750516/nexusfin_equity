package com.nexusfin.equity.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("async_compensation_partition_runtime")
public class AsyncCompensationPartitionRuntime {

    @TableId
    private Integer partitionNo;
    private String workerId;
    private String workerStatus;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String currentTaskId;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime currentTaskStartedTs;
    private LocalDateTime lastHeartbeatTs;
    private LocalDateTime updatedTs;

    public Integer getPartitionNo() {
        return partitionNo;
    }

    public void setPartitionNo(Integer partitionNo) {
        this.partitionNo = partitionNo;
    }

    public String getWorkerId() {
        return workerId;
    }

    public void setWorkerId(String workerId) {
        this.workerId = workerId;
    }

    public String getWorkerStatus() {
        return workerStatus;
    }

    public void setWorkerStatus(String workerStatus) {
        this.workerStatus = workerStatus;
    }

    public String getCurrentTaskId() {
        return currentTaskId;
    }

    public void setCurrentTaskId(String currentTaskId) {
        this.currentTaskId = currentTaskId;
    }

    public LocalDateTime getCurrentTaskStartedTs() {
        return currentTaskStartedTs;
    }

    public void setCurrentTaskStartedTs(LocalDateTime currentTaskStartedTs) {
        this.currentTaskStartedTs = currentTaskStartedTs;
    }

    public LocalDateTime getLastHeartbeatTs() {
        return lastHeartbeatTs;
    }

    public void setLastHeartbeatTs(LocalDateTime lastHeartbeatTs) {
        this.lastHeartbeatTs = lastHeartbeatTs;
    }

    public LocalDateTime getUpdatedTs() {
        return updatedTs;
    }

    public void setUpdatedTs(LocalDateTime updatedTs) {
        this.updatedTs = updatedTs;
    }
}
