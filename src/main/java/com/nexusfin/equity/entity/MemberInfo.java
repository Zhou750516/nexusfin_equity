package com.nexusfin.equity.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("member_info")
public class MemberInfo {

    @TableId
    private String memberId;
    private String techPlatformUserId;
    private String externalUserId;
    private String mobileEncrypted;
    private String mobileHash;
    private String idCardEncrypted;
    private String idCardHash;
    private String realNameEncrypted;
    private String memberStatus;
    private LocalDateTime createdTs;
    private LocalDateTime updatedTs;

    public String getMemberId() {
        return memberId;
    }

    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }

    public String getExternalUserId() {
        return externalUserId;
    }

    public String getTechPlatformUserId() {
        return techPlatformUserId;
    }

    public void setTechPlatformUserId(String techPlatformUserId) {
        this.techPlatformUserId = techPlatformUserId;
    }

    public void setExternalUserId(String externalUserId) {
        this.externalUserId = externalUserId;
    }

    public String getMobileEncrypted() {
        return mobileEncrypted;
    }

    public void setMobileEncrypted(String mobileEncrypted) {
        this.mobileEncrypted = mobileEncrypted;
    }

    public String getMobileHash() {
        return mobileHash;
    }

    public void setMobileHash(String mobileHash) {
        this.mobileHash = mobileHash;
    }

    public String getIdCardEncrypted() {
        return idCardEncrypted;
    }

    public void setIdCardEncrypted(String idCardEncrypted) {
        this.idCardEncrypted = idCardEncrypted;
    }

    public String getIdCardHash() {
        return idCardHash;
    }

    public void setIdCardHash(String idCardHash) {
        this.idCardHash = idCardHash;
    }

    public String getRealNameEncrypted() {
        return realNameEncrypted;
    }

    public void setRealNameEncrypted(String realNameEncrypted) {
        this.realNameEncrypted = realNameEncrypted;
    }

    public String getMemberStatus() {
        return memberStatus;
    }

    public void setMemberStatus(String memberStatus) {
        this.memberStatus = memberStatus;
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
