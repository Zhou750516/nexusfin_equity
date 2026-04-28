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
            migratePaymentProtocol(metaData, databaseProduct);
            migrateLoanApplicationMapping(metaData, databaseProduct);
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
        addColumnIfNeeded(metaData, "benefit_order", "pay_protocol_no_snapshot", "varchar(128)");
        addColumnIfNeeded(metaData, "benefit_order", "pay_protocol_source", "varchar(32)");
    }

    private void migratePaymentRecord(DatabaseMetaData metaData, String databaseProduct) throws SQLException {
        if (!hasTable(metaData, "payment_record")) {
            return;
        }
        renameColumnIfNeeded(metaData, databaseProduct, "payment_record", "channel_name", "provider_code", "varchar(64) not null");
    }

    private void migratePaymentProtocol(DatabaseMetaData metaData, String databaseProduct) throws SQLException {
        if (!hasTable(metaData, "member_payment_protocol")) {
            jdbcTemplate.execute(buildCreateMemberPaymentProtocolSql(databaseProduct));
            log.info("schema table created table=member_payment_protocol");
        }
        createIndexIfNeeded(metaData, databaseProduct, "member_payment_protocol", "uk_provider_protocol_no",
                "CREATE UNIQUE INDEX uk_provider_protocol_no ON member_payment_protocol (provider_code, protocol_no)");
        createIndexIfNeeded(metaData, databaseProduct, "member_payment_protocol", "idx_member_provider_status",
                "CREATE INDEX idx_member_provider_status ON member_payment_protocol (member_id, provider_code, protocol_status)");
        createIndexIfNeeded(metaData, databaseProduct, "member_payment_protocol", "idx_external_user_provider_status",
                "CREATE INDEX idx_external_user_provider_status ON member_payment_protocol (external_user_id, provider_code, protocol_status)");
    }

    private void migrateLoanApplicationMapping(DatabaseMetaData metaData, String databaseProduct) throws SQLException {
        if (hasTable(metaData, "loan_application_mapping")) {
            addColumnIfNeeded(metaData, "loan_application_mapping", "purpose", "varchar(32)");
            return;
        }
        jdbcTemplate.execute(buildCreateLoanApplicationMappingSql(databaseProduct));
        log.info("schema table created table=loan_application_mapping");
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

    private void addColumnIfNeeded(DatabaseMetaData metaData, String tableName, String columnName, String columnDefinition)
            throws SQLException {
        if (hasColumn(metaData, tableName, columnName)) {
            return;
        }
        jdbcTemplate.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnDefinition);
        log.info("schema column added table={} column={}", tableName, columnName);
    }

    private void createIndexIfNeeded(
            DatabaseMetaData metaData,
            String databaseProduct,
            String tableName,
            String indexName,
            String createSql
    ) throws SQLException {
        if (hasIndex(metaData, tableName, indexName)) {
            return;
        }
        jdbcTemplate.execute(createSql);
        log.info("schema index created table={} index={}", tableName, indexName);
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

    private String buildCreateMemberPaymentProtocolSql(String databaseProduct) {
        String idDefinition = databaseProduct.contains("h2")
                ? "id bigint generated by default as identity primary key"
                : "id bigint auto_increment primary key";
        return "CREATE TABLE member_payment_protocol ("
                + idDefinition + ", "
                + "member_id varchar(64) not null, "
                + "external_user_id varchar(64) not null, "
                + "provider_code varchar(64) not null, "
                + "protocol_no varchar(128) not null, "
                + "protocol_status varchar(32) not null, "
                + "sign_request_no varchar(64), "
                + "channel_code varchar(64), "
                + "signed_ts timestamp, "
                + "expired_ts timestamp, "
                + "last_verified_ts timestamp, "
                + "created_ts timestamp not null, "
                + "updated_ts timestamp not null"
                + ")";
    }

    private String buildCreateLoanApplicationMappingSql(String databaseProduct) {
        return "CREATE TABLE loan_application_mapping ("
                + "application_id varchar(64) primary key, "
                + "member_id varchar(64) not null, "
                + "benefit_order_no varchar(64), "
                + "channel_code varchar(64), "
                + "external_user_id varchar(64), "
                + "upstream_query_type varchar(32), "
                + "upstream_query_value varchar(128), "
                + "purpose varchar(32), "
                + "mapping_status varchar(32) not null, "
                + "created_ts timestamp not null, "
                + "updated_ts timestamp not null"
                + ")";
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

    private boolean hasIndex(DatabaseMetaData metaData, String tableName, String indexName) throws SQLException {
        try (ResultSet indexes = metaData.getIndexInfo(null, null, tableName, false, false)) {
            while (indexes.next()) {
                String currentIndexName = indexes.getString("INDEX_NAME");
                if (indexName.equalsIgnoreCase(currentIndexName)) {
                    return true;
                }
            }
        }
        return false;
    }
}
