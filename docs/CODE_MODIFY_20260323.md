# 代码修改说明 (Code Modify) 文档
**项目**: NexusFin Equity Service  
**对应审查文档**: `docs/CODE_REVIEW_20260323.md`  
**修改日期**: 2026-03-23

## 1. 本次修改结论

本次根据 `CODE_REVIEW_20260323.md` 复核后，只落地了其中真正影响当前基线规格的一部分问题，并保持现有基线范围不被无关的生产化增强项扩散。

## 1.1 2026-03-24 跨域认证改造补充

本次新增了一轮跨域认证改造，核心变化如下：

1. 新增 `GET /api/auth/sso-callback`，用科技平台 token 做 SSO 回调登录
2. 新增 `GET /api/users/me`，供前端判定本地登录态
3. 新增本地 JWT + Cookie 认证过滤器
4. `/api/equity/**` 业务接口改为从本地登录态获取当前会员，不再接收显式 `memberId`
5. 下线旧的 `/api/users/register` 静默注册入口

交付说明补充：

1. `redirect_url` 只允许命中白名单前缀，避免开放重定向
2. 本地登录态通过 JWT Cookie 回写，Cookie 属性受配置控制，当前要求至少覆盖 `HttpOnly`、`Secure`、`SameSite`
3. SSO 回调只消费上游 `token` 后再跳转，不把上游 token 继续透传到业务落地页，避免把认证信息留在前端跳转 URL 中

涉及核心文件：

- `src/main/java/com/nexusfin/equity/controller/AuthController.java`
- `src/main/java/com/nexusfin/equity/service/impl/AuthServiceImpl.java`
- `src/main/java/com/nexusfin/equity/service/impl/TechPlatformUserClientImpl.java`
- `src/main/java/com/nexusfin/equity/config/JwtAuthenticationFilter.java`
- `src/main/java/com/nexusfin/equity/util/JwtUtil.java`
- `src/main/java/com/nexusfin/equity/util/CookieUtil.java`
- `src/main/java/com/nexusfin/equity/controller/BenefitOrderController.java`
- `src/main/java/com/nexusfin/equity/service/impl/BenefitOrderServiceImpl.java`
- `src/main/resources/application.yml`
- `src/main/resources/db/schema.sql`

## 1.2 本轮验证补充

已执行：

```bash
mvn test
mvn clean package -DskipTests
mvn checkstyle:check
```

结果：

- `mvn test` 通过
- `mvn clean package -DskipTests` 通过
- `mvn checkstyle:check` 通过

本次已完成两类核心调整：

1. 创建权益订单接口补齐请求级幂等能力
2. 回调通知在“订单不存在/处理失败”场景下补齐审计状态

## 1.3 2026-03-31 联调前开发收口补充

本次补充聚焦昨天计划中仍未完全收口的两项任务：`T021` 与 `T026`。

核心变化如下：

1. 补齐 `BenefitOrderController` 的认证态访问日志：
   - 产品页访问
   - 查单
   - 行权链接获取
2. 保持日志口径统一输出 `traceId + bizOrderNo`，便于联调阶段串联用户访问和订单链路。
3. 给 `TechPlatformUserClientImpl` 增加最小重试能力，避免上游用户校验接口的瞬时失败直接中断登录链路。
4. 给上游用户校验增加成功 / 失败日志留痕，明确记录：
   - `techPlatformPath`
   - `attempt/maxAttempts`
   - 上游返回失败或网络异常原因
5. 新增 `retry-max-attempts` 配置项，并同步测试环境默认值。

涉及核心文件：

- `src/main/java/com/nexusfin/equity/controller/BenefitOrderController.java`
- `src/main/java/com/nexusfin/equity/service/impl/TechPlatformUserClientImpl.java`
- `src/main/java/com/nexusfin/equity/config/AuthProperties.java`
- `src/main/resources/application.yml`
- `src/test/resources/application.yml`
- `src/test/resources/application-mysql-it.yml`
- `src/test/java/com/nexusfin/equity/controller/BenefitOrderControllerIntegrationTest.java`
- `src/test/java/com/nexusfin/equity/service/impl/TechPlatformUserClientImplTest.java`

未修改的项，主要属于以下两类：

1. 审查文档提出了合理但不属于当前基线强制范围的增强建议
2. 审查文档对现有规格的理解存在偏差，不适合直接按建议修改

## 2. 已修改内容

### 2.1 创建权益订单接口缺少幂等性

