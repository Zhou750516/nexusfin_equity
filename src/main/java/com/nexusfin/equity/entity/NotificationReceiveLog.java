package com.nexusfin.equity.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("notification_receive_log")
public class NotificationReceiveLog {

    @TableId
    private String notifyNo;
    private String benefitOrderNo;
    private String notifyType;
    private String requestId;
    private String processStatus;
    private String payload;
    private Integer retryCount;
    private LocalDateTime receivedTs;
    private LocalDateTime processedTs;

    public String getNotifyNo() {
        return notifyNo;
    }

    public void setNotifyNo(String notifyNo) {
        this.notifyNo = notifyNo;
    }

    public String getBenefitOrderNo() {
        return benefitOrderNo;
    }

    public void setBenefitOrderNo(String benefitOrderNo) {
        this.benefitOrderNo = benefitOrderNo;
    }

    public String getNotifyType() {
        return notifyType;
    }

    public void setNotifyType(String notifyType) {
        this.notifyType = notifyType;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getProcessStatus() {
        return processStatus;
    }

    public void setProcessStatus(String processStatus) {
        this.processStatus = processStatus;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public LocalDateTime getReceivedTs() {
        return receivedTs;
    }

    public void setReceivedTs(LocalDateTime receivedTs) {
        this.receivedTs = receivedTs;
    }

    public LocalDateTime getProcessedTs() {
        return processedTs;
    }

    public void setProcessedTs(LocalDateTime processedTs) {
        this.processedTs = processedTs;
    }
}
