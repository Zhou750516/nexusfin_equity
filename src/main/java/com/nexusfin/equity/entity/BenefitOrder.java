package com.nexusfin.equity.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("benefit_order")
public class BenefitOrder {

    @TableId
    private String benefitOrderNo;
    private String memberId;
    private String sourceChannelCode;
    private String externalUserId;
    private String productCode;
    private String agreementNo;
    private Long loanAmount;
    private String orderStatus;
    private String firstDeductStatus;
    private String fallbackDeductStatus;
    private String exerciseStatus;
    private String refundStatus;
    private String grantStatus;
    private String loanOrderNo;
    private String syncStatus;
    private String requestId;
    private String payProtocolNoSnapshot;
    private String payProtocolSource;
    private LocalDateTime createdTs;
    private LocalDateTime updatedTs;

    public String getBenefitOrderNo() {
        return benefitOrderNo;
    }

    public void setBenefitOrderNo(String benefitOrderNo) {
        this.benefitOrderNo = benefitOrderNo;
    }

    public String getMemberId() {
        return memberId;
    }

    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }

    public String getSourceChannelCode() {
        return sourceChannelCode;
    }

    public void setSourceChannelCode(String sourceChannelCode) {
        this.sourceChannelCode = sourceChannelCode;
    }

    public String getExternalUserId() {
        return externalUserId;
    }

    public void setExternalUserId(String externalUserId) {
        this.externalUserId = externalUserId;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public String getAgreementNo() {
        return agreementNo;
    }

    public void setAgreementNo(String agreementNo) {
        this.agreementNo = agreementNo;
    }

    public Long getLoanAmount() {
        return loanAmount;
    }

    public void setLoanAmount(Long loanAmount) {
        this.loanAmount = loanAmount;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }

    public String getFirstDeductStatus() {
        return firstDeductStatus;
    }

    public void setFirstDeductStatus(String firstDeductStatus) {
        this.firstDeductStatus = firstDeductStatus;
    }

    public String getFallbackDeductStatus() {
        return fallbackDeductStatus;
    }

    public void setFallbackDeductStatus(String fallbackDeductStatus) {
        this.fallbackDeductStatus = fallbackDeductStatus;
    }

    public String getExerciseStatus() {
        return exerciseStatus;
    }

    public void setExerciseStatus(String exerciseStatus) {
        this.exerciseStatus = exerciseStatus;
    }

    public String getRefundStatus() {
        return refundStatus;
    }

    public void setRefundStatus(String refundStatus) {
        this.refundStatus = refundStatus;
    }

    public String getGrantStatus() {
        return grantStatus;
    }

    public void setGrantStatus(String grantStatus) {
        this.grantStatus = grantStatus;
    }

    public String getLoanOrderNo() {
        return loanOrderNo;
    }

    public void setLoanOrderNo(String loanOrderNo) {
        this.loanOrderNo = loanOrderNo;
    }

    public String getSyncStatus() {
        return syncStatus;
    }

    public void setSyncStatus(String syncStatus) {
        this.syncStatus = syncStatus;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getPayProtocolNoSnapshot() {
        return payProtocolNoSnapshot;
    }

    public void setPayProtocolNoSnapshot(String payProtocolNoSnapshot) {
        this.payProtocolNoSnapshot = payProtocolNoSnapshot;
    }

    public String getPayProtocolSource() {
        return payProtocolSource;
    }

    public void setPayProtocolSource(String payProtocolSource) {
        this.payProtocolSource = payProtocolSource;
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
