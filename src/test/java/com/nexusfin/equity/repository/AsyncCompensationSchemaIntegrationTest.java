package com.nexusfin.equity.repository;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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

    @Test
    void shouldDefineMySqlAsyncPayloadColumnsAsMediumText() throws Exception {
        String schemaSql = readClasspathResource("db/schema.sql").toLowerCase();
        String migrationSql = readClasspathResource("db/migration/V20260418__create_async_compensation_tables.sql")
                .toLowerCase();

        assertThat(schemaSql)
                .contains("request_payload mediumtext not null")
                .contains("response_payload mediumtext")
                .contains("request_payload mediumtext")
                .contains("response_payload mediumtext");
        assertThat(migrationSql)
                .contains("request_payload mediumtext not null")
                .contains("response_payload mediumtext")
                .contains("request_payload mediumtext")
                .contains("response_payload mediumtext");
    }

    private String readClasspathResource(String path) throws Exception {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(path)) {
            assertThat(inputStream).as("classpath resource %s", path).isNotNull();
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
