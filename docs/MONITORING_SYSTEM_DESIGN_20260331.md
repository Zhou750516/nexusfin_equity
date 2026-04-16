# NexusFin Equity 监控系统完整设计方案

**文档日期**: 2026-03-31
**版本**: 1.0
**状态**: 需求评审
**部署环境**: 阿里云（ECS + RDS + OSS）
**目标**: 简单、成本低、部署方便、易于维护

---

## 文档导航

1. [执行摘要](#执行摘要)
2. [项目现状分析](#项目现状分析)
3. [系统层面监控](#系统层面监控设计)
4. [业务层面监控](#业务层面监控设计)
5. [推荐方案](#推荐监控方案)
6. [实施路线图](#实施路线图)
7. [成本估算](#成本估算)

---

## 执行摘要

### 监控目标

本项目是一个**金融权益分发服务**，涉及复杂的业务流程和多个外部服务集成，对**可靠性、可观测性、合规性**有极高的要求。

### 推荐方案

**阿里云原生 + 开源补充** 的分阶段方案：

```
┌─────────────────────────────────────────────────┐
│  第一阶段（1-2周）：基础监控 + 应用指标         │
│  └─ CloudMonitor + SLS + Micrometer             │
├─────────────────────────────────────────────────┤
│  第二阶段（3-4周）：完整业务监控 + 告警        │
│  └─ 业务指标埋点 + 告警规则配置                │
├─────────────────────────────────────────────────┤
│  第三阶段（5-8周）：高级能力（可选）          │
│  └─ 链路追踪 + 自适应告警 + 成本优化           │
└─────────────────────────────────────────────────┘
```

### 成本估算

```
月均成本：  ¥80-150 RMB
年均成本：  ¥960-1,800 RMB
vs 商业方案: 节省 70%+ 成本
```

---

## 项目现状分析

### 1. 核心业务流程

```
用户注册/登录
    ↓
权益订单创建 → 协议生成 → 云卡同步(QW)
    ↓
首扣代扣(Allinpay)
    ↓
支付回调处理
    ↓
权益行权 → 通知消费 → 对账检查
```

### 2. 关键外部服务

| 服务 | 用途 | 风险等级 | 监控优先级 |
|------|------|---------|----------|
| QW (云卡) | 会员同步、权益行权 | 高 | P0 |
| Allinpay (通联) | 代扣支付、直连集成 | 高 | P0 |
| Tech Platform | 用户认证、信息查询 | 中 | P1 |
| MySQL RDS | 数据持久化 | 高 | P0 |

### 3. 现有监控能力

✅ **已有**:
- ECS/RDS 基础设施监控（CloudMonitor 自动）
- TraceId 和 RequestId 传播机制
- 异常捕获和日志记录

❌ **缺失**:
- 应用性能指标（APM）
- 业务关键指标监控
- 自动告警机制
- 日志聚合分析
- 链路追踪

---

## 系统层面监控设计

### 1. 基础设施监控

#### 1.1 ECS 计算资源监控

| 指标 | 目标值 | 告警阈值 | 优先级 | 意义 |
|------|--------|---------|--------|------|
| CPU 使用率 | <70% | >80% | P1 | CPU 资源不足导致请求延迟 |
| 内存使用率 | <75% | >85% | P1 | 内存溢出导致 OOM 和服务挂起 |
| 磁盘使用率 | <80% | >90% | P1 | 磁盘满导致日志写入失败 |
| 网络入流量 | 无限 | >500Mbps | P2 | DDoS 或异常流量 |
| 网络出流量 | 无限 | >500Mbps | P2 | 大量数据外传 |
| 网络丢包率 | <0.01% | >0.1% | P2 | 网络不稳定 |

**监控来源**: CloudMonitor（自动）

**告警渠道**: 钉钉群 + 邮件

---

#### 1.2 RDS 数据库监控

| 指标 | 目标值 | 告警阈值 | 优先级 | 意义 |
|------|--------|---------|--------|------|
| CPU 使用率 | <60% | >75% | P1 | 数据库查询瓶颈 |
| 连接数 | <50% | >80% | P1 | 连接池耗尽导致新请求被拒 |
| 慢查询（>1s） | <10/小时 | >20/小时 | P2 | 数据库 SQL 性能问题 |
| 磁盘空间 | <80% | >90% | P1 | 数据库空间不足 |
| 主从延迟 | <1s | >5s | P1 | 主从不同步，读写一致性问题 |
| 事务回滚率 | <0.1% | >0.5% | P2 | 业务异常导致事务失败 |
| QPS (查询数) | 根据容量 | 按容量80% | P2 | 数据库吞吐量瓶颈 |

**监控来源**:
- CloudMonitor 自动收集（CPU、连接数、磁盘）
- RDS 慢查询日志（需配置）
- 自定义查询（通过应用端查询元数据）

**告警渠道**: 钉钉 + 邮件

**示例告警规则**:
```
IF db.connections > capacity * 0.8 FOR 5 minutes
THEN send("钉钉", "数据库连接池使用率 > 80%")
```

---

### 2. 应用层监控

#### 2.1 JVM 运行时指标

| 指标 | 目标值 | 告警阈值 | 优先级 | 意义 |
|------|--------|---------|--------|------|
| 堆内存使用率 | <80% | >90% | P1 | 内存泄漏或并发高峰 |
| GC 暂停时间 P95 | <100ms | >500ms | P2 | GC 导致的响应延迟 |
| 线程数 | <500 | >800 | P2 | 线程池泄漏或任务堆积 |
| 类加载数 | 稳定 | 持续增加 | P2 | 可能的 ClassLoader 泄漏 |

**采集方式**: Micrometer + Prometheus Registry 推送到 CloudMonitor

**示例配置**:
```yaml
management:
  metrics:
    tags:
      application: nexusfin-equity
      environment: prod
    export:
      prometheus:
        enabled: true
```

---

#### 2.2 Web 应用监控

| 指标 | 目标值 | 告警阈值 | 优先级 | 意义 |
|------|--------|---------|--------|------|
| 请求吞吐量（QPS） | >100 req/s | <50 req/s | P2 | 服务可用性下降 |
| 请求延迟 P50 | <100ms | - | P1 | 用户体验 |
| 请求延迟 P95 | <500ms | >1s | P1 | 大部分用户的体验 |
| 请求延迟 P99 | <1s | >2s | P1 | 最差用户的体验 |
| 错误率 (5xx) | <0.1% | >0.5% | P1 | 服务故障 |
| 4xx 错误率 | <1% | >5% | P2 | 客户端错误（可能的攻击） |
| 请求拒绝率 | 0% | >1% | P1 | 服务过载 |

**采集方式**: Spring Boot Actuator + Micrometer

**示例埋点**:
```java
@PostMapping("/orders")
public CreateBenefitOrderResponse createOrder(...) {
    Timer.Sample sample = Timer.start(meterRegistry);
    try {
        CreateBenefitOrderResponse response = service.createOrder(...);
        meterRegistry.counter("http.requests",
            "endpoint", "/orders",
            "status", "2xx"
        ).increment();
        return response;
    } catch (Exception e) {
        meterRegistry.counter("http.requests",
            "endpoint", "/orders",
            "status", String.valueOf(e.getStatus())
        ).increment();
        throw e;
    } finally {
        sample.stop(Timer.builder("http.request.duration")
            .tag("endpoint", "/orders")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(meterRegistry));
    }
}
```

---

#### 2.3 数据库连接池监控

| 指标 | 目标值 | 告警阈值 | 优先级 | 意义 |
|------|--------|---------|--------|------|
| 活跃连接数 | <20 | >30 | P2 | 数据库连接不足 |
| 空闲连接数 | >10 | <5 | P2 | 连接泄漏 |
| 等待连接平均时间 | <10ms | >100ms | P2 | 连接获取困难 |
| 连接获取失败数 | 0 | >0 | P1 | 连接池耗尽 |

**采集方式**: HikariCP 自带指标 + Micrometer

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

---

### 3. 网络和依赖监控

#### 3.1 外部服务连接性

| 服务 | 监控项 | 目标值 | 告警阈值 | 优先级 |
|------|--------|--------|---------|--------|
| QW API | 可达性 | 100% | <99% | P0 |
| Allinpay API | 可达性 | 100% | <99% | P0 |
| Tech Platform | 可达性 | 100% | <99% | P1 |
| DNS | 解析时间 | <50ms | >200ms | P2 |

**实现方式**: 应用端定期 HTTP HEAD 请求

```java
@Scheduled(fixedDelay = 30000)
public void checkExternalServiceHealth() {
    checkService("qw", "https://qw-api.example.com/health");
    checkService("allinpay", "https://allinpay.example.com/health");
    checkService("tech-platform", "https://tech.example.com/health");
}

private void checkService(String name, String url) {
    try {
        long start = System.currentTimeMillis();
        int status = httpClient.head(url).getStatusCode();
        long duration = System.currentTimeMillis() - start;

        meterRegistry.counter("external.service.health",
            "service", name,
            "status", String.valueOf(status)
        ).increment();

        meterRegistry.timer("external.service.latency",
            "service", name
        ).record(duration, TimeUnit.MILLISECONDS);
    } catch (Exception e) {
        meterRegistry.counter("external.service.health",
            "service", name,
            "status", "error"
        ).increment();
    }
}
```

---

## 业务层面监控设计

### 1. 认证与授权监控

| 监控项 | 目标值 | 告警阈值 | 数据源 | 意义 |
|--------|--------|---------|--------|------|
| 登录成功率 | >99% | <95% | 应用日志 | 认证系统故障 |
| 登录请求数/分钟 | 根据业务 | 异常增加 | 应用日志 | 暴力破解或营销活动 |
| JWT 过期导致的重新认证 | 监控 | 正常水平 | 应用日志 | 了解用户会话管理 |
| Tech Platform 认证失败率 | <1% | >5% | 应用日志 | 下游服务故障 |
| 无效 Token 请求 | <1% | >10% | 应用日志 | 可能的安全问题 |

**埋点位置**: `AuthServiceImpl`, `AuthController`

```java
@PostMapping("/login")
public AuthResponse login(@RequestBody LoginRequest request) {
    try {
        AuthResponse response = authService.login(...);
        meterRegistry.counter("auth.login.attempts",
            "status", "success"
        ).increment();
        meterRegistry.counter("auth.login.success").increment();
        return response;
    } catch (InvalidCredentialsException e) {
        meterRegistry.counter("auth.login.attempts",
            "status", "invalid_credentials"
        ).increment();
        throw e;
    } catch (UserNotFoundException e) {
        meterRegistry.counter("auth.login.attempts",
            "status", "user_not_found"
        ).increment();
        throw e;
    } catch (Exception e) {
        meterRegistry.counter("auth.login.attempts",
            "status", "error"
        ).increment();
        throw e;
    }
}
```

---

### 2. 权益订单核心业务监控

#### 2.1 订单创建流程

| 监控项 | 目标值 | 告警阈值 | 优先级 | 意义 |
|--------|--------|---------|--------|------|
| 订单创建成功率 | >99% | <95% | P0 | 核心业务可用性 |
| 订单创建平均延迟 | <500ms | >1000ms | P1 | 用户体验 |
| 订单创建失败原因分布 | 监控 | 异常增加 | P2 | 故障诊断 |
| 重复订单（幂等）检测 | 监控 | 正常水平 | P2 | 了解用户重试模式 |
| 云卡同步成功率 | >99% | <95% | P0 | 下游系统一致性 |
| 云卡同步失败重试次数 | 监控 | 过高表示问题 | P2 | 系统可靠性 |

**埋点位置**: `BenefitOrderServiceImpl`, `BenefitOrderController`

```java
@PostMapping("/orders")
public CreateBenefitOrderResponse createOrder(
    @RequestBody CreateBenefitOrderRequest request) {

    Timer.Sample sample = Timer.start(meterRegistry);
    try {
        // 记录订单创建尝试
        meterRegistry.counter("business.order.create.attempts").increment();

        CreateBenefitOrderResponse response = benefitOrderService.createOrder(
            request.getMemberId(),
            request.getProductId(),
            ...
        );

        // 成功指标
        meterRegistry.counter("business.order.created",
            "status", "success",
            "product_id", request.getProductId()
        ).increment();

        // 云卡同步指标记录在 DownstreamSyncService
        return response;
    } catch (ProductNotFoundException e) {
        meterRegistry.counter("business.order.created",
            "status", "failed",
            "reason", "product_not_found"
        ).increment();
        throw e;
    } catch (InsufficientQuotaException e) {
        meterRegistry.counter("business.order.created",
            "status", "failed",
            "reason", "insufficient_quota"
        ).increment();
        throw e;
    } catch (Exception e) {
        meterRegistry.counter("business.order.created",
            "status", "failed",
            "reason", "unknown_error"
        ).increment();
        throw e;
    } finally {
        sample.stop(Timer.builder("business.order.create.duration")
            .description("订单创建耗时")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(meterRegistry));
    }
}
```

#### 2.2 订单状态转移监控

| 监控项 | 目标值 | 告警阈值 | 优先级 | 意义 |
|--------|--------|---------|--------|------|
| 订单待首扣状态停留时间 | <24小时 | >48小时 | P1 | 支付流程卡顿 |
| 订单已首扣→待行权 | 监控 | - | P2 | 流程正常推进 |
| 订单行权成功率 | >98% | <95% | P0 | 核心功能可用性 |
| 订单退款成功率 | >98% | <95% | P0 | 客户服务能力 |

**实现方式**: 在 `OrderStateMachine` 和状态转移时记录

```java
public class OrderStateMachine {
    public OrderState transition(OrderState current, OrderStateTransition event) {
        OrderState next = computeNextState(current, event);

        // 记录状态转移指标
        meterRegistry.counter("business.order.state_transition",
            "from", current.name(),
            "to", next.name(),
            "event", event.name()
        ).increment();

        return next;
    }
}
```

---

### 3. 支付核心流程监控

#### 3.1 首扣代扣监控

| 监控项 | 目标值 | 告警阈值 | 优先级 | 意义 |
|--------|--------|---------|--------|------|
| 首扣代扣成功率 | >99% | <95% | **P0** | **最关键的业务指标** |
| 首扣代扣平均延迟 | <2s | >5s | P1 | Allinpay 服务性能 |
| 首扣代扣失败原因分布 | 监控 | - | P2 | 故障诊断（余额不足、卡被冻结等） |
| Allinpay 服务调用成功率 | >99% | <95% | P0 | 支付网关可用性 |
| Allinpay 调用延迟 P99 | <2s | >5s | P1 | 支付延迟 |

**埋点位置**: `PaymentServiceImpl`, Allinpay 调用处

```java
public PaymentStatusResponse handleFirstDeductCallback(
    DeductionCallbackRequest request) {

    Timer.Sample sample = Timer.start(meterRegistry);

    try {
        // 模拟调用 Allinpay
        long startTime = System.currentTimeMillis();
        PaymentResult result = allinpayClient.deduct(...);
        long duration = System.currentTimeMillis() - startTime;

        // 记录 Allinpay 调用指标
        meterRegistry.timer("external.allinpay.deduction.duration")
            .record(duration, TimeUnit.MILLISECONDS);
        meterRegistry.counter("external.allinpay.deduction",
            "status", result.isSuccess() ? "success" : "failed",
            "error_code", result.getErrorCode()
        ).increment();

        // 业务指标
        meterRegistry.counter("business.payment.first_deduct",
            "status", "success"
        ).increment();

        return new PaymentStatusResponse("SUCCESS");
    } catch (AllinpayException e) {
        // Allinpay 服务异常
        meterRegistry.counter("external.allinpay.deduction",
            "status", "failed",
            "error_type", e.getErrorType()
        ).increment();

        meterRegistry.counter("business.payment.first_deduct",
            "status", "failed",
            "reason", "allinpay_error"
        ).increment();

        throw e;
    } catch (Exception e) {
        meterRegistry.counter("business.payment.first_deduct",
            "status", "failed",
            "reason", "unknown_error"
        ).increment();
        throw e;
    } finally {
        sample.stop(Timer.builder("business.payment.callback.duration")
            .tag("type", "first_deduct")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(meterRegistry));
    }
}
```

#### 3.2 兜底代扣监控

| 监控项 | 目标值 | 告警阈值 | 优先级 | 意义 |
|--------|--------|---------|--------|------|
| 兜底代扣触发率 | <5% | >10% | P2 | 首扣成功率太低 |
| 兜底代扣成功率 | >95% | <90% | P1 | 备用支付可靠性 |
| 兜底代扣执行延迟 | <30s | >60s | P2 | 及时性 |

---

### 4. 云卡同步监控（QW）

| 监控项 | 目标值 | 告警阈值 | 优先级 | 意义 |
|--------|--------|---------|--------|------|
| 会员同步成功率 | >99% | <95% | P0 | 下游系统一致性 |
| 会员同步平均延迟 | <1s | >3s | P1 | 用户体验 |
| 权益行权成功率 | >98% | <95% | P0 | 核心功能 |
| QW API 调用成功率 | >98% | <95% | P0 | 服务可用性 |
| QW API 调用延迟 P99 | <1s | >2s | P1 | 性能 |
| 重试次数分布 | 监控 | 高重试率 | P2 | 稳定性 |

**埋点位置**: `DownstreamSyncServiceImpl`, `QwBenefitClientImpl`

```java
public QwMemberSyncResponse syncMemberOrder(
    QwMemberSyncRequest request) {

    Timer.Sample sample = Timer.start(meterRegistry);
    int retryCount = 0;

    try {
        long startTime = System.currentTimeMillis();
        QwMemberSyncResponse response = qwClient.syncMember(request);
        long duration = System.currentTimeMillis() - startTime;

        // QW 服务调用指标
        meterRegistry.timer("external.qw.sync.duration")
            .record(duration, TimeUnit.MILLISECONDS);
        meterRegistry.counter("external.qw.sync",
            "status", "success"
        ).increment();

        // 业务指标
        meterRegistry.counter("business.qw.member_sync",
            "status", "success",
            "retry_count", String.valueOf(retryCount)
        ).increment();

        return response;
    } catch (QwServiceException e) {
        meterRegistry.counter("external.qw.sync",
            "status", "failed",
            "error_code", e.getErrorCode()
        ).increment();

        meterRegistry.counter("business.qw.member_sync",
            "status", "failed",
            "reason", "qw_service_error",
            "retry_count", String.valueOf(retryCount)
        ).increment();

        throw e;
    } catch (Exception e) {
        meterRegistry.counter("business.qw.member_sync",
            "status", "failed",
            "reason", "unknown_error"
        ).increment();
        throw e;
    } finally {
        sample.stop(Timer.builder("business.qw.sync.duration")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(meterRegistry));
    }
}
```

---

### 5. 通知与消费监控

| 监控项 | 目标值 | 告警阈值 | 优先级 | 意义 |
|--------|--------|---------|--------|------|
| 通知生成速率 | 监控 | - | P2 | 业务负载 |
| 通知消费成功率 | >99% | <95% | P0 | 通知可靠性 |
| 通知消费延迟 | <5s | >10s | P1 | 实时性 |
| 通知消费失败原因 | 监控 | - | P2 | 故障诊断 |
| 死信队列中的消息数 | 0 | >0 | P1 | 需要人工干预 |

**埋点位置**: `NotificationServiceImpl`, `NotificationCallbackController`

```java
@PostMapping("/notifications/consume")
public void consumeNotification(@RequestBody NotificationEvent event) {
    Timer.Sample sample = Timer.start(meterRegistry);

    try {
        meterRegistry.counter("business.notification.consumed",
            "status", "processing"
        ).increment();

        notificationService.process(event);

        meterRegistry.counter("business.notification.consumed",
            "status", "success",
            "event_type", event.getType()
        ).increment();
    } catch (RetryableException e) {
        meterRegistry.counter("business.notification.consumed",
            "status", "retryable_error",
            "event_type", event.getType()
        ).increment();
        throw e;
    } catch (Exception e) {
        meterRegistry.counter("business.notification.consumed",
            "status", "failed",
            "event_type", event.getType(),
            "error", e.getClass().getSimpleName()
        ).increment();
        throw e;
    } finally {
        sample.stop(Timer.builder("business.notification.consume.duration")
            .tag("event_type", event.getType())
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(meterRegistry));
    }
}
```

---

### 6. 数据对账监控

| 监控项 | 目标值 | 告警阈值 | 优先级 | 意义 |
|--------|--------|---------|--------|------|
| 对账周期 | 每小时 | 延迟>2小时 | P1 | 及时发现数据差异 |
| 对账成功率 | 100% | <99% | P0 | 数据一致性 |
| 数据差异发现率 | <0.01% | >0.05% | P0 | 财务准确性 |
| 差异订单处理时间 | <1小时 | >4小时 | P1 | 运营效率 |

**埋点位置**: `ReconciliationServiceImpl`

```java
@Scheduled(cron = "0 0 * * * *") // 每小时执行
public void executeReconciliation() {
    Timer.Sample sample = Timer.start(meterRegistry);

    try {
        ReconciliationResult result = reconciliationService.reconcile();

        // 对账指标
        meterRegistry.counter("business.reconciliation.executed").increment();
        meterRegistry.gauge("business.reconciliation.discrepancies",
            result.getDiscrepancyCount());

        if (result.getDiscrepancyCount() > 0) {
            meterRegistry.counter("business.reconciliation.discrepancies_found",
                "count", String.valueOf(result.getDiscrepancyCount()),
                "types", result.getDiscrepancyTypes()
            ).increment();
        }

        // 对账结果指标
        meterRegistry.counter("business.reconciliation.status",
            "status", result.isSuccess() ? "success" : "partial_success"
        ).increment();
    } catch (Exception e) {
        meterRegistry.counter("business.reconciliation.status",
            "status", "failed"
        ).increment();
        throw e;
    } finally {
        sample.stop(Timer.builder("business.reconciliation.duration")
            .description("对账执行耗时")
            .register(meterRegistry));
    }
}
```

---

### 7. 业务关键指标汇总

| 指标名称 | 计算方式 | 示例查询 | 告警阈值 | 优先级 |
|---------|---------|---------|---------|--------|
| **核心成功率** | (成功数/(成功数+失败数)) × 100% | 订单成功率 | >95% | P0 |
| **关键路径延迟** | 业务流程端到端时间 | 订单创建→首扣→行权 | <10s | P1 |
| **流动性指标** | 待处理状态订单数 | 待首扣订单 | <100单 | P2 |
| **可靠性指标** | 自动恢复成功率 | 重试成功率 | >90% | P2 |

---

## 推荐监控方案

### 方案对比

| 维度 | 纯阿里云 | 阿里云+开源 | ARMS | 开源 |
|------|--------|-----------|------|------|
| 部署复杂度 | ⭐☆☆☆☆ | ⭐⭐⭐⭐☆ | ⭐☆☆☆☆ | ⭐⭐⭐⭐⭐ |
| 维护复杂度 | ⭐☆☆☆☆ | ⭐⭐⭐⭐☆ | ⭐☆☆☆☆ | ⭐⭐⭐⭐☆ |
| 成本 | ¥80-150/月 | ¥50-80/月 | ¥200-500/月 | $0 + ECS |
| 功能完整性 | 7/10 | 9/10 | 10/10 | 9/10 |
| 学习曲线 | 2/10 | 6/10 | 2/10 | 7/10 |
| **推荐度** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐ |

### 最终推荐：阿里云原生方案

**核心观点**:
- 零额外基础设施投入
- 与现有 ECS/RDS 原生集成
- 成本透明可控（按量付费）
- 完整的中文支持和文档

**架构**:
```
Spring Boot 应用
  ├─ Micrometer 指标采集
  │   └─ CloudMonitor 推送
  ├─ SLF4J 日志输出
  │   └─ 日志服务 SLS 推送
  └─ TraceId 传播
      └─ 日志聚合关联
         ↓
    CloudMonitor 仪表板
         ↓
    告警规则 → 钉钉/邮件
```

---

## 实施路线图

### Phase 1: 快速启动（第 1-2 周）

**目标**: 部署基础监控和应用性能指标

**任务**:

1. **CloudMonitor 告警配置**（0.5 天）
   - ECS CPU/内存/磁盘告警
   - RDS 连接数/慢查询告警
   - 通知渠道配置（钉钉、邮件）

2. **应用端 Micrometer 集成**（1 天）
   ```xml
   <dependency>
       <groupId>io.micrometer</groupId>
       <artifactId>micrometer-registry-prometheus</artifactId>
   </dependency>
   <dependency>
       <groupId>org.springframework.boot</groupId>
       <artifactId>spring-boot-starter-actuator</artifactId>
   </dependency>
   ```

3. **日志服务 SLS 配置**（0.5 天）
   - 创建 Project 和 Logstore
   - 配置 Logback Appender
   - 日志查询规则设置

**预期交付**:
- ✓ 基础设施监控仪表板
- ✓ 应用 JVM 指标可见
- ✓ 基础告警规则生效

**工作量**: 3-4 个开发日

---

### Phase 2: 完整业务监控（第 3-4 周）

**目标**: 覆盖所有关键业务指标

**任务**:

1. **业务指标埋点**（2 天）
   - 认证流程（登录、Token 验证）
   - 订单创建和状态转移
   - 支付处理（首扣、兜底、回调）
   - 云卡同步和权益行权
   - 通知消费和对账

2. **告警规则配置**（1 天）
   - 关键业务指标告警
   - 分级告警（P0/P1/P2）
   - 告警聚合去重

3. **仪表板设计**（1 day）
   - 实时业务监控面板
   - 服务可用性概览
   - 故障诊断快速查询

**预期交付**:
- ✓ 完整的业务监控
- ✓ 自动告警机制
- ✓ 美观易用的仪表板

**工作量**: 4-5 个开发日

---

### Phase 3: 高级能力（第 5-8 周，可选）

**目标**: 链路追踪和自适应告警

**任务**:

1. **链路追踪**（可选，2 天）
   - Spring Cloud Sleuth 集成
   - Jaeger Server 部署
   - 跨服务追踪

2. **自适应告警**（1 day）
   - 基于历史数据的异常检测
   - 动态阈值调整

3. **成本优化**（1 day）
   - 日志采样策略
   - 数据保留期优化

**工作量**: 3-5 个开发日（可选）

---

## 成本估算

### 第一阶段成本（月）

| 项目 | 数量 | 单价 | 合计 |
|------|------|------|------|
| CloudMonitor | 免费 | - | ¥0 |
| 日志服务 SLS | 5GB/天 | ¥20/GB | ¥100 |
| 消息队列（告警） | 1000/月 | ¥0.001 | ¥1 |
| **月合计** | | | **¥101** |
| **年合计** | | | **¥1,212** |

### 与商业方案对比

```
年度成本对比（日均 5GB 日志）：

方案 A - 阿里云原生：
  月均: ¥100-150
  年均: ¥1,200-1,800
  ✓ 推荐，最经济

方案 B - Prometheus + 开源：
  月均: ¥50-80 (额外 ECS) + $0 (开源)
  年均: ¥600-960
  ✓ 成本更低，但维护复杂

方案 C - ARMS 商业：
  月均: ¥200-500
  年均: ¥2,400-6,000
  ✗ 成本是方案A的2-5倍

方案 D - New Relic：
  月均: $50-100
  年均: $600-1,200 (~¥4,200-8,400)
  ✗ 成本是方案A的3-7倍

方案 E - DataDog：
  月均: $100-200
  年均: $1,200-2,400 (~¥8,400-16,800)
  ✗ 成本是方案A的7-14倍

节省对比（vs 商业方案）：
vs ARMS:       ¥1,200-4,200/年 (50-77% 节省)
vs New Relic:  ¥3,000-6,600/年 (72-85% 节省)
vs DataDog:    ¥6,600-15,000/年 (79-89% 节省)
```

---

## 关键告警规则配置

### P0 级告警（立即通知）

```
规则 1: 订单创建成功率 < 95%
触发: 连续 5 分钟
通知: 钉钉 + 邮件 + 短信
消息: "【严重】订单服务不可用，成功率: {rate}%"

规则 2: 首扣代扣成功率 < 95%
触发: 连续 3 分钟
通知: 钉钉 + 邮件 + 短信
消息: "【严重】支付系统故障，成功率: {rate}%"

规则 3: QW 调用成功率 < 95%
触发: 连续 5 分钟
通知: 钉钉 + 邮件
消息: "【严重】云卡服务故障，成功率: {rate}%"

规则 4: RDS 连接数 > 80%
触发: 立即
通知: 钉钉 + 邮件
消息: "【严重】数据库连接即将耗尽，当前使用: {usage}%"

规则 5: ECS CPU > 85% 且持续 > 10 分钟
触发: 连续 10 分钟
通知: 钉钉 + 邮件
消息: "【严重】服务器 CPU 过高，可能出现雪崩"
```

### P1 级告警（需要关注）

```
规则 1: 请求延迟 P99 > 2s
触发: 连续 5 分钟
通知: 钉钉
消息: "请求延迟升高，P99: {p99}ms"

规则 2: 错误率 > 0.5%
触发: 连续 3 分钟
通知: 钉钉
消息: "应用错误率升高，错误率: {rate}%"

规则 3: 云卡同步延迟 > 3s
触发: 连续 5 分钟
通知: 钉钉
消息: "云卡同步变慢，P99 延迟: {latency}ms"

规则 4: 对账失败或差异超阈值
触发: 立即
通知: 钉钉 + 邮件
消息: "对账异常，发现差异订单: {count} 笔"
```

### P2 级告警（监控）

```
规则 1: RDS 慢查询 > 20/小时
触发: 立即
通知: 邮件
消息: "数据库慢查询告警"

规则 2: JVM 堆内存 > 85%
触发: 连续 3 分钟
通知: 钉钉
消息: "内存警告，堆内存使用: {usage}%"

规则 3: 数据库连接池活跃连接 > 30
触发: 连续 5 分钟
通知: 邮件
消息: "数据库连接使用过多"
```

---

## 监控指标汇总表

### 核心监控指标清单

| 分类 | 指标名称 | Micrometer 指标名 | 采样间隔 | 告警阈值 |
|------|---------|------------------|---------|---------|
| **基础设施** | ECS CPU | system.cpu.usage | 1m | >80% |
| | RDS 连接数 | rds.connections.active | 1m | >80% |
| **应用层** | 请求延迟 P99 | http.request.duration{quantile=0.99} | 1m | >2000ms |
| | 错误率 | http.server.requests{status=5xx} | 1m | >0.5% |
| **业务** | 订单成功率 | business.order.created{status=success} | 5m | <95% |
| | 首扣成功率 | business.payment.first_deduct{status=success} | 5m | <95% |
| | QW 同步成功率 | business.qw.member_sync{status=success} | 5m | <95% |
| | 通知消费成功率 | business.notification.consumed{status=success} | 5m | <95% |

---

## 维护与运营

### 日常检查清单

**每日** (10 分钟):
- [ ] 查看告警消息，确认无遗漏
- [ ] 检查主要业务指标趋势

**每周** (30 分钟):
- [ ] 审计告警规则准确度（误报/漏报）
- [ ] 检查监控数据完整性
- [ ] 优化日志搜索规则

**每月** (1-2 小时):
- [ ] 性能趋势分析
- [ ] 成本优化评估
- [ ] 文档更新

### 常见故障快速诊断

**订单成功率下降**:
1. 检查 ECS CPU/内存是否过高
2. 检查 RDS 连接数和慢查询
3. 检查 QW/Allinpay 服务可用性
4. 查看应用错误日志

**支付失败率升高**:
1. 检查 Allinpay 服务状态
2. 查看 Allinpay 错误日志
3. 检查首扣和兜底的成功率分布
4. 联系 Allinpay 技术支持

**云卡同步延迟**:
1. 检查 QW API 可达性
2. 查看 QW 调用延迟指标
3. 检查网络连接状态
4. 评估是否需要增加重试

---

## 总结与建议

### 关键成功要素

1. **快速部署** (2 周)
   - 选择阿里云原生方案
   - 迅速建立基础监控

2. **全面覆盖** (4 周)
   - 系统层面 + 业务层面
   - 所有关键路径的监控

3. **持续优化**
   - 月度告警规则调优
   - 季度架构评估

### 预期效果

| 指标 | 现状 | 目标（3个月） | 提升 |
|------|------|----------|------|
| 故障发现时间 | 30 分钟 | 2 分钟 | **93% ↓** |
| 问题诊断时间 | 20 分钟 | 5 分钟 | **75% ↓** |
| 可用性透明度 | 被动感知 | 主动监控 | **关键提升** |

### 投入与回报

```
投入:
- 工作量: 10-15 个开发日（2-3 周）
- 成本: ¥1,200-1,800/年（运营）
- 学习曲线: 2-4 周

回报:
- 故障发现时间从 30 分钟降至 2 分钟
- 与商业方案相比节省 70%+ 成本
- 建立关键业务的完整可观测性
- 支撑公司业务增长和风险管理
```

---

## 附录

### A. 推荐的监控指标库

完整的指标清单已在上面各部分详细描述，包括：
- 系统层面：基础设施、JVM、应用层、数据库
- 业务层面：认证、订单、支付、云卡、通知、对账

### B. 集成代码示例

详见上面各业务流程的埋点代码示例

### C. 告警规则模板

详见上面"关键告警规则配置"部分

### D. 相关参考文档

- [MONITORING_STRATEGY_ALIYUN.md](./MONITORING_STRATEGY_ALIYUN.md) - 详细的阿里云方案分析
- [RISK_ASSESSMENT_20260331.md](./RISK_ASSESSMENT_20260331.md) - 风险评估报告

---

**文档版本**: 1.0
**最后更新**: 2026-03-31
**下一步**: 待团队评审和确认，准备进入 Phase 1 实施
