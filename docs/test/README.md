# 测试文档 - 快速导航

**项目**: NexusFin Equity Service
**文档日期**: 2026-03-31

---

## 📋 文档清单

### 0.0. 🧾 [20260421_H5全流程体验完成情况.md](./20260421_H5全流程体验完成情况.md)
**H5 全流程体验完成情况** - 汇总本轮页面体验、后端日志检查与 MySQL 落库核对的已完成 / 未完成事项

**适合人群**:
- 需要快速了解 `2026-04-21` 本轮 H5 体验结果的人
- 需要区分“已经完成”和“仍待确认”事项的人
- 需要查看借款主链与还款主链当前核对结论的人

**压缩总结**:
- ✅ 本地 H5 / 后端 / 科技平台 stub / 云卡 stub / MySQL 已拉起
- ✅ H5 主流程已完成体验，后端日志整体正常
- ✅ 借款主链已完成 MySQL 落库与日志一致性核对
- ⚠️ 还款主链接口已成功，但本地持久化落库闭环仍待确认

### 0. 🧭 [20260415_H5本地联调资料索引.md](./20260415_H5本地联调资料索引.md)
**H5 本地联调入口文档** - 一页看清启动清单、测试报告和推荐阅读顺序

**适合人群**:
- 需要开始 H5 本地联调的人
- 需要快速定位联调资料的人
- 需要复用本地联调口径的人

**主要内容**:
- ✅ 推荐阅读顺序
- ✅ 启动清单与测试报告的分工
- ✅ 当前本地联调四个关键前提

---

### 0.1. 🧪 [20260415_H5人工测试执行步骤与检查项.md](./20260415_H5人工测试执行步骤与检查项.md)
**H5 人工测试执行文档** - 按页面流程一步步手工走查，并同步核对接口与数据库

**适合人群**:
- 需要自己手工走一遍 H5 主流程的人
- 需要边测边看关键检查项的人
- 需要快速判断异常先查哪里的协作同学

**压缩总结**:
- ✅ 启动 H5 / Java 后端 / 科技平台 stub / 云卡 stub
- ✅ 确认后端使用 Yunka `REST`，不是默认 `MOCK`
- ✅ 检查 `HUXUAN_CARD`、登录态 Cookie、支付协议三项前置条件
- ✅ 按首页 → 审批中 → 权益详情 → 审批结果 → 还款确认 → 还款成功顺序执行
- ✅ 每一步同时检查页面、接口返回、MySQL 落库
- ✅ 遇到 `rejected` / `0.00` / `PRODUCT_NOT_FOUND` / 未登录时有明确排查方向

---

本文件夹包含项目的全量测试文档，分为三个主要部分：

### 1. 📚 [TEST_CASES_INVENTORY.md](./TEST_CASES_INVENTORY.md)
**全量测试用例清单** - 所有 25 个测试类、60+ 个测试方法的详细文档

**适合人群**:
- 想了解项目有哪些测试
- 查找特定功能的测试
- 学习测试覆盖范围

**主要内容**:
- ✅ 每个测试类的详细说明
- ✅ 每个测试方法的功能描述
- ✅ Mock 策略和测试数据
- ✅ 关键测试代码示例
- ✅ 25 个测试文件完整清单

**快速定位**:
```
按功能领域快速找到对应测试:
├─ 认证与授权测试         → [Section 认证与授权测试]
├─ 订单管理测试           → [Section 订单管理测试]
├─ 支付处理测试           → [Section 支付处理测试]
├─ 通知处理测试           → [Section 通知处理测试]
├─ 工具类测试             → [Section 工具类测试]
├─ 集成测试               → [Section 集成测试]
├─ 支撑服务测试           → [Section 支撑服务测试]
└─ 第三方集成测试         → [Section 第三方集成测试]
```

---

### 2. 🎯 [TEST_EXECUTION_MATRIX.md](./TEST_EXECUTION_MATRIX.md)
**测试执行矩阵与场景** - 不同场景下应运行哪些测试

**适合人群**:
- 开发者准备提交代码
- CI/CD 维护者
- 测试工程师

**主要内容**:
- ✅ 快速反馈循环 (5-10 分钟)
- ✅ 持续集成流程 (40 分钟)
- ✅ 发布前检查清单
- ✅ 故障排查指南
- ✅ GitHub Actions/Jenkins 配置示例
- ✅ 测试性能基准

**常见使用场景**:
```bash
# 1. 开发时快速测试
mvn test -Dgroups=fast

# 2. 提交前完整测试
mvn test -Dtest='*ServiceTest,*IntegrationTest' -DexcludedGroups=mysql-it

# 3. 发布前全量测试
export MYSQL_IT_ENABLED=true && mvn clean test jacoco:report

# 4. 故障排查 - 支付相关
mvn test -Dtest=PaymentCallbackControllerIntegrationTest,PaymentServiceTest
```

---

