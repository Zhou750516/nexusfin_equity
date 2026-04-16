package com.nexusfin.equity.config;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class QwPayProtocolOverrideGuard {

    private static final Logger log = LoggerFactory.getLogger(QwPayProtocolOverrideGuard.class);

    private final QwProperties qwProperties;
    private final Environment environment;

    public QwPayProtocolOverrideGuard(QwProperties qwProperties, Environment environment) {
        this.qwProperties = qwProperties;
        this.environment = environment;
    }

    @PostConstruct
    void validateOnStartup() {
        String override = qwProperties.getMemberSyncPayProtocolNoOverride();
        if (override == null || override.isBlank()) {
            return;
        }
        if (!qwProperties.isAllowMemberSyncPayProtocolNoOverride()) {
            throw new IllegalStateException(
                    "QW_MEMBER_SYNC_PAY_PROTOCOL_NO_OVERRIDE is configured but QW_ALLOW_MEMBER_SYNC_PAY_PROTOCOL_NO_OVERRIDE is false"
            );
        }
        List<String> allowedProfiles = normalizeProfiles(qwProperties.getMemberSyncPayProtocolNoOverrideAllowedProfiles());
        List<String> activeProfiles = normalizeProfiles(Arrays.asList(environment.getActiveProfiles()));
        boolean matched = activeProfiles.stream().anyMatch(allowedProfiles::contains);
        if (!matched) {
            throw new IllegalStateException(
                    "QW_MEMBER_SYNC_PAY_PROTOCOL_NO_OVERRIDE is only allowed in configured integration profiles; "
                            + "activeProfiles=" + activeProfiles + ", allowedProfiles=" + allowedProfiles
            );
        }
        log.warn("traceId=SYSTEM bizOrderNo=SYSTEM qw payProtocolNo override enabled; profile={} allowedProfiles={}",
                activeProfiles.stream().filter(allowedProfiles::contains).findFirst().orElse("unknown"),
                allowedProfiles);
    }

    private List<String> normalizeProfiles(List<String> profiles) {
        if (profiles == null) {
            return List.of();
        }
        return profiles.stream()
                .filter(profile -> profile != null && !profile.isBlank())
                .map(profile -> profile.trim().toLowerCase(Locale.ROOT))
                .collect(Collectors.toList());
    }
}
