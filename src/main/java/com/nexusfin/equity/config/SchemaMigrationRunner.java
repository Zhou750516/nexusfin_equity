package com.nexusfin.equity.config;

import jakarta.annotation.PostConstruct;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class SchemaMigrationRunner {

    private static final Logger log = LoggerFactory.getLogger(SchemaMigrationRunner.class);

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    public SchemaMigrationRunner(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void migrateIfNeeded() {
        jdbcTemplate.execute((Connection connection) -> {
            DatabaseMetaData metaData = connection.getMetaData();
            String databaseProduct = metaData.getDatabaseProductName().toLowerCase(Locale.ROOT);
            migrateBenefitOrder(metaData, databaseProduct);
            migratePaymentRecord(metaData, databaseProduct);
            return null;
        });
    }

    private void migrateBenefitOrder(DatabaseMetaData metaData, String databaseProduct) throws SQLException {
        if (!hasTable(metaData, "benefit_order")) {
            return;
        }
        renameColumnIfNeeded(metaData, databaseProduct, "benefit_order", "channel_code", "source_channel_code", "varchar(64) not null");
        renameColumnIfNeeded(metaData, databaseProduct, "benefit_order", "qw_first_deduct_status", "first_deduct_status", "varchar(32) not null");
        renameColumnIfNeeded(metaData, databaseProduct, "benefit_order", "qw_fallback_deduct_status", "fallback_deduct_status", "varchar(32) not null");
        renameColumnIfNeeded(metaData, databaseProduct, "benefit_order", "qw_exercise_status", "exercise_status", "varchar(32) not null");
    }

    private void migratePaymentRecord(DatabaseMetaData metaData, String databaseProduct) throws SQLException {
        if (!hasTable(metaData, "payment_record")) {
            return;
        }
        renameColumnIfNeeded(metaData, databaseProduct, "payment_record", "channel_name", "provider_code", "varchar(64) not null");
    }

    private void renameColumnIfNeeded(
            DatabaseMetaData metaData,
            String databaseProduct,
            String tableName,
            String oldColumnName,
            String newColumnName,
            String columnDefinition
    ) throws SQLException {
        boolean hasOld = hasColumn(metaData, tableName, oldColumnName);
        boolean hasNew = hasColumn(metaData, tableName, newColumnName);
        if (!hasOld || hasNew) {
            return;
        }
        String sql = buildRenameColumnSql(databaseProduct, tableName, oldColumnName, newColumnName, columnDefinition);
        jdbcTemplate.execute(sql);
        log.info("schema column migrated table={} from={} to={}", tableName, oldColumnName, newColumnName);
    }

    private String buildRenameColumnSql(
            String databaseProduct,
            String tableName,
            String oldColumnName,
            String newColumnName,
            String columnDefinition
    ) {
        if (databaseProduct.contains("mysql")) {
            return "ALTER TABLE " + tableName + " CHANGE COLUMN " + oldColumnName + " " + newColumnName + " " + columnDefinition;
        }
        if (databaseProduct.contains("h2")) {
            return "ALTER TABLE " + tableName + " ALTER COLUMN " + oldColumnName + " RENAME TO " + newColumnName;
        }
        return "ALTER TABLE " + tableName + " RENAME COLUMN " + oldColumnName + " TO " + newColumnName;
    }

    private boolean hasTable(DatabaseMetaData metaData, String tableName) throws SQLException {
        try (ResultSet tables = metaData.getTables(null, null, tableName, null)) {
            return tables.next();
        }
    }

    private boolean hasColumn(DatabaseMetaData metaData, String tableName, String columnName) throws SQLException {
        try (ResultSet columns = metaData.getColumns(null, null, tableName, columnName)) {
            return columns.next();
        }
    }
}
