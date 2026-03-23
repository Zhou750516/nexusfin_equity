package com.nexusfin.equity.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("benefit_order")
public class BenefitOrder {

    @TableId
    private String benefitOrderNo;
    private String memberId;
    private String channelCode;
    private String externalUserId;
    private String productCode;
    private String agreementNo;
    private Long loanAmount;
    private String orderStatus;
    private String qwFirstDeductStatus;
    private String qwFallbackDeductStatus;
    private String qwExerciseStatus;
    private String refundStatus;
    private String grantStatus;
    private String loanOrderNo;
    private String syncStatus;
    private String requestId;
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

    public String getQwFirstDeductStatus() {
        return qwFirstDeductStatus;
    }

    public void setQwFirstDeductStatus(String qwFirstDeductStatus) {
        this.qwFirstDeductStatus = qwFirstDeductStatus;
    }

    public String getQwFallbackDeductStatus() {
        return qwFallbackDeductStatus;
    }

    public void setQwFallbackDeductStatus(String qwFallbackDeductStatus) {
        this.qwFallbackDeductStatus = qwFallbackDeductStatus;
    }

    public String getQwExerciseStatus() {
        return qwExerciseStatus;
    }

    public void setQwExerciseStatus(String qwExerciseStatus) {
        this.qwExerciseStatus = qwExerciseStatus;
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
