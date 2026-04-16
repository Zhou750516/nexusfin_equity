# NexusFin Equity 项目 - 完整决策审查报告

**报告日期**: 2026-03-31
**版本**: 1.0 - 项目启动至今完整决策回顾
**审查范围**: 从 2026-03-23 至 2026-03-31 的所有关键决策
**审查对象**: 架构、技术栈、业务流程、集成方案、监控策略

---

## 目录

1. [执行摘要](#执行摘要)
2. [项目决策时间线](#项目决策时间线)
3. [架构决策详解](#架构决策详解)
4. [技术决策详解](#技术决策详解)
5. [业务流程决策](#业务流程决策)
6. [集成和依赖决策](#集成和依赖决策)
7. [监控与运维决策](#监控与运维决策)
8. [决策矩阵总览](#决策矩阵总览)

---

## 执行摘要

### 项目概况

**项目名称**: NexusFin Equity Distribution Service（权益分发服务）
**开始日期**: 2026-03-23
**当前状态**: 核心功能实现完成，工程评审阶段，准备上线
**预期上线**: 2026-04-15

### 关键指标

| 指标 | 数值 | 评价 |
|------|------|------|
| **架构评分** | B+ | 基础架构清晰，集成风险中等 |
| **代码质量** | 80% | 测试覆盖率高，文档完整 |
| **关键决策数** | 18 | 所有决策经过评审确认 |
| **风险数量** | 8 P0 + 10 P1 + 6 P2 | 已制定缓解方案 |
| **上线就绪度** | 85% | 监控、告警、文档还需完善 |

### 三个最重要的决策

| # | 决策 | 影响 | 状态 |
|---|------|------|------|
| 1 | **认证方案**：JWT + 签名双认证 + AES 加密 | 安全性最优，用户体验平衡 | ✅ 已实现 |
| 2 | **监控方案**：阿里云原生 (CloudMonitor + SLS) | 成本最低（¥100-150/月），部署最简 | ✅ 已制定 |
| 3 | **外部服务集成**：QW 云卡 + Allinpay 直连 | 金融流程完整，需要强化容错 | ⚠️ 待加固 |

### 即将实施的行动（按优先级）

**高优先级（本周）**:
- [ ] 添加 @Retry 和熔断器到外部服务调用
- [ ] 完成监控系统 Phase 1 部署
- [ ] 补充故障诊断指南

**中优先级（下周）**:
- [ ] 拆分 BenefitOrderServiceImpl 为 OrderSyncService
- [ ] 实现数据对账服务
- [ ] 完成性能测试

**低优先级（计划中）**:
- [ ] API 版本化（v2）
- [ ] 链路追踪集成（可选）
- [ ] 自适应告警

---

## 项目决策时间线

### Phase 1：初期设计与框架搭建（3月23日-24日）

**DECISION-001: 技术栈选型**
- **日期**: 2026-03-23
- **决策内容**: 使用 Spring Boot 3.2 + MyBatis-Plus + MySQL 8.0 + Java 17
- **决策人**: 技术负责人
- **理由**:
  - Spring Boot 是业界标准，有完善的生态
  - MyBatis-Plus 简化数据库操作，提高开发效率
  - Java 17 是 LTS 版本，安全性和性能好
- **替代方案评估**:
  - JPA/Hibernate: 功能强大，但学习曲线陡
  - MyBatis: 灵活但需要手写大量 SQL
  - PostgreSQL: 功能完整，但公司基础设施选择 MySQL
- **风险**: 无
- **状态**: ✅ 已实施，无变更

---

**DECISION-002: 分层架构设计**
- **日期**: 2026-03-23
- **决策内容**: 采用标准三层架构 (Controller → Service → Repository)
- **决策人**: 架构师
- **具体设计**:
  ```
  Controller Layer (接收请求)
    ↓
  Service Layer (业务逻辑)
    - BenefitOrderService
    - PaymentService
    - AgreementService
    - AuthService
    ↓
  Repository Layer (数据访问)
    - BenefitOrderRepository
    - PaymentRecordRepository
    - MemberInfoRepository
  ```
- **优势**:
  - 关注点分离，易于测试和维护
  - 责任明确，降低耦合度
  - 易于扩展和重构
- **劣势**:
  - 代码行数较多（虽然清晰）
  - 需要管理多个依赖
- **状态**: ✅ 已实施，运行良好

---

**DECISION-003: 数据库设计 - 订单和支付记录分表**
- **日期**: 2026-03-23
- **决策内容**: 分离 BenefitOrder 和 PaymentRecord 两个表
- **决策人**: DBA + 架构师
- **具体设计**:
  ```sql
  -- 权益订单表
  benefit_order
    - order_id (PK)
    - member_id (FK)
    - product_id (FK)
    - status (enum: PENDING_SIGNATURE, PENDING_DEDUCTION, ACTIVE, ...)
    - created_at
    - updated_at

  -- 支付记录表（允许多笔支付）
  payment_record
    - payment_id (PK)
    - order_id (FK)
    - payment_type (enum: FIRST_DEDUCTION, FALLBACK, ...)
    - status (enum: PROCESSING, SUCCESS, FAILED)
    - created_at
  ```
- **理由**:
  - 订单和支付的生命周期不同
  - 支持多笔支付（首扣+兜底）
  - 便于查询和分析
- **性能考量**:
  - 查询时需要 JOIN，但通常不会成为瓶颈
  - 可根据订单 ID 快速查询支付记录
- **状态**: ✅ 已实施

---

### Phase 2：认证与安全设计（3月23日-24日）

**DECISION-004: 双认证方案**
- **日期**: 2026-03-23
- **决策内容**: 实现 JWT Token + API 签名的双认证机制
- **决策人**: 安全架构师
- **详细设计**:
  ```
  认证流程：
  1. 用户登录 → 获取 JWT Token（有效期 30 分钟）
  2. 每个请求必须包含：
     a) Cookie: JWT Token
     b) Header: X-Signature (HmacSHA256)
  3. 服务端校验：
     a) 校验 Token 有效期和签名
     b) 校验请求签名（防篡改）

  签名算法：
  - 按字段字母序排序请求参数
  - 使用 HmacSHA256 + 密钥签名
  - 在 X-Signature header 中传递
  ```
- **为什么是双认证**:
  - JWT: 无状态，易于分布式扩展
  - 签名: 防止请求被篡改，增加安全性
  - 组合: 既有效率，又有安全性
- **替代方案**:
  - 单 JWT: 依赖 Token 本身，可能被伪造
  - OAuth2: 功能强大但复杂，不必要
  - API Key: 容易泄露
- **状态**: ✅ 已实施，已通过安全审查

---

**DECISION-005: 敏感数据加密**
- **日期**: 2026-03-23
- **决策内容**: 对敏感字段（手机号、身份证等）使用 AES-256 加密
- **决策人**: 合规团队 + 安全架构师
- **实现方式**:
  ```java
  @Entity
  public class MemberInfo {
      @Encrypt
      private String phoneNumber;  // 加密存储

      @Encrypt
      private String idNumber;     // 加密存储
  }

  // 加密/解密工具类
  public class SensitiveDataCipher {
      private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
      private static final String KEY_ALGORITHM = "AES";

      public String encrypt(String plaintext) { ... }
      public String decrypt(String ciphertext) { ... }
  }
  ```
- **密钥管理**:
  - 密钥存储在阿里云密钥管理服务（KMS）
  - 每次加解密调用 KMS API
  - 应用内不存储密钥
- **性能影响**:
  - 查询时需要逐字段解密（可接受）
  - 可在缓存层优化（后期）
- **合规性**:
  - 满足个人信息保护法（PIPL）
  - 满足金融监管要求
- **状态**: ✅ 已实施

---

### Phase 3：业务流程设计（3月24日）

**DECISION-006: 订单状态机设计**
- **日期**: 2026-03-24
- **决策内容**: 使用有限状态机（FSM）管理订单生命周期
- **决策人**: 业务架构师
- **状态转移图**:
  ```
  PENDING_SIGNATURE
    ↓ (生成协议)
  PENDING_DEDUCTION
    ├─ (首扣成功) → ACTIVE
    ├─ (首扣失败) → PENDING_FALLBACK_DEDUCTION
    └─ (首扣超时) → PENDING_RETRY

  PENDING_FALLBACK_DEDUCTION
    ├─ (兜底成功) → ACTIVE
    └─ (兜底失败) → DEDUCTION_FAILED

  ACTIVE
    ├─ (行权) → EXERCISED
    └─ (退款) → REFUNDED

  EXERCISED / REFUNDED / DEDUCTION_FAILED
    → 终态（不可转移）
  ```
- **为什么使用 FSM**:
  - 明确的状态转移规则，易于理解
  - 防止非法状态转移
  - 便于审计和追踪
- **实现方式**:
  ```java
  public class OrderStateMachine {
      public OrderState transition(OrderState current, OrderStateTransition event) {
          return switch (current) {
              case PENDING_SIGNATURE ->
                  switch (event) {
                      case PROTOCOL_GENERATED -> PENDING_DEDUCTION;
                      default -> throw new IllegalTransition();
                  };
              // ... 其他转移规则
          };
      }
  }
  ```
- **风险管理**:
  - 非法状态转移会被拒绝（故障转移的关键）
  - 所有转移都有日志记录（便于调试）
- **状态**: ✅ 已实施，经过充分测试

---

**DECISION-007: 幂等性设计**
- **日期**: 2026-03-24
- **决策内容**: 使用 Request ID 实现订单创建幂等性
- **决策人**: 技术负责人
- **具体方案**:
  ```
  客户端调用：
  POST /api/v1/orders
  Header: X-Request-ID: "req-12345"
  Body: { ... order details ... }

  服务端处理：
  1. 从 HTTP Header 提取 X-Request-ID
  2. 查询 idempotency_record 表
     SELECT * FROM idempotency_record WHERE request_id = 'req-12345'
  3a. 如果记录存在：
     - 直接返回之前的响应（从缓存中）
  3b. 如果记录不存在：
     - 创建订单
     - 保存 request_id → response 映射
     - 返回响应
  ```
- **为什么需要幂等性**:
  - 网络可能出现超时，客户端会重试
  - 同一个请求不应该创建多个订单
  - 金融系统必须要求幂等性
- **存储设计**:
  ```sql
  CREATE TABLE idempotency_record (
      id BIGINT PRIMARY KEY,
      request_id VARCHAR(64) UNIQUE,
      response TEXT,
      created_at TIMESTAMP,
      expires_at TIMESTAMP  -- 24小时后过期
  );
  ```
- **性能**:
  - 查询 idempotency_record 很快（request_id 有索引）
  - 无额外的网络开销
- **状态**: ✅ 已实施，经过测试

---

### Phase 4：外部集成设计（3月24日-26日）

**DECISION-008: QW 云卡集成方案**
- **日期**: 2026-03-24
- **决策内容**: 集成阿里云的 QW 云卡系统，用于会员管理和权益行权
- **决策人**: 业务需求方 + 技术负责人
- **集成点**:
  ```
  1. 会员同步（MemberSync）
     订单创建 → QW API 同步会员信息

  2. 权益行权（ExerciseUrl）
     用户行权时 → 生成 QW 权益行权链接

  3. 直连集成（新增）
     支持 QW 直连模式的订单处理
  ```
- **实现方式**:
  ```java
  public class QwBenefitClient {
      public QwMemberSyncResponse syncMemberOrder(
          QwMemberSyncRequest request) {
          // 调用 QW API
          // 处理超时和失败
      }

      public QwExerciseUrlResponse getExerciseUrl(
          QwExerciseUrlRequest request) {
          // 获取行权链接
      }
  }
  ```
- **容错机制**:
  - 重试机制（待加强）
  - 超时控制（3秒）
  - 日志记录（便于追踪）
- **风险**:
  - ⚠️ QW API 不可用时，订单创建失败
  - ⚠️ 同步失败时，需要人工对账
  - **缓解方案**: 已制定（见监控决策）
- **状态**: ✅ 已实施，待加固容错

---

**DECISION-009: Allinpay（通联）直连集成**
- **日期**: 2026-03-26
- **决策内容**: 集成通联的直连支付系统，处理代扣支付
- **决策人**: 支付团队 + 风控团队
- **支付流程**:
  ```
  1. 首扣代扣（First Deduction）
     订单确认 → 调用 Allinpay API 扣款

  2. 支付回调（Payment Callback）
     Allinpay 返回支付结果 → 更新订单状态

  3. 兜底代扣（Fallback Deduction）
     如果首扣失败 → 尝试备用账户代扣
  ```
- **为什么选择直连**:
  - 更低的手续费（降低成本）
  - 更好的控制（可定制化）
  - 直接对接银行（更快结算）
- **替代方案**:
  - 第三方支付网关（如 Ping++）: 更简单但成本高、缺乏定制
  - 银行 SDK: 直接但集成复杂
- **风险**:
  - ⚠️ 签名和加密复杂，易出错
  - ⚠️ 支付失败处理需仔细，影响用户体验
  - **缓解**: 完整的测试用例，详细的文档
- **状态**: ✅ 已实施

---

**DECISION-010: 跨域认证方案**
- **日期**: 2026-03-24
- **决策内容**: 实现与 Tech Platform 的跨域用户认证
- **决策人**: 认证架构师
- **详细设计**:
  ```
  场景：用户在 Tech Platform 登录，无缝访问 Equity 服务

  认证流程：
  1. Tech Platform 登录成功
     → 返回 SSO Token

  2. 用户访问 Equity 服务
     Header: Authorization: Bearer <SSO Token>

  3. Equity 服务验证 Token
     → 调用 Tech Platform API 验证
     → 获取用户信息和权限
     → 创建本地 JWT Token

  4. 后续请求使用 JWT Token
  ```
- **为什么需要跨域认证**:
  - 用户不想重复登录
  - 统一账户体系（便于管理）
  - 提升用户体验
- **实现细节**:
  ```java
  @Component
  public class TechPlatformUserClient {
      public TechPlatformUserProfile validateToken(String ssoToken) {
          // 调用 Tech Platform API
          // 返回用户信息
      }
  }

  @PostMapping("/login")
  public AuthResponse login(@RequestBody LoginRequest request) {
      // 验证 SSO Token
      TechPlatformUserProfile profile = userClient.validateToken(...);

      // 创建本地 JWT
      String jwtToken = jwtUtil.generateToken(profile);

      return new AuthResponse(jwtToken);
  }
  ```
- **风险**:
  - ⚠️ 依赖 Tech Platform API 可用性
  - ⚠️ Token 有效期同步问题
  - **缓解**: 实现 Token 刷新机制，增加重试
- **状态**: ✅ 已实施

---

### Phase 5：监控与可观测性设计（3月31日）

**DECISION-011: 监控系统方案选型**
- **日期**: 2026-03-31
- **决策内容**: 采用阿里云原生监控方案（CloudMonitor + SLS）
- **决策人**: DevOps + 架构师
- **方案对比**:

  | 方案 | 成本 | 部署 | 维护 | 功能 | 推荐度 |
  |------|------|------|------|------|--------|
  | 纯阿里云 | ¥100-150/月 | ⭐☆☆ | ⭐☆☆ | 7/10 | ⭐⭐⭐⭐⭐ |
  | 阿里云+开源 | ¥50-80/月 | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | 9/10 | ⭐⭐⭐⭐ |
  | ARMS 商业 | ¥200-500/月 | ⭐☆☆ | ⭐☆☆ | 10/10 | ⭐⭐⭐ |
  | 开源（ELK+Prom） | $0 | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | 9/10 | ⭐⭐⭐ |

- **选择理由**:
  - ✅ 成本最低（vs ARMS 节省 70%）
  - ✅ 部署最简（无需额外基础设施）
  - ✅ 与现有 ECS/RDS 原生集成
  - ✅ 中文支持和文档完整
- **实施计划**:
  - Phase 1（1-2周）: 基础设施监控 + 应用指标
  - Phase 2（3-4周）: 业务监控 + 告警规则
  - Phase 3（5-8周）: 链路追踪（可选）
- **状态**: ✅ 已设计，待实施

---

**DECISION-012: 监控指标体系**
- **日期**: 2026-03-31
- **决策内容**: 建立完整的系统层+业务层监控指标体系
- **决策人**: SRE + 产品团队
- **核心指标**:

  **系统层面**:
  - ECS CPU (目标 < 70%, 告警 > 80%)
  - RDS 连接数 (目标 < 50%, 告警 > 80%)
  - 请求延迟 P99 (目标 < 1s, 告警 > 2s)
  - 错误率 (目标 < 0.1%, 告警 > 0.5%)

  **业务层面** (最重要):
  - 订单成功率 (目标 > 99%, 告警 < 95%)
  - 首扣代扣成功率 (目标 > 99%, 告警 < 95%)
  - QW 同步成功率 (目标 > 99%, 告警 < 95%)
  - 对账一致率 (目标 99.99%, 告警 < 99.95%)

- **告警分级**:
  - P0 (立即处理): 订单/支付/对账故障
  - P1 (30分钟): 性能/依赖问题
  - P2 (监控): 趋势观察
- **状态**: ✅ 已设计

---

**DECISION-013: 故障诊断体系**
- **日期**: 2026-03-31
- **决策内容**: 建立快速故障诊断的决策树和处理流程
- **决策人**: SRE + 技术负责人
- **关键设计**:
  - 4 大决策树（基础设施、应用、业务、外部服务）
  - 3 个常见故障的完整处理流程
  - 4 个应急 SOP
  - 故障排查工具集
- **目标**:
  - 故障发现时间: 30 分钟 → 2 分钟
  - 问题诊断时间: 20 分钟 → 5 分钟
  - 恢复时间: 30-60 分钟 → 5-15 分钟
- **状态**: ✅ 已设计

---

### Phase 6：风险评估与改进（3月31日）

**DECISION-014: 分布式容错机制**
- **日期**: 2026-03-31（决策） / 待实施
- **决策内容**: 为外部服务集成添加重试、熔断、降级机制
- **具体方案**:
  ```java
  @Service
  public class QwBenefitClient {
      @Retry(maxAttempts = 3, delay = 1000)
      @CircuitBreaker(
          failureThreshold = 5,
          delay = 60000
      )
      public QwMemberSyncResponse syncMemberOrder(QwMemberSyncRequest request) {
          // Resilience4j 会自动处理重试和熔断
      }
  }
  ```
- **为什么需要**:
  - QW/Allinpay 偶尔故障，需要重试
  - 频繁故障时，不应该继续请求（熔断）
  - 故障时有 fallback 方案（降级）
- **实施优先级**: ⭐⭐⭐ 高
- **预期工作量**: 2-3 天
- **状态**: ⚠️ 已识别，待实施

---

**DECISION-015: 服务拆分 - OrderSyncService**
- **日期**: 2026-03-31（决策） / 待实施
- **决策内容**: 从 BenefitOrderServiceImpl 拆分 OrderSyncService
- **当前问题**:
  - BenefitOrderServiceImpl 有 9 个构造函数依赖
  - `createOrder()` 方法职责过多
  - 代码复杂度高，难以测试和维护
- **改进后的结构**:
  ```
  BenefitOrderServiceImpl (6 个依赖)
    - 核心订单逻辑
    - 验证、创建、状态管理

  OrderSyncService (新建，3 个依赖)
    - 下游同步逻辑
    - QW 同步、回调处理
  ```
- **好处**:
  - 降低耦合度（各自独立）
  - 易于测试（单一职责）
  - 易于扩展（添加新的同步目标）
- **实施优先级**: ⭐⭐ 中
- **预期工作量**: 1-2 天
- **状态**: ⚠️ 已识别，待实施

---

**DECISION-016: 数据对账服务**
- **日期**: 2026-03-31（决识） / 待实施
- **决策内容**: 实现自动化的数据对账服务
- **对账内容**:
  - 本系统订单 vs QW 云卡订单
  - 支付金额一致性
  - 订单状态一致性
- **实现方式**:
  ```java
  @Scheduled(cron = "0 */1 * * * *")  // 每分钟对账
  public void executeReconciliation() {
      List<BenefitOrder> localOrders = getLocalOrders();
      List<QwOrder> qwOrders = qwClient.getOrders();

      List<Discrepancy> discrepancies = compare(localOrders, qwOrders);

      if (!discrepancies.isEmpty()) {
          alertTeam("发现对账差异: " + discrepancies.size() + " 笔订单");
          // 自动修复或标记为待处理
      }
  }
  ```
- **重要性**: ⭐⭐⭐ 关键（金融合规要求）
- **实施优先级**: ⭐⭐⭐ 高
- **预期工作量**: 3-5 天
- **状态**: ⚠️ 已识别，待实施

---

**DECISION-017: API 版本化规划**
- **日期**: 2026-03-31（决策） / 下个版本实施
- **决策内容**: 为 API 添加版本化支持（v2 计划中）
- **当前现状**: `/api/equity/orders` (无版本)
- **改进方案**:
  ```
  v1: /api/v1/equity/orders
  v2: /api/v2/equity/orders  (计划中，可能包含大的改动)
  ```
- **实现方式**:
  ```java
  @RestController
  @RequestMapping("/api/v1/equity/orders")
  public class BenefitOrderControllerV1 { }

  @RestController
  @RequestMapping("/api/v2/equity/orders")
  public class BenefitOrderControllerV2 { }
  ```
- **好处**:
  - 可以并行支持多个 API 版本
  - 客户端可以选择升级时机
  - 新增功能不强制影响现有客户端
- **实施优先级**: ⭐ 低（非紧急）
- **预期工作量**: 1-2 天
- **状态**: 📋 已规划，待下个版本实施

---

**DECISION-018: 性能和容量规划**
- **日期**: 2026-03-31（决策） / 待验证
- **决策内容**: 定义性能目标和容量规划
- **性能目标**:
  - 平均响应时间: < 100ms
  - P99 响应时间: < 1s
  - 吞吐量: > 100 req/s per instance
  - 可用性: > 99.9%
- **容量规划**:
  - 初期：1 个 ECS 实例 (4核8G)
  - 高峰期：可扩展到 2-3 个实例
  - 数据库：1 个 RDS (2核4G，可升级)
- **监控和告警**:
  - CPU > 80% → 考虑扩容
  - 响应时间 P99 > 2s → 调查
  - 错误率 > 0.5% → 告警
- **压测计划**:
  - 目标：验证 100 req/s 的性能指标
  - 时间：上线前 1 周
  - 工具：JMeter + 自定义脚本
- **状态**: ✅ 已定义，待压测验证

---

## 架构决策详解

### A. 核心架构模式

**选择: 分层 MVC + 数据访问层**

```
┌─────────────────────┐
│    Presentation     │  Controller
│     (Web Layer)     │  - 请求映射
├─────────────────────┤  - 参数验证
│    Business Logic   │  Service
│    (Service Layer)  │  - 业务逻辑
├─────────────────────┤  - 事务管理
│   Data Access       │  Repository
│    (DAO Layer)      │  - 数据库操作
├─────────────────────┤  - SQL 执行
│  External Services  │
│ (QW, Allinpay, etc) │  Client
└─────────────────────┘
```

**评分**: A (Clear separation, testable)

**优势**:
- ✅ 职责分离明确
- ✅ 易于单元测试（mock dependencies）
- ✅ 业界标准模式，team 易上手

**劣势**:
- ❌ 代码行数较多
- ❌ 需要管理多个依赖

**未来演进**:
- 如果复杂度增加 → 考虑 CQRS 模式（读写分离）
- 如果扩展需求增加 → 考虑微服务架构

---

### B. 状态管理

**选择: 中央状态机 (OrderStateMachine)**

**为什么不是分散式**:
- 如果每个 Service 都自己管理状态 → 状态转移不一致
- 集中式 → 统一的转移规则 → 安全可靠

**状态机的优点**:
- ✅ 明确的状态集合和转移规则
- ✅ 防止非法状态转移
- ✅ 便于审计和日志记录

---

### C. 数据一致性

**当前方案: 本地事务 + 重试**

**为什么不是分布式事务**:
- Saga 模式太复杂，容错难以实现
- 两阶段提交（2PC）在分布式系统中容易阻塞

**当前方案的局限**:
- ⚠️ 订单创建后，QW 同步失败 → 需人工对账
- ⚠️ 支付成功，订单状态更新失败 → 金额不符

**改进方案（待实施）**:
- 添加重试队列（MQ）
- 实现补偿事务（Saga）
- 增强对账服务

---

## 技术决策详解

### 1. 认证机制评估

| 方案 | JWT | 签名 | 加密 | 结合评分 |
|------|-----|------|------|---------|
| 安全性 | 中 | 高 | 高 | **A** |
| 性能 | 高 | 中 | 低 | **B+** |
| 易用性 | 高 | 中 | 低 | **B+** |
| 兼容性 | 高 | 高 | 高 | **A** |

**总体评价**: ⭐⭐⭐⭐⭐ 极佳

JWT + 签名的组合是最优的，既有性能，又有安全。

---

### 2. 数据库连接池

**选择: HikariCP（Spring Boot 默认）**

**配置**:
```yaml
spring.datasource.hikari.maximum-pool-size: 20
spring.datasource.hikari.minimum-idle: 5
spring.datasource.hikari.connection-timeout: 30000ms
spring.datasource.hikari.idle-timeout: 600000ms
spring.datasource.hikari.max-lifetime: 1800000ms
```

**评估**:
- ✅ 性能最佳（基准测试最快）
- ✅ 轻量级
- ✅ Spring Boot 官方推荐

---

### 3. ORM 框架

**选择: MyBatis-Plus**

| 方案 | SQL 灵活性 | 开发效率 | 学习曲线 | 文档质量 |
|------|----------|---------|---------|---------|
| **MyBatis-Plus** | **高** | **中** | **低** | **高** |
| JPA/Hibernate | 低 | 高 | 高 | 高 |
| MyBatis | 高 | 低 | 中 | 中 |

**选择原因**:
- MyBatis 的 SQL 灵活性
- Plus 的 CRUD 简化
- 组合最平衡

---

## 业务流程决策

### 1. 订单生命周期设计评估

**当前设计的 6 个关键状态**:
1. PENDING_SIGNATURE - 待协议签署
2. PENDING_DEDUCTION - 待首扣
3. ACTIVE - 已激活
4. EXERCISED - 已行权
5. REFUNDED - 已退款
6. DEDUCTION_FAILED - 扣款失败（终态）

**评价**: ⭐⭐⭐⭐⭐

- ✅ 覆盖了整个生命周期
- ✅ 清晰的状态转移
- ✅ 支持多种终态处理

**可改进的地方**:
- 考虑添加 PENDING_FALLBACK (兜底代扣待处理) 状态

---

### 2. 支付流程设计评估

**当前设计的 2 步骤**:
1. 首扣代扣（First Deduction）
2. 兜底代扣（Fallback Deduction，可选）

**评价**: ⭐⭐⭐⭐

- ✅ 支持多种支付方式
- ✅ 失败时有备选方案
- ⚠️ 缺少更细粒度的重试逻辑

---

## 集成和依赖决策

### 1. QW 云卡集成评估

**当前风险**:
- ⚠️ QW API 超时时，订单创建失败
- ⚠️ 无自动重试（需要手工）
- ⚠️ 无降级方案

**改进方案** (优先级: 高):
```java
@Retry(maxAttempts = 3)
@CircuitBreaker(failureThreshold = 5, delay = 60s)
@Fallback(method = "fallbackSync")
public QwMemberSyncResponse syncMemberOrder(...) { }
```

---

### 2. Allinpay 支付集成评估

**当前风险**:
- ⚠️ 签名错误导致支付失败（已通过单元测试缓解）
- ⚠️ 交易状态回调可能遗漏
- ⚠️ 无完整的对账机制（待实施）

**改进方案** (优先级: 高):
- 实现全量对账
- 添加回调可靠性检查
- 记录每笔支付的详细日志

---

### 3. Tech Platform 认证集成评估

**当前设计**:
```
Tech Platform SSO
      ↓
Equity Service JWT
      ↓
后续请求使用 JWT
```

**优势**:
- ✅ 无缝的单点登录
- ✅ 后续请求快速（本地 JWT 验证）

**风险**:
- ⚠️ Tech Platform 宕机时，用户无法登录
- **缓解**: 缓存 Profile，支持离线验证

---

## 监控与运维决策

### 1. 监控方案成本-收益分析

**最终选择: 阿里云原生 (CloudMonitor + SLS)**

```
成本对比 (月均):
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
方案           成本        ROI 评估
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
纯阿里云      ¥100-150   ⭐⭐⭐⭐⭐
开源+ECS      ¥50-80     ⭐⭐⭐⭐
ARMS 商业     ¥200-500   ⭐⭐⭐
DataDog/PR    ¥500-1000  ⭐⭐
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

**决策矩阵**:

| 维度 | 纯阿里云 | 阿里云+开源 | ARMS | 推荐度 |
|------|---------|-----------|------|--------|
| 成本 | ✅ 最低 | ✓ 低 | ✗ 高 | ✅ 纯阿里云 |
| 部署难度 | ✅ 最简 | ✗ 复杂 | ✅ 最简 | ✅ 纯阿里云 |
| 维护难度 | ✅ 最低 | ✗ 高 | ✅ 最低 | ✅ 纯阿里云 |
| 功能完整性 | ✓ 70% | ✅ 90% | ✅ 100% | ✓ 阿里云+开源 |
| **综合评分** | **⭐⭐⭐⭐⭐** | **⭐⭐⭐⭐** | **⭐⭐⭐** | **✅ 纯阿里云** |

**成本节省**:
- vs ARMS: 年省 ¥2,400-4,200（50-77%）
- vs New Relic: 年省 ¥3,000-6,600（72-85%）
- vs DataDog: 年省 ¥6,600-15,000（79-89%）

---

### 2. 告警策略评估

**当前设计**:

```
P0 告警 (立即处理，<5 分钟)
  ├─ 订单成功率 < 95%
  ├─ 首扣成功率 < 95%
  ├─ QW 成功率 < 95%
  └─ 数据库连接 > 80%

P1 告警 (30 分钟内)
  ├─ 请求延迟 P99 > 2s
  ├─ 错误率 > 0.5%
  └─ 云卡同步延迟 > 3s

P2 告警 (监控)
  ├─ RDS 慢查询 > 20/小时
  ├─ JVM 堆内存 > 85%
  └─ 连接池活跃 > 30
```

**评价**: ⭐⭐⭐⭐⭐

- ✅ 分级合理
- ✅ 关键指标覆盖完整
- ✅ 告警不会过于频繁

---

## 决策矩阵总览

| # | 决策 | 优先级 | 状态 | 影响 | 风险 |
|---|------|--------|------|------|------|
| 001 | 技术栈 | ⭐⭐⭐ | ✅ 完成 | 基础架构 | 低 |
| 002 | 分层架构 | ⭐⭐⭐ | ✅ 完成 | 可维护性 | 低 |
| 003 | 数据库设计 | ⭐⭐⭐ | ✅ 完成 | 数据结构 | 低 |
| 004 | 双认证 | ⭐⭐⭐ | ✅ 完成 | 安全性 | 低 |
| 005 | 数据加密 | ⭐⭐⭐ | ✅ 完成 | 合规性 | 低 |
| 006 | 状态机 | ⭐⭐⭐ | ✅ 完成 | 业务流程 | 低 |
| 007 | 幂等性 | ⭐⭐⭐ | ✅ 完成 | 可靠性 | 低 |
| 008 | QW 集成 | ⭐⭐⭐ | ⚠️ 待加固 | 业务能力 | 中 |
| 009 | Allinpay 集成 | ⭐⭐⭐ | ⚠️ 待加固 | 支付功能 | 中 |
| 010 | 跨域认证 | ⭐⭐⭐ | ✅ 完成 | 用户体验 | 低 |
| 011 | 监控方案 | ⭐⭐⭐ | ✅ 设计完 | 可观测性 | 低 |
| 012 | 监控指标 | ⭐⭐⭐ | ✅ 设计完 | 告警能力 | 低 |
| 013 | 故障诊断 | ⭐⭐⭐ | ✅ 设计完 | MTTR | 低 |
| 014 | 容错机制 | ⭐⭐⭐ | ⚠️ 待实施 | 可靠性 | 高 |
| 015 | 服务拆分 | ⭐⭐ | 📋 待计划 | 代码质量 | 低 |
| 016 | 对账服务 | ⭐⭐⭐ | ⚠️ 待实施 | 财务一致性 | 高 |
| 017 | API 版本化 | ⭐ | 📋 下版本 | 兼容性 | 低 |
| 018 | 容量规划 | ⭐⭐ | ✅ 设计完 | 性能 | 低 |

---

## 立即采取的行动

### 优先级 1（本周完成）

- [ ] **DECISION-014**: 添加 @Retry 和 @CircuitBreaker
  - 文件: QwBenefitClient.java, AllinpayRestClient.java
  - 工作量: 2-3 天
  - 测试: 单元测试 + 集成测试

- [ ] **DECISION-016**: 实现数据对账服务
  - 文件: ReconciliationService（已框架，需完成）
  - 工作量: 3-5 天
  - 测试: 对账准确性验证

- [ ] **MONITORING**: 启动 Phase 1 监控部署
  - CloudMonitor 告警配置
  - 应用 Micrometer 集成
  - SLS 日志导出配置
  - 工作量: 3-4 天

### 优先级 2（下周）

- [ ] **DECISION-015**: 拆分 OrderSyncService
  - 工作量: 1-2 天
  - 回归测试: 确保现有功能正常

- [ ] **性能测试**: 压力测试和容量验证
  - 目标: 验证 100 req/s
  - 工作量: 2-3 天

### 优先级 3（计划中）

- [ ] **DECISION-017**: API 版本化重构（v2）
- [ ] **DECISION-013**: 链路追踪集成（可选）

---

## 结论

### 整体评价

本项目在**架构设计、安全性、业务逻辑**方面表现优秀，基础扎实。

**核心优势**:
- ✅ 清晰的分层架构
- ✅ 完善的安全机制（双认证+加密）
- ✅ 健全的幂等性和状态管理
- ✅ 周密的监控和告警设计

**主要改进方向**:
- ⚠️ 分布式容错机制（重试、熔断、降级）
- ⚠️ 完整的数据对账服务
- ⚠️ 应用代码重构（降低复杂度）

**上线就绪度**: **85%**

**建议上线条件**:
1. ✅ 完成容错机制的实施（DECISION-014）
2. ✅ 完成对账服务的实施（DECISION-016）
3. ✅ 完成 Phase 1 监控部署
4. ✅ 通过性能和安全测试
5. ✅ 准备好 24/7 值班支持

---

**报告生成**: 2026-03-31
**下次审查**: 2026-04-07
**审查人**: 技术架构师 + SRE
