package com.nexusfin.equity.config;

import java.util.List;

public class QwPaymentProperties {

    private String defaultPayProtocolPrefix = "proto-";
    private String memberSyncPayProtocolNoOverride;
    private boolean allowMemberSyncPayProtocolNoOverride;
    private List<String> memberSyncPayProtocolNoOverrideAllowedProfiles = List.of("test", "mysql-it", "local");

    public String getDefaultPayProtocolPrefix() {
        return defaultPayProtocolPrefix;
    }

    public void setDefaultPayProtocolPrefix(String defaultPayProtocolPrefix) {
        this.defaultPayProtocolPrefix = defaultPayProtocolPrefix;
    }

    public String getMemberSyncPayProtocolNoOverride() {
        return memberSyncPayProtocolNoOverride;
    }

    public void setMemberSyncPayProtocolNoOverride(String memberSyncPayProtocolNoOverride) {
        this.memberSyncPayProtocolNoOverride = memberSyncPayProtocolNoOverride;
    }

    public boolean isAllowMemberSyncPayProtocolNoOverride() {
        return allowMemberSyncPayProtocolNoOverride;
    }

    public void setAllowMemberSyncPayProtocolNoOverride(boolean allowMemberSyncPayProtocolNoOverride) {
        this.allowMemberSyncPayProtocolNoOverride = allowMemberSyncPayProtocolNoOverride;
    }

    public List<String> getMemberSyncPayProtocolNoOverrideAllowedProfiles() {
        return memberSyncPayProtocolNoOverrideAllowedProfiles;
    }

    public void setMemberSyncPayProtocolNoOverrideAllowedProfiles(List<String> memberSyncPayProtocolNoOverrideAllowedProfiles) {
        this.memberSyncPayProtocolNoOverrideAllowedProfiles = memberSyncPayProtocolNoOverrideAllowedProfiles;
    }
}