### 3. 💡 [TEST_BEST_PRACTICES.md](./TEST_BEST_PRACTICES.md)
**测试最佳实践与常见问题** - 如何编写高质量测试用例

**适合人群**:
- 新开发者学习项目测试风格
- 想改进测试质量的团队成员
- Code Review 时的参考指南

**主要内容**:
- ✅ 命名约定和规范
- ✅ 测试结构 (AAA 模式)
- ✅ Mock 策略指导
- ✅ 常见问题解答 (Q&A)
- ✅ 反面模式识别
- ✅ 性能优化建议

**常见问题快速定位**:
```
Q1. 如何测试加密字段?
Q2. 如何测试幂等性?
Q3. 如何测试状态转换?
Q4. 如何测试签名验证?
Q5. 如何测试异常情况?
```

---

## 🚀 快速开始

### 我是新开发者，想了解项目测试
1. 先看 [TEST_BEST_PRACTICES.md](./TEST_BEST_PRACTICES.md) - 了解命名约定和结构
2. 再看 [TEST_CASES_INVENTORY.md](./TEST_CASES_INVENTORY.md) - 了解哪些功能有测试
3. 根据你要修改的功能，找到对应的测试文件并学习

### 我要提交代码到 main 分支
1. 查看 [TEST_EXECUTION_MATRIX.md](./TEST_EXECUTION_MATRIX.md) - [发布前检查](#) 部分
2. 运行推荐的完整测试套件
3. 确保覆盖率 >= 80%

### CI/CD 维护者要配置自动化测试
1. 查看 [TEST_EXECUTION_MATRIX.md](./TEST_EXECUTION_MATRIX.md) - [持续集成流程](#) 部分
2. 参考 GitHub Actions / Jenkins 配置示例
3. 配置 4-stage 流程: Build → Unit → Integration → Coverage

### 我要调试测试失败
1. 查看 [TEST_EXECUTION_MATRIX.md](./TEST_EXECUTION_MATRIX.md) - [故障排查](#) 部分
2. 按故障类型找对应的测试命令
3. 参考 [TEST_CASES_INVENTORY.md](./TEST_CASES_INVENTORY.md) 了解具体测试逻辑

---

## 📊 测试统计

| 指标 | 数值 |
|------|------|
| 测试类总数 | 25 |
| 测试方法总数 | 60+ |
| 代码覆盖率目标 | >= 80% |
| 单元测试比例 | 60% |
| 集成测试比例 | 30% |
| 端到端测试比例 | 10% |
| 完整测试套件耗时 | 10-15 分钟 |
| 快速测试耗时 | 3-5 分钟 |

---

## 🗂️ 文件夹结构

```
docs/test/
├── README.md                      (本文件 - 导航与索引)
├── TEST_CASES_INVENTORY.md        (测试用例全量清单)
├── TEST_EXECUTION_MATRIX.md       (执行矩阵与场景)
└── TEST_BEST_PRACTICES.md         (最佳实践与常见问题)

src/test/java/
└── com/nexusfin/equity/
    ├── config/                    (配置类测试)
    │   ├── JwtAuthenticationFilterTest.java
    │   └── SignatureServiceTest.java
    ├── controller/                (控制器集成测试)
    │   ├── AuthControllerIntegrationTest.java
    │   ├── BenefitOrderControllerIntegrationTest.java
    │   ├── PaymentCallbackControllerIntegrationTest.java
    │   ├── NotificationCallbackControllerIntegrationTest.java
    │   ├── MySqlCallbackFlowIntegrationTest.java
    │   └── MySqlRoundTripIntegrationTest.java
    ├── service/                   (服务单元测试)
    │   ├── BenefitOrderServiceTest.java
    │   ├── BenefitOrderServiceAuthTest.java
    │   ├── PaymentServiceTest.java
    │   ├── NotificationServiceTest.java
    │   ├── AuthServiceTest.java
    │   ├── AgreementServiceTest.java
    │   ├── IdempotencyServiceTest.java
    │   ├── ReconciliationServiceTest.java
    │   ├── DownstreamSyncServiceTest.java
    │   ├── FallbackDeductServiceTest.java
    │   └── impl/
    │       └── TechPlatformUserClientImplTest.java
    ├── util/                      (工具类测试)
    │   ├── JwtUtilTest.java
    │   ├── CookieUtilTest.java
    │   ├── SensitiveDataCipherTest.java
    │   └── OrderStateMachineTest.java
    ├── thirdparty/                (第三方集成测试)
    │   └── techplatform/
    │       └── TechPlatformClientTest.java
    └── NexusfinEquityApplicationTests.java  (应用启动测试)
```

---

## 🔑 核心测试类速查

### 认证与授权 (3 个测试类)
```
JwtAuthenticationFilterTest        JWT 过滤器
AuthControllerIntegrationTest      SSO 端点集成
AuthServiceTest                    成员预配与认证
```

### 订单管理 (4 个测试类)
```
BenefitOrderControllerIntegrationTest   订单 HTTP 端点
BenefitOrderServiceTest                 订单创建逻辑
BenefitOrderServiceAuthTest             认证相关错误
OrderStateMachineTest                   状态转换机
```

### 支付处理 (3 个测试类)
```
PaymentCallbackControllerIntegrationTest   支付回调端点
PaymentServiceTest                         支付逻辑
FallbackDeductServiceTest                  兜底代扣
```

### 通知处理 (2 个测试类)
```
NotificationCallbackControllerIntegrationTest   通知端点
NotificationServiceTest                         通知逻辑
```

---

## 🎓 学习路径

### 初级 (新开发者)
```
1. 阅读 TEST_BEST_PRACTICES.md - 命名约定部分
   └─ 了解如何命名测试类和方法

2. 查看 TEST_CASES_INVENTORY.md - 工具类测试部分
   └─ 学习简单的单元测试结构

3. 运行快速测试观察行为
   mvn test -Dgroups=fast
```

### 中级 (有基础的开发者)
```
1. 阅读 TEST_BEST_PRACTICES.md - Mock 策略部分
   └─ 了解如何正确 Mock 依赖

2. 查看 TEST_CASES_INVENTORY.md - 服务单元测试部分
   └─ 学习业务逻辑的测试方法

3. 修改一个现有测试，提交代码
   参考 TEST_EXECUTION_MATRIX.md - 功能测试后部分
```

### 高级 (架构师/TL)
```
1. 阅读 TEST_BEST_PRACTICES.md - 反面模式部分
   └─ Code Review 时识别坏的测试

2. 查看 TEST_CASES_INVENTORY.md - 集成测试部分
   └─ 理解端到端测试的价值

3. 配置 CI/CD 流程
   参考 TEST_EXECUTION_MATRIX.md - 持续集成流程部分
```

---

## 🛠️ 常用命令速查

### 开发时 (快速反馈)
```bash
# 运行工具类测试 (最快)
mvn test -Dtest='*Util*'

# 运行服务单元测试
mvn test -Dtest='*Service*' -DexcludedGroups=mysql-it

# 运行特定测试类
mvn test -Dtest=BenefitOrderServiceTest

# 运行特定测试方法
mvn test -Dtest=BenefitOrderServiceTest#shouldCreateBenefitOrderAndPersistAgreementArtifacts
```

### 提交前 (完整验证)
```bash
# 运行所有单元和集成测试
mvn test -DexcludedGroups=mysql-it

# 完整验证 (包括 MySQL)
export MYSQL_IT_ENABLED=true
mvn clean test jacoco:report
```

### CI/CD (自动化)
```bash
# Stage 1: 快速检查
mvn clean compile && mvn test -Dgroups=fast

# Stage 2: 完整单元测试
mvn test -DexcludedGroups=mysql-it,integration

# Stage 3: 集成测试
mvn test -Dgroups=integration

# Stage 4: 覆盖率报告
mvn jacoco:report
```

---

## 📞 常见问题

### Q: 如何快速验证我的改动不破坏现有功能?
A:
```bash
# 运行相关的单元测试 (5 分钟)
mvn test -Dtest='*BenefitOrderService*,*PaymentService*'

# 或运行相关的集成测试 (10 分钟)
mvn test -Dtest='*BenefitOrderController*,*PaymentCallback*'
```

### Q: MySQL 集成测试为什么没有运行?
A: 需要启用环境变量
```bash
export MYSQL_IT_ENABLED=true
mvn test -Dgroups=mysql-it
```

### Q: 如何查看代码覆盖率报告?
A:
```bash
mvn clean test jacoco:report
# 打开报告: target/site/jacoco/index.html
```

### Q: 我写的新功能，应该补充什么测试?
A: 参考 [TEST_BEST_PRACTICES.md](./TEST_BEST_PRACTICES.md) 的反面模式部分，确保:
- [ ] 单元测试覆盖核心业务逻辑
- [ ] 集成测试覆盖 HTTP 端点
- [ ] 幂等性测试覆盖重复请求场景
- [ ] 错误测试覆盖异常情况

---

## 📚 相关文件

- **项目工程审查**: `docs/ENG_REVIEW_20260401.md`
- **构建配置**: `pom.xml`
- **测试配置**: `src/test/resources/application.yml`, `src/test/resources/application-mysql-it.yml`
- **测试数据**: `src/test/resources/db/test-data.sql`

---

## 📌 重要提示

1. **所有测试必须独立运行**: 不依赖其他测试的执行顺序
2. **数据隔离很重要**: 每个测试应在 @BeforeEach 中清理数据
3. **Mock 外部依赖，真实内部**: 数据库和核心逻辑应该是真实的
4. **编写清晰的断言**: 让失败的测试能立即指出问题所在
5. **维护测试文档**: 复杂的业务逻辑需要清晰的测试注释

---

**文档版本**: 1.0
**最后更新**: 2026-03-31
**维护者**: 工程师团队

如有问题或改进建议，请更新相应的文档文件。
