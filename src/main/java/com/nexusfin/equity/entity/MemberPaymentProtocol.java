package com.nexusfin.equity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("member_payment_protocol")
public class MemberPaymentProtocol {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String memberId;
    private String externalUserId;
    private String providerCode;
    private String protocolNo;
    private String protocolStatus;
    private String signRequestNo;
    private String channelCode;
    private LocalDateTime signedTs;
    private LocalDateTime expiredTs;
    private LocalDateTime lastVerifiedTs;
    private LocalDateTime createdTs;
    private LocalDateTime updatedTs;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMemberId() {
        return memberId;
    }

    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }

    public String getExternalUserId() {
        return externalUserId;
    }

    public void setExternalUserId(String externalUserId) {
        this.externalUserId = externalUserId;
    }

    public String getProviderCode() {
        return providerCode;
    }

    public void setProviderCode(String providerCode) {
        this.providerCode = providerCode;
    }

    public String getProtocolNo() {
        return protocolNo;
    }

    public void setProtocolNo(String protocolNo) {
        this.protocolNo = protocolNo;
    }

    public String getProtocolStatus() {
        return protocolStatus;
    }

    public void setProtocolStatus(String protocolStatus) {
        this.protocolStatus = protocolStatus;
    }

    public String getSignRequestNo() {
        return signRequestNo;
    }

    public void setSignRequestNo(String signRequestNo) {
        this.signRequestNo = signRequestNo;
    }

    public String getChannelCode() {
        return channelCode;
    }

    public void setChannelCode(String channelCode) {
        this.channelCode = channelCode;
    }

    public LocalDateTime getSignedTs() {
        return signedTs;
    }

    public void setSignedTs(LocalDateTime signedTs) {
        this.signedTs = signedTs;
    }

    public LocalDateTime getExpiredTs() {
        return expiredTs;
    }

    public void setExpiredTs(LocalDateTime expiredTs) {
        this.expiredTs = expiredTs;
    }

    public LocalDateTime getLastVerifiedTs() {
        return lastVerifiedTs;
    }

    public void setLastVerifiedTs(LocalDateTime lastVerifiedTs) {
        this.lastVerifiedTs = lastVerifiedTs;
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
