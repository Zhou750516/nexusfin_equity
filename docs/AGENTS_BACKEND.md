# AGENTS Backend Guide

## Scope

Java 17, Spring Boot 3.2, MyBatis-Plus, and MySQL 8.0 backend code under `src/main/java/com/nexusfin/equity/`.

## Build and Verification

- Build: `mvn clean package -DskipTests`
- Test: `mvn test`
- MySQL regression: `MYSQL_IT_ENABLED=true MYSQL_IT_DATABASE=nexusfin_equity mvn -Dtest=MySqlRoundTripIntegrationTest,MySqlCallbackFlowIntegrationTest test`
- Checkstyle: `mvn checkstyle:check`

## Non-Negotiable Rules

- All REST APIs return `Result<T>`
- No business logic in controllers
- No hardcoded config values
- No `System.out.println`
- No database queries inside loops
- No swallowed exceptions

## Domain Rules

- Amount fields use `Long` in fen
- Sensitive fields must be encrypted at rest and queried with hash indexes
- Key business logs include `traceId + bizOrderNo`
