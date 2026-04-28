package com.nexusfin.equity.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("loan_application_mapping")
public class LoanApplicationMapping {

    @TableId
    private String applicationId;
    private String memberId;
    private String benefitOrderNo;
    private String channelCode;
    private String externalUserId;
    private String upstreamQueryType;
    private String upstreamQueryValue;
    private String purpose;
    private String mappingStatus;
    private LocalDateTime createdTs;
    private LocalDateTime updatedTs;

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public String getMemberId() {
        return memberId;
    }

    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }

    public String getBenefitOrderNo() {
        return benefitOrderNo;
    }

    public void setBenefitOrderNo(String benefitOrderNo) {
        this.benefitOrderNo = benefitOrderNo;
    }

    public String getChannelCode() {
        return channelCode;
    }

    public void setChannelCode(String channelCode) {
        this.channelCode = channelCode;
    }

    public String getExternalUserId() {
        return externalUserId;
    }

    public void setExternalUserId(String externalUserId) {
        this.externalUserId = externalUserId;
    }

    public String getUpstreamQueryType() {
        return upstreamQueryType;
    }

    public void setUpstreamQueryType(String upstreamQueryType) {
        this.upstreamQueryType = upstreamQueryType;
    }

    public String getUpstreamQueryValue() {
        return upstreamQueryValue;
    }

    public void setUpstreamQueryValue(String upstreamQueryValue) {
        this.upstreamQueryValue = upstreamQueryValue;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public String getMappingStatus() {
        return mappingStatus;
    }

    public void setMappingStatus(String mappingStatus) {
        this.mappingStatus = mappingStatus;
    }

    public LocalDateTime getCreatedTs() {
        return createdTs;
    }

    public void setCreatedTs(LocalDateTime createdTs) {
        this.createdTs = createdTs;
    }

    public LocalDateTime getUpdatedTs() {
        return updatedTs;
    }

    public void setUpdatedTs(LocalDateTime updatedTs) {
        this.updatedTs = updatedTs;
    }
}
