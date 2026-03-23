# Quickstart: NexusFin Equity Service Baseline

## Purpose

This quickstart verifies that the ABS-side baseline service can support the
documented feature scope before feature-level implementation expands on it.

## Prerequisites

- Java 17 available locally
- Maven available locally
- MySQL 8.0 available when running against a real database
- Optional local test database through H2 for isolated test execution
- Local MySQL can be connected with `mysql -uroot` and reuses the existing
  `nexusfin_equity` schema without rebuilding tables

## Verification Steps

1. Confirm the current feature branch is active:

```bash
git branch --show-current
```

Expected result: `001-equity-service-baseline`

2. Run unit and Spring context tests:

```bash
mvn test
```

This now includes the quickstart smoke flow in
`src/test/java/com/nexusfin/equity/NexusfinEquityApplicationTests.java`, which
covers:

- silent registration
- product page loading
- benefit order creation
- first deduct success callback
- YunKa grant-forward callback
- exercise URL query
- reconciliation lookup by order number, member ID, and request ID

3. Run real-MySQL round-trip verification against the existing local schema:

```bash
MYSQL_IT_ENABLED=true MYSQL_IT_DATABASE=nexusfin_equity \
mvn -Dtest=MySqlRoundTripIntegrationTest,MySqlCallbackFlowIntegrationTest test
```

4. Verify the project packages cleanly:

```bash
mvn clean package -DskipTests
```

5. Run style and static consistency checks:

```bash
mvn checkstyle:check
```

## Baseline Functional Validation

After implementation fills in the baseline feature, validate these business
flows:

1. Push a rejected user into the registration endpoint and confirm:
   - one `member_id` is returned
   - duplicate replay does not create a second member
   - channel linkage is present

2. Create a benefit order and confirm:
   - a `benefit_order_no` is created
   - signing tasks are generated or tracked
   - downstream sync is recorded for both the direct-continue path and the
     fallback-eligible path

3. Replay first deduct, fallback deduct, grant, repayment, exercise, and refund
   callbacks and confirm:
   - processing is idempotent
   - order status changes are legal
    - payment and notification logs remain queryable
   - reconciliation queries can locate records by `benefit_order_no`,
     `member_id`, `payment_no`, and `request_id`

## Documentation Cross-Check

For any interface or table change, update and review against:

- `docs/惠聚项目-技术概要设计方案.pdf`
- `docs/NexusFin 接口文档.pdf`
- `docs/NexusFin 数据库ER关系说明.pdf`
- `docs/NexusFin 接口+数据表梳理.pdf`

## Exit Criteria

The baseline plan is considered executable when:

- build and test commands are green
- real-MySQL verification passes against the reused `nexusfin_equity` schema
- the documented ABS flows can be implemented without unresolved contract gaps
- `benefit_order_no` remains the primary ABS-to-YunKa business linkage
- callback handling is safe under retries and replays