**来源于审查文档**

- `CODE_REVIEW_20260323.md` 将“创建权益订单接口缺少幂等性”列为高优先级问题

**结论**

- 该问题属实，且与当前规格一致冲突，必须修改

**原因**

- 规格 `FR-013` 明确要求：所有状态变更的入站交互必须支持 request-level idempotency
- 原实现的创建订单接口没有 `requestId` 字段，也没有重复请求回放逻辑

**实际修改**

- 在 `CreateBenefitOrderRequest` 中新增 `requestId`
- `BenefitOrderServiceImpl.createOrder()` 先检查 `idempotency_record`
- 如果同一个 `requestId` 已处理，则回放已存在的 `benefit_order_no`
- 新建订单时将业务请求号写入 `benefit_order.request_id`
- 订单创建成功后写入 `idempotency_record`

**涉及文件**

- `src/main/java/com/nexusfin/equity/dto/request/CreateBenefitOrderRequest.java`
- `src/main/java/com/nexusfin/equity/service/impl/BenefitOrderServiceImpl.java`
- `src/test/java/com/nexusfin/equity/service/BenefitOrderServiceTest.java`
- `src/test/java/com/nexusfin/equity/controller/BenefitOrderControllerIntegrationTest.java`
- `src/test/java/com/nexusfin/equity/NexusfinEquityApplicationTests.java`
- `src/test/java/com/nexusfin/equity/controller/MySqlRoundTripIntegrationTest.java`
- `specs/001-equity-service-baseline/contracts/abs-public-api.yaml`
- `specs/001-equity-service-baseline/quickstart.md`
- `README.md`

### 2.2 回调接口在订单不存在时缺少清晰审计状态

**来源于审查文档**

- `CODE_REVIEW_20260323.md` 提到行权、退款、还款等回调在订单不存在时处理策略不一致

**结论**

- 该问题部分属实，需要调整，但不是按审查文档原样修改

**原因**

- 基线规格要求：当关联订单不存在或状态不匹配时，系统必须记录异常并支持后续追查
- 原实现虽然写入了通知日志，但默认直接记为 `PROCESSED`
- 这会让“缺单”场景在审计上看起来像成功处理，不利于对账和人工排障

**实际修改**

- 回调通知先统一记录为 `RECEIVED`
- 业务处理成功后更新为 `PROCESSED`
- 如果订单不存在，或者处理过程中抛出运行时异常，则更新为 `FAILED`
- 还款回调也会校验订单是否存在；不存在时仅记录失败审计，不误写幂等成功记录

**涉及文件**

- `src/main/java/com/nexusfin/equity/service/impl/NotificationServiceImpl.java`
- `src/test/java/com/nexusfin/equity/service/NotificationServiceTest.java`
- `src/test/java/com/nexusfin/equity/controller/NotificationCallbackControllerIntegrationTest.java`

## 3. 未修改内容

以下项目来自 `CODE_REVIEW_20260323.md`，但本次没有改动。

### 3.1 还款回调必须更新订单状态

**审查建议**

- 审查文档认为还款回调“仅记录通知，不更新订单状态”是不完整实现，建议直接更新订单状态

**本次未修改**

- 未按该建议修改

**原因**

- 当前基线规格 `FR-010` 的核心要求是：消费 repayment 通知，并将其关联到正确权益订单，供对账、客服、争议处理使用
- 数据模型也明确写了 repayment notification 是“丰富 servicing state”，而不是替代 payment lifecycle
- 当前基线没有定义单独的“还款主状态字段”或“还款记录表”
- 在没有清晰领域模型前，强行改 `benefit_order.order_status` 反而可能污染订单主状态机

**处理策略**

- 本次只补“缺单时的失败审计”
- 如果后续进入生产化阶段，可以新增专门的 repayment record / servicing state 模型

### 3.2 行权链接安全性增强

**审查建议**

- 审查文档建议给行权链接增加签名、Token、过期配置化等安全设计

**本次未修改**

- 未改

**原因**

- 这是合理的生产化增强项，但不属于当前基线必须修复的问题
- 当前基线的目标是打通接口、状态流转、审计和幂等闭环，不是完成真实外部平台安全集成
- 若此时引入签名 Token 机制，需要同步增加验签、密钥配置、回放策略和测试矩阵，属于下一阶段工作

### 3.3 Controller 路径参数补 `@NotBlank`

**审查建议**

- 审查文档建议给多个路径参数加 `@NotBlank`

