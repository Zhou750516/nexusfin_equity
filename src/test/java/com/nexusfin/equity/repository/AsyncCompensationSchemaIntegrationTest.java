package com.nexusfin.equity.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AsyncCompensationSchemaIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldCreateAsyncCompensationTables() {
        Integer taskTable = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where lower(table_name) = ?",
                Integer.class,
                "async_compensation_task"
        );
        Integer attemptTable = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where lower(table_name) = ?",
                Integer.class,
                "async_compensation_attempt"
        );
        Integer runtimeTable = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where lower(table_name) = ?",
                Integer.class,
                "async_compensation_partition_runtime"
        );

        assertThat(taskTable).isEqualTo(1);
        assertThat(attemptTable).isEqualTo(1);
        assertThat(runtimeTable).isEqualTo(1);
    }
}
