# nexusfin-equity

艾博生权益分发服务基线工程，基于 Java 17、Spring Boot 3.2、MyBatis-Plus 和 MySQL 8.0。

## Build And Verify

- `mvn test`
- `MYSQL_IT_ENABLED=true MYSQL_IT_DATABASE=nexusfin_equity mvn -Dtest=MySqlRoundTripIntegrationTest,MySqlCallbackFlowIntegrationTest test`
- `mvn clean package -DskipTests`
- `mvn checkstyle:check`

## Local MySQL

- 复用现有数据库：`nexusfin_equity`
- 本地连接方式：`mysql -uroot`
- 不需要重建数据库或数据表

## Covered Baseline Flows

- 静默注册、产品查询、权益订单创建
- 权益订单创建按 `request_id` 幂等回放
- 协议任务与合同归档落库
- 首次代扣、兜底代扣、放款/还款/行权/退款回调
- 下游 YunKa 同步与回调幂等
- 按 `benefit_order_no`、`member_id`、`payment_no`、`request_id` 的对账查询
