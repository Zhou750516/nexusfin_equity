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
public class QwFullPlaintextLogGuard {

    private static final Logger log = LoggerFactory.getLogger(QwFullPlaintextLogGuard.class);

    private final QwProperties qwProperties;
    private final Environment environment;

    public QwFullPlaintextLogGuard(QwProperties qwProperties, Environment environment) {
        this.qwProperties = qwProperties;
        this.environment = environment;
    }

    @PostConstruct
    void validateOnStartup() {
        if (!qwProperties.getHttp().isLogFullPlaintextPayload()) {
            return;
        }
        List<String> allowedProfiles = normalizeProfiles(
                qwProperties.getHttp().getLogFullPlaintextPayloadAllowedProfiles()
        );
        List<String> activeProfiles = normalizeProfiles(Arrays.asList(environment.getActiveProfiles()));
        boolean matched = activeProfiles.stream().anyMatch(allowedProfiles::contains);
        if (!matched) {
            throw new IllegalStateException(
                    "QW_FULL_PLAINTEXT_LOG_NOT_ALLOWED: QW full plaintext payload logging is only allowed "
                            + "in configured integration profiles; activeProfiles=" + activeProfiles
                            + ", allowedProfiles=" + allowedProfiles
            );
        }
        log.warn("traceId=SYSTEM bizOrderNo=SYSTEM errorNo=QW_FULL_PLAINTEXT_LOG_ENABLED "
                        + "errorMsg=QW full plaintext payload logging is enabled; profile={} allowedProfiles={} "
                        + "qw full plaintext payload logging enabled",
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
