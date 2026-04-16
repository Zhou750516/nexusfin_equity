package com.nexusfin.equity.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nexusfin.third-party.yunka")
public record YunkaProperties(
        boolean enabled,
        String mode,
        String baseUrl,
        String gatewayPath,
        int connectTimeoutMs,
        int readTimeoutMs,
        Paths paths
) {

    public record Paths(
            String loanCalculate,
            String loanQuery,
            String loanApply,
            String repayTrial,
            String repayApply,
            String repayQuery
    ) {
    }
}
