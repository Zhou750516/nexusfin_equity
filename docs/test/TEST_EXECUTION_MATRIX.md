# 测试执行矩阵与场景

**文档日期**: 2026-03-31
**目的**: 指导不同场景下应运行的测试集合

---

## 目录

1. [快速反馈循环](#快速反馈循环)
2. [持续集成流程](#持续集成流程)
3. [发布前检查](#发布前检查)
4. [故障排查](#故障排查)
5. [测试标记与分类](#测试标记与分类)

---

## 快速反馈循环

### 开发者本地测试 (5-10 分钟)

**目标**: 开发功能时快速验证

#### 最小化测试集合
```bash
# 仅运行 "fast" 标记的测试
mvn test -Dgroups=fast

# 或指定包
mvn test -Dtest=com.nexusfin.equity.util.*
mvn test -Dtest=com.nexusfin.equity.service.*
```

#### 核心测试类 (快速路径)
```
✓ OrderStateMachineTest          (~50ms)
✓ JwtUtilTest                    (~30ms)
✓ SignatureServiceTest           (~20ms)
✓ CookieUtilTest                 (~20ms)
✓ SensitiveDataCipherTest        (~50ms)
✓ IdempotencyServiceTest         (~40ms)
```

**预期时间**: ~3-5 分钟

#### 示例命令
```bash
# 仅运行工具类测试
mvn test -Dtest='*Util*,*ServiceTest'

# 排除 MySQL 集成测试
mvn test -DexcludedGroups=mysql-it

# 运行单个测试方法
mvn test -Dtest=OrderStateMachineTest#shouldApplyFirstDeductSuccessState
```

---

### 功能测试后 (15-20 分钟)

**目标**: 验证新功能不破坏现有逻辑

#### 测试覆盖
```
✓ 所有 service 单元测试
✓ 相关 controller 集成测试
✓ 业务逻辑 E2E 测试
```

#### 关键路径
```bash
# 1. 认证流程
mvn test -Dtest=AuthControllerIntegrationTest,AuthServiceTest

# 2. 订单创建流程
mvn test -Dtest=BenefitOrderControllerIntegrationTest,BenefitOrderServiceTest

# 3. 支付回调流程
mvn test -Dtest=PaymentCallbackControllerIntegrationTest,PaymentServiceTest

# 4. 整体命令
mvn test -Dtest='*IntegrationTest,*ServiceTest' -DexcludedGroups=mysql-it
```

**预期时间**: 15-20 分钟

---

## 持续集成流程

### CI 流程 (Maven Pipeline)

#### Stage 1: 快速验证 (5 分钟)
```yaml
# GitHub Actions / GitLab CI / Jenkins
- name: Fast Unit Tests
  run: mvn test -Dgroups=fast -DfailIfNoTests=false
```

#### Stage 2: 完整单元测试 (10 分钟)
```yaml
- name: All Unit Tests
  run: mvn test -DexcludedGroups=mysql-it,integration
```

#### Stage 3: 集成测试 (15 分钟)
```yaml
- name: Integration Tests
  run: mvn test -Dgroups=integration,mysql-it
```

#### Stage 4: 代码覆盖率 (10 分钟)
```yaml
- name: Coverage Report
  run: mvn clean test jacoco:report
  - name: Upload to Codecov
    run: bash <(curl -s https://codecov.io/bash)
```

#### 总耗时: ~40 分钟

### CI 矩阵

| Stage | 命令 | 时间 | 失败时 |
|-------|------|------|--------|
| Build | `mvn clean compile` | 3m | 中断 |
| Unit Tests | `mvn test` (无 MySQL) | 10m | 中断 |
| Integration | `mvn test -Dgroups=integration` | 15m | 标记失败 |
| MySQL IT | `MYSQL_IT_ENABLED=true mvn test` | 10m | 可选 (通知) |
| Coverage | `mvn jacoco:report` | 5m | 标记 |

---

## 发布前检查

### Pre-Release Checklist (30 分钟)

```bash
# 1. 清理构建
mvn clean

# 2. 完整测试 (所有，包括 MySQL)
export MYSQL_IT_ENABLED=true
mvn test

# 3. 代码质量检查
mvn clean test jacoco:report
mvn checkstyle:check

# 4. 构建发布包
mvn clean package -DskipTests

# 5. 验证覆盖率
coverage_percent=$(grep -oP 'Covered instructions: \K[0-9]+' target/site/jacoco/index.html)
if [ $coverage_percent -lt 80 ]; then
    echo "FAIL: Coverage ${coverage_percent}% < 80%"
    exit 1
fi

# 6. 清理检查
git status --porcelain  # 确保无脏数据
```

### 发布时测试矩阵

| 类别 | 测试集 | 必须通过 | 时间 |
|------|--------|---------|------|
| **单元测试** | 所有 service/util | ✅ 是 | 10m |
| **集成测试** | 所有 controller | ✅ 是 | 15m |
| **端到端** | MySQL 流程 | ✅ 是 | 10m |
| **覆盖率** | >80% | ✅ 是 | 5m |

---

## 故障排查

### 场景 1: 订单创建失败

```bash
# 1. 运行订单创建相关测试
mvn test -Dtest=BenefitOrderServiceTest,BenefitOrderControllerIntegrationTest

# 2. 检查幂等性
mvn test -Dtest='*IdempotencyServiceTest,*ReplayDuplicate*'

# 3. 检查协议生成
mvn test -Dtest=AgreementServiceTest

# 4. 检查状态机
mvn test -Dtest=OrderStateMachineTest

# 5. 完整流程测试
mvn test -Dtest=MySqlCallbackFlowIntegrationTest
```

### 场景 2: 支付回调异常

```bash
# 1. 支付回调处理
mvn test -Dtest=PaymentCallbackControllerIntegrationTest,PaymentServiceTest

# 2. 签名验证
mvn test -Dtest=SignatureServiceTest

# 3. 幂等性处理
mvn test -Dtest=IdempotencyServiceTest

# 4. 端到端流程
mvn test -Dtest=MySqlCallbackFlowIntegrationTest
```

### 场景 3: 认证异常

```bash
# 1. JWT 处理
mvn test -Dtest=JwtUtilTest,JwtAuthenticationFilterTest

# 2. Cookie 处理
mvn test -Dtest=CookieUtilTest

# 3. 认证服务
mvn test -Dtest=AuthServiceTest,AuthControllerIntegrationTest

# 4. 签名验证
mvn test -Dtest=SignatureServiceTest
```

### 场景 4: 敏感数据泄露

```bash
# 1. 加密验证
mvn test -Dtest=SensitiveDataCipherTest

# 2. 成员数据加密
mvn test -Dtest=AuthServiceTest

# 3. 数据库验证
mvn test -Dtest=MySqlRoundTripIntegrationTest

# 4. 所有加密相关
mvn test -Dtest='*Cipher,*Encryption,*Crypto'
```

---

## 测试标记与分类

### Maven Surefire 标记示例

#### 按类型分类
```java
@Tag("fast")                    // 快速测试 (<100ms)
@Tag("slow")                    // 慢速测试
@Tag("unit")                    // 单元测试
@Tag("integration")             // 集成测试
@Tag("mysql-it")                // MySQL 依赖
@Tag("encryption")              // 加密相关
@Tag("payment")                 // 支付流程
@Tag("idempotency")             // 幂等性
```

#### 执行示例
```bash
# 仅快速测试
mvn test -Dgroups=fast

# 快速 + 单元
mvn test -Dgroups="fast,unit"

# 排除慢速 + MySQL
mvn test -DexcludedGroups="slow,mysql-it"

# 仅支付
mvn test -Dgroups=payment
```

---

## 测试性能基准

### 各测试类运行时间

| 测试类 | 标记 | 时间 | 类型 |
|--------|------|------|------|
| `SignatureServiceTest` | fast, unit | 20ms | 工具 |
| `JwtUtilTest` | fast, unit | 30ms | 工具 |
| `CookieUtilTest` | fast, unit | 20ms | 工具 |
| `OrderStateMachineTest` | fast, unit | 50ms | 业务逻辑 |
| `IdempotencyServiceTest` | fast, unit | 40ms | 业务逻辑 |
| `BenefitOrderServiceTest` | unit | 100ms | 业务逻辑 |
| `PaymentServiceTest` | unit | 80ms | 业务逻辑 |
| `AgreementServiceTest` | unit | 60ms | 业务逻辑 |
| `AuthServiceTest` | unit | 120ms | 业务逻辑 |
| `JwtAuthenticationFilterTest` | unit, integration | 200ms | 过滤器 |
| `AuthControllerIntegrationTest` | integration | 500ms | 控制器 |
| `BenefitOrderControllerIntegrationTest` | integration | 800ms | 控制器 |
| `PaymentCallbackControllerIntegrationTest` | integration | 600ms | 控制器 |
| `NotificationCallbackControllerIntegrationTest` | integration | 700ms | 控制器 |
| `MySqlRoundTripIntegrationTest` | mysql-it | 1500ms | E2E |
| `MySqlCallbackFlowIntegrationTest` | mysql-it | 2000ms | E2E |
| `TechPlatformClientTest` | unit, external | 300ms | 集成 |
| `TechPlatformUserClientImplTest` | unit, retry | 200ms | 集成 |

**总计**: ~10-15 分钟 (完整套件)

---

## 自动化测试配置建议

### GitHub Actions 工作流示例

```yaml
# .github/workflows/test.yml
name: Test Suite

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    strategy:
      matrix:
        java-version: [17]

    steps:
      - uses: actions/checkout@v3

      - name: Set up Java
        uses: actions/setup-java@v3
        with:
          java-version: ${{ matrix.java-version }}
          distribution: 'temurin'
          cache: maven

      - name: Run tests
        run: mvn clean test jacoco:report

      - name: Upload coverage
        uses: codecov/codecov-action@v3
        with:
          files: ./target/site/jacoco/jacoco.xml
```

### Jenkins Pipeline 示例

```groovy
pipeline {
    agent any
    stages {
        stage('Build') {
            steps {
                sh 'mvn clean compile'
            }
        }
        stage('Unit Tests') {
            steps {
                sh 'mvn test -DexcludedGroups=mysql-it'
            }
        }
        stage('Integration Tests') {
            steps {
                sh 'mvn test -Dgroups=integration'
            }
        }
        stage('Coverage') {
            steps {
                sh 'mvn jacoco:report'
            }
        }
    }
    post {
        always {
            junit 'target/surefire-reports/*.xml'
            jacoco execPattern: 'target/jacoco.exec'
        }
    }
}
```

---

## 测试数据清理策略

### H2 数据库 (本地/CI)

**自动清理**: 每个 @BeforeEach 中

```java
@BeforeEach
void setUp() {
    // 清理前置数据
    contractArchiveRepo.delete(new QueryWrapper<ContractArchive>());
    signTaskRepo.delete(new QueryWrapper<SignTask>());
    // ... 其他表
}
```

### MySQL 数据库 (条件化)

**隔离策略**: 使用唯一前缀

```java
String testId = "test_" + System.currentTimeMillis() + "_" + random();

// 创建成员
String memberId = "mem_" + testId;

// 创建订单
String benefitOrderNo = "ord_" + testId;

// 检索
MemberInfo member = memberRepo.selectById(memberId);

// 清理 (可选，@TruncateMode)
```

---

## 监控与告警

### 测试覆盖率目标

```
目标: 80% 代码覆盖率

分类:
├─ Critical paths: 95%+    (订单、支付、认证)
├─ Core services: 85%+     (业务逻辑)
├─ Utils: 80%+              (工具类)
└─ Controllers: 80%+        (端点)
```

### 测试失败告警

| 失败类型 | 严重性 | 响应 |
|---------|--------|------|
| 单元测试失败 | 🔴 Critical | 立即中断 CI |
| 集成测试失败 | 🟠 High | 通知 PR 作者 |
| MySQL IT 失败 | 🟡 Medium | 可选通知 |
| 覆盖率下降 | 🟡 Medium | 标记 PR |

---

## 版本化测试计划

### V1.1.0 (当前)
- ✅ 25 个测试类
- ✅ 60+ 个测试方法
- ✅ ~80% 覆盖率

### V1.2.0 (计划)
- 📝 添加 @Retry 重试测试
- 📝 添加熔断器测试
- 📝 添加并发订单测试
- 📝 添加压力测试 (jmeter)

### V2.0.0 (Future)
- 📝 异步架构测试
- 📝 事件驱动测试
- 📝 性能基准测试

---

**文档维护**: 每个发布周期更新一次
**最后更新**: 2026-03-31
