# Quickstart: NexusFin Equity Service Baseline

## Purpose

This quickstart verifies the current auth-first ABS service entry flow:

1. SSO callback login
2. Current-user lookup
3. Product access
4. Benefit order creation
5. Callback-driven order progression

## Verification Steps

1. Run unit and integration tests:

```bash
mvn test
```

2. Run packaging verification:

```bash
mvn clean package -DskipTests
```

3. Run style checks:

```bash
mvn checkstyle:check
```

4. Optional real-MySQL verification:

```bash
MYSQL_IT_ENABLED=true MYSQL_IT_DATABASE=nexusfin_equity \
mvn -Dtest=MySqlRoundTripIntegrationTest,MySqlCallbackFlowIntegrationTest test
```

Notes:

- `MySqlRoundTripIntegrationTest` will write and read back real data in the local `nexusfin_equity` database.
- To support local-database reuse, the test first aligns `member_info.tech_platform_user_id` and its unique index if they are missing, then runs the write-path verification.
- This alignment is only for local regression convenience. Delivery environments still need an explicit schema upgrade before rollout.

## Baseline Functional Validation

Validate these flows:

1. Browser hits `GET /api/auth/sso-callback` and receives local auth cookie.
2. Frontend calls `GET /api/users/me` and resolves current member.
3. Logged-in user calls:
   - `GET /api/equity/products/{productCode}`
   - `POST /api/equity/orders`
4. Signed callback endpoints continue to handle:
   - first deduct
   - fallback deduct
   - grant
   - repayment
   - exercise
   - refund

## Exit Criteria

The baseline is considered executable when:

- auth-first browser login works through SSO callback
- `/api/equity/**` business endpoints only work with valid local JWT cookie
- `/api/callbacks/**` still work with signed partner requests
- order and callback flows remain idempotent and traceable
