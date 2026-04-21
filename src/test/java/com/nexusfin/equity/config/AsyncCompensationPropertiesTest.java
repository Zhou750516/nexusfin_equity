package com.nexusfin.equity.config;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

class AsyncCompensationPropertiesTest {

    @Test
    void shouldExposeSchedulingDefaults() {
        AsyncCompensationProperties properties = new AsyncCompensationProperties();

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.isWorkerEnabled()).isFalse();
        assertThat(properties.isSupervisorEnabled()).isFalse();
        assertThat(properties.getWorkerPollIntervalMs()).isEqualTo(3000L);
        assertThat(properties.getSupervisorIntervalMs()).isEqualTo(15000L);
        assertThat(properties.getWorkerId()).isBlank();
        assertThat(properties.getOwnedPartitions()).isEmpty();
    }

    @Test
    void shouldBindSchedulingPropertiesFromConfiguration() {
        MapConfigurationPropertySource source = new MapConfigurationPropertySource(Map.of(
                "nexusfin.async-compensation.enabled", "true",
                "nexusfin.async-compensation.worker-enabled", "true",
                "nexusfin.async-compensation.supervisor-enabled", "false",
                "nexusfin.async-compensation.worker-poll-interval-ms", "5000",
                "nexusfin.async-compensation.supervisor-interval-ms", "25000",
                "nexusfin.async-compensation.worker-id", "worker-a",
                "nexusfin.async-compensation.owned-partitions[0]", "0",
                "nexusfin.async-compensation.owned-partitions[1]", "3",
                "nexusfin.async-compensation.owned-partitions[2]", "7"
        ));

        AsyncCompensationProperties properties = new Binder(source)
                .bind("nexusfin.async-compensation", AsyncCompensationProperties.class)
                .orElseThrow(IllegalStateException::new);

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.isWorkerEnabled()).isTrue();
        assertThat(properties.isSupervisorEnabled()).isFalse();
        assertThat(properties.getWorkerPollIntervalMs()).isEqualTo(5000L);
        assertThat(properties.getSupervisorIntervalMs()).isEqualTo(25000L);
        assertThat(properties.getWorkerId()).isEqualTo("worker-a");
        assertThat(properties.getOwnedPartitions()).containsExactly(0, 3, 7);
    }

    @Test
    void shouldKeepBackoffCalculationAfterSchedulingFieldsAdded() {
        AsyncCompensationProperties properties = new AsyncCompensationProperties();
        properties.setRetryInitialDelaySeconds(10);
        properties.setRetryMaxDelaySeconds(30);

        assertThat(properties.nextRetryDelaySeconds(1)).isEqualTo(10L);
        assertThat(properties.nextRetryDelaySeconds(2)).isEqualTo(20L);
        assertThat(properties.nextRetryDelaySeconds(3)).isEqualTo(30L);
        assertThat(properties.nextRetryDelaySeconds(4)).isEqualTo(30L);
    }

    @Test
    void shouldAllowEmptyOwnedPartitionsWhenSchedulingEnabled() {
        MapConfigurationPropertySource source = new MapConfigurationPropertySource(Map.of(
                "nexusfin.async-compensation.enabled", "true",
                "nexusfin.async-compensation.worker-enabled", "true"
        ));

        AsyncCompensationProperties properties = new Binder(source)
                .bind("nexusfin.async-compensation", AsyncCompensationProperties.class)
                .orElseThrow(IllegalStateException::new);

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.isWorkerEnabled()).isTrue();
        assertThat(properties.getOwnedPartitions()).isEqualTo(List.of());
    }
}
