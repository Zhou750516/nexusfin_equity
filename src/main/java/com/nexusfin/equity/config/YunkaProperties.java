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
            String repayQuery,
            String protocolQuery,
            String userToken,
            String userQuery,
            String loanRepayPlan,
            String cardSmsSend,
            String cardSmsConfirm,
            String cardUserCards,
            String creditImageQuery,
            String benefitSync
    ) {
    }
}
