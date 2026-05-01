package com.nexusfin.equity.config;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

class YunkaPropertiesTest {

    @Test
    void shouldBindYunkaPathDefaultsFromApplicationYaml() throws IOException {
        YunkaProperties properties = bindApplicationDefaults();

        assertThat(properties.paths().loanCalculate()).isEqualTo("/loan/trial");
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

    @Test
    void shouldKeepApplicationDefaultsAlignedWithLocalStubContractPaths() throws IOException {
        YunkaProperties.Paths paths = bindApplicationDefaults().paths();

        assertThat(List.of(
                paths.loanCalculate(),
                paths.loanApply(),
                paths.loanQuery(),
                paths.protocolQuery(),
                paths.benefitSync(),
                paths.userToken(),
                paths.userQuery(),
                paths.repayTrial(),
                paths.repayApply(),
                paths.repayQuery()
        )).containsExactly(
                "/loan/trial",
                "/loan/apply",
                "/loan/query",
                "/protocol/queryProtocolAggregationLink",
                "/benefit/sync",
                "/user/token",
                "/user/query",
                "/repay/trial",
                "/repay/apply",
                "/repay/query"
        );
    }

    private YunkaProperties bindApplicationDefaults() throws IOException {
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        List<PropertySource<?>> sources = loader.load("application.yml", new ClassPathResource("application.yml"));
        Map<String, Object> flattened = new java.util.LinkedHashMap<>();
        for (PropertySource<?> source : sources) {
            if (source.getSource() instanceof Map<?, ?> map) {
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    flattened.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
                }
            }
        }
        MapConfigurationPropertySource source = new MapConfigurationPropertySource(flattened);
        return new Binder(new ConfigurationPropertySource[]{source})
                .bind("nexusfin.third-party.yunka", YunkaProperties.class)
                .orElseThrow(IllegalStateException::new);
    }
}