**本次未修改**

- 未改

**原因**

- 这是低风险增强项，不属于当前最关键缺口
- Spring 对 path variable 的路由本身已提供基础约束
- 与“创建订单幂等缺失”和“回调异常审计缺失”相比，优先级明显更低

### 3.4 健康检查、查询接口补日志/缓存/监控

**审查建议**

- 审查文档建议健康检查接口补日志、查询接口补缓存或监控

**本次未修改**

- 未改

**原因**

- 这类建议偏运维增强，不是当前基线验收阻塞项
- 当前重点仍是业务闭环正确性、幂等性和可审计性

### 3.5 支付、退款等金额范围的更严格校验

**审查建议**

- 审查文档建议增加更多金额上下限校验

**本次未修改**

- 未改

**原因**

- 当前 DTO 已覆盖正数或非负约束的基础校验
- 更细的业务金额上限规则需要明确产品和渠道约束来源，否则容易变成硬编码
- 这部分适合作为后续业务规则配置化工作处理

## 4. 本次回归验证

### 4.1 H2 / 单元与集成测试

已执行：

```bash
mvn -q test
```

结果：

- 通过
- 本轮最新代码状态下重新执行通过
- 共覆盖 56 个测试用例，跳过 2 个仅在 `MYSQL_IT_ENABLED=true` 时启用的真实 MySQL 用例

### 4.2 打包验证

已执行：

```bash
mvn -q clean package -DskipTests
```

结果：

- 通过
- 已在最新代码状态下重新执行，产物可正常生成到 `target/`

### 4.3 Checkstyle

已执行：

```bash
mvn -q checkstyle:check
```

结果：

- 通过
- 已在补充 MySQL 回归前置逻辑后再次确认通过

### 4.4 2026-03-31 联调前收口验证

已执行：

```bash
mvn -q -Dtest=BenefitOrderControllerIntegrationTest,TechPlatformUserClientImplTest test
```

结果：

- 通过
- 已确认产品页、查单、行权链接访问日志输出符合预期
- 已确认上游用户校验在瞬时失败后会按配置执行重试

后续全量验证见当天的执行记录与计划文档。

### 4.4 真实 MySQL 回归

已执行：

```bash
MYSQL_IT_ENABLED=true MYSQL_IT_DATABASE=nexusfin_equity \
mvn -q -Dtest=MySqlRoundTripIntegrationTest,MySqlCallbackFlowIntegrationTest test
```

结果：

- 最终通过
- `MySqlRoundTripIntegrationTest` 已验证真实 MySQL 写入并回读以下对象：
  - `member_info`
  - `member_channel`
  - `benefit_product`
  - `benefit_order`
  - `sign_task`
  - `contract_archive`
  - `idempotency_record`
- `MySqlCallbackFlowIntegrationTest` 已验证回调链路在真实 MySQL 下继续可写且可推进订单状态

补充说明：

- 首次真实 MySQL 回归暴露了一个存量库兼容问题：本地复用的 `nexusfin_equity.member_info` 缺少 `tech_platform_user_id` 列
- 为兼容“复用已有本地数据库、不重建库表”的约束，已在 [MySqlRoundTripIntegrationTest](/Users/lixiaokun/Projects/nexusFin/nexusfin-equity/src/test/java/com/nexusfin/equity/controller/MySqlRoundTripIntegrationTest.java) 增加测试前置 schema 对齐逻辑
- 当前逻辑会在真实 MySQL 回归开始前检查并补齐 `member_info.tech_platform_user_id` 列及其唯一索引，然后再执行写库验证

## 5. 总结

本次不是“按审查文档逐条照改”，而是做了基于规格、实现和测试现状的二次判断。

本次最终落地的修改重点是：

1. 修复了创建订单接口缺少 request-level 幂等的问题
2. 修复了通知回调在缺单/异常场景下审计状态不清晰的问题

本次明确没有修改的内容主要是：

1. 还款主状态建模不足但未到适合硬改 `benefit_order` 的程度
2. 行权链接安全化、缓存、监控、更多日志等生产化增强项
3. 部分低优先级参数校验增强

因此，`CODE_REVIEW_20260323.md` 的结论中：

- **已采纳并修改**: 创建订单幂等、回调异常审计一致性
- **部分采纳但调整实现方式**: 回调接口订单不存在处理
- **暂未采纳**: 还款回调直接改订单状态、行权链接安全增强、缓存/监控/日志增强、部分低优先级参数校验
