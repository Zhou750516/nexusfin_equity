# Quickstart: 惠选卡权益与借款流程 V1 页面交互理解

## Current Implemented Slice

截至 `2026-04-26`，当前工作区已形成以下实现切片：

### 1. Shared Foundation

- 补齐 Yunka 路径配置：协议、用户、还款计划、短信发送/确认、绑卡列表、图片查询、权益同步
- 新增 `XiaohuaGatewayService` facade 和 typed request/response 模型
- 补齐放款 / 还款回调 DTO，并接入 `NotificationCallbackController` + `NotificationServiceImpl`

### 2. Benefits Alignment

- `GET /api/benefits/card-detail` 已聚合动态协议与用户绑卡列表
- `POST /api/benefits/activate` 已增加协议 readiness 校验
- 权益开通成功后已触发 benefit sync

### 3. Loan Alignment

- `LoanApplyRequest` 已扩展 richer Xiaohua fields
- `7001 / 7002 / 7003` 已按 success / processing / failure 对齐
- 审批结果页已可按需补完整 repayment plan
- 放款结果回调已按新 DTO 语义处理

### 4. Repayment Alignment

- `GET /api/repayment/info/{loanId}` 已接入绑卡上下文
- 已新增 `POST /api/repayment/sms-send`
- 已新增 `POST /api/repayment/sms-confirm`
- 还款提交 / 查询已补 `swiftNumber`、卡上下文与 pending/success/failure 映射
- 还款结果回调已按新 DTO 语义处理

## Local Verification

### 1. Targeted feature verification

```bash
mvn -q -Dtest=YunkaPropertiesTest,XiaohuaGatewayServiceTest test
mvn -q -Dtest=BenefitsServiceTest,LoanServiceTest,RepaymentServiceTest,NotificationServiceTest,BenefitsControllerIntegrationTest,LoanControllerIntegrationTest,RepaymentControllerIntegrationTest,NotificationCallbackControllerIntegrationTest test
mvn -q -Dtest=NexusfinEquityApplicationTests,Phase9TaskGroupAIntegrationTest,Phase9TaskGroupCIntegrationTest,Phase9TaskGroupDIntegrationTest test
```

### 2. Full test suite

```bash
mvn -q test
```

### 3. Build verification

```bash
mvn -q clean package -DskipTests
```

### 4. Style verification

```bash
mvn -q checkstyle:check
```

## Latest Verification Record

### 2026-04-25 Targeted Verification

以下针对 Shared Foundation 与权益 / 借款 / 还款对齐的重点测试已实际执行并通过：

- `mvn -q -Dtest=YunkaPropertiesTest,XiaohuaGatewayServiceTest test`
- `mvn -q -Dtest=BenefitsServiceTest,LoanServiceTest,RepaymentServiceTest,NotificationServiceTest,BenefitsControllerIntegrationTest,LoanControllerIntegrationTest,RepaymentControllerIntegrationTest,NotificationCallbackControllerIntegrationTest test`
- `mvn -q -Dtest=NexusfinEquityApplicationTests,Phase9TaskGroupAIntegrationTest,Phase9TaskGroupCIntegrationTest,Phase9TaskGroupDIntegrationTest test`

### 2026-04-26 Full Verification

以下完整验证已实际执行并通过：

- `mvn -q test`
- `mvn -q clean package -DskipTests`
- `mvn -q checkstyle:check`

补充说明：

- `mvn -q test` 在沙箱环境下曾因 `RestYunkaGatewayClientTest` 需要绑定本地端口而失败一次；
- 提权后复跑确认属于环境限制，不是代码失败；
- 随后完整测试套件通过。

## Mock / Runtime Notes

- 测试环境默认使用本地配置与 mock / stub 数据完成回归
- `RestYunkaGatewayClientTest` 依赖本地端口绑定，受受限沙箱环境影响时需要在可绑定端口的环境中执行

## Review Entry

建议 review 先看以下文件：

1. `src/main/java/com/nexusfin/equity/service/XiaohuaGatewayService.java`
2. `src/main/java/com/nexusfin/equity/service/impl/XiaohuaGatewayServiceImpl.java`
3. `src/main/java/com/nexusfin/equity/service/impl/BenefitsServiceImpl.java`
4. `src/main/java/com/nexusfin/equity/service/impl/LoanServiceImpl.java`
5. `src/main/java/com/nexusfin/equity/service/impl/RepaymentServiceImpl.java`
6. `src/main/java/com/nexusfin/equity/service/impl/NotificationServiceImpl.java`
7. `src/test/java/com/nexusfin/equity/service/XiaohuaGatewayServiceTest.java`
8. `src/test/java/com/nexusfin/equity/service/BenefitsServiceTest.java`
9. `src/test/java/com/nexusfin/equity/service/LoanServiceTest.java`
10. `src/test/java/com/nexusfin/equity/service/RepaymentServiceTest.java`
