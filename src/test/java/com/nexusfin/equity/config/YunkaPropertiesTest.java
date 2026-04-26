package com.nexusfin.equity.config;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

class YunkaPropertiesTest {

    @Test
    void shouldBindExtendedYunkaPathsFromConfiguration() {
        MapConfigurationPropertySource source = new MapConfigurationPropertySource(Map.ofEntries(
                Map.entry("nexusfin.third-party.yunka.enabled", "true"),
                Map.entry("nexusfin.third-party.yunka.mode", "REST"),
                Map.entry("nexusfin.third-party.yunka.base-url", "https://yunka.test"),
                Map.entry("nexusfin.third-party.yunka.gateway-path", "/api/gateway/proxy"),
                Map.entry("nexusfin.third-party.yunka.connect-timeout-ms", "2000"),
                Map.entry("nexusfin.third-party.yunka.read-timeout-ms", "3000"),
                Map.entry("nexusfin.third-party.yunka.paths.loan-calculate", "/loan/trail"),
                Map.entry("nexusfin.third-party.yunka.paths.loan-query", "/loan/query"),
                Map.entry("nexusfin.third-party.yunka.paths.loan-apply", "/loan/apply"),
                Map.entry("nexusfin.third-party.yunka.paths.repay-trial", "/repay/trial"),
                Map.entry("nexusfin.third-party.yunka.paths.repay-apply", "/repay/apply"),
                Map.entry("nexusfin.third-party.yunka.paths.repay-query", "/repay/query"),
                Map.entry("nexusfin.third-party.yunka.paths.protocol-query", "/protocol/queryProtocolAggregationLink"),
                Map.entry("nexusfin.third-party.yunka.paths.user-token", "/user/token"),
                Map.entry("nexusfin.third-party.yunka.paths.user-query", "/user/query"),
                Map.entry("nexusfin.third-party.yunka.paths.loan-repay-plan", "/loan/repayPlan"),
                Map.entry("nexusfin.third-party.yunka.paths.card-sms-send", "/card/smsSend"),
                Map.entry("nexusfin.third-party.yunka.paths.card-sms-confirm", "/card/smsConfirm"),
                Map.entry("nexusfin.third-party.yunka.paths.card-user-cards", "/card/userCards"),
                Map.entry("nexusfin.third-party.yunka.paths.credit-image-query", "/credit/image/query"),
                Map.entry("nexusfin.third-party.yunka.paths.benefit-sync", "/benefit/sync")
        ));

        YunkaProperties properties = new Binder(source)
                .bind("nexusfin.third-party.yunka", YunkaProperties.class)
                .orElseThrow(IllegalStateException::new);

        assertThat(properties.paths().loanCalculate()).isEqualTo("/loan/trail");
        assertThat(properties.paths().loanQuery()).isEqualTo("/loan/query");
        assertThat(properties.paths().loanApply()).isEqualTo("/loan/apply");
        assertThat(properties.paths().repayTrial()).isEqualTo("/repay/trial");
        assertThat(properties.paths().repayApply()).isEqualTo("/repay/apply");
        assertThat(properties.paths().repayQuery()).isEqualTo("/repay/query");
        assertThat(properties.paths().protocolQuery()).isEqualTo("/protocol/queryProtocolAggregationLink");
        assertThat(properties.paths().userToken()).isEqualTo("/user/token");
        assertThat(properties.paths().userQuery()).isEqualTo("/user/query");
        assertThat(properties.paths().loanRepayPlan()).isEqualTo("/loan/repayPlan");
        assertThat(properties.paths().cardSmsSend()).isEqualTo("/card/smsSend");
        assertThat(properties.paths().cardSmsConfirm()).isEqualTo("/card/smsConfirm");
        assertThat(properties.paths().cardUserCards()).isEqualTo("/card/userCards");
        assertThat(properties.paths().creditImageQuery()).isEqualTo("/credit/image/query");
        assertThat(properties.paths().benefitSync()).isEqualTo("/benefit/sync");
    }
}
