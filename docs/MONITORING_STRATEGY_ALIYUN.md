# NexusFin Equity 项目 - 阿里云监控系统分析方案

**文档日期**: 2026-03-31
**部署环境**: 阿里云（ECS + RDS + 对象存储）
**目标**: 简单、成本低、部署方便、易于维护

---

## 执行摘要

### 核心建议

**推荐方案**: 阿里云原生监控 + 开源补充

```
阶层                 推荐方案                      备注
─────────────────────────────────────────────────────────
基础设施监控    → CloudMonitor（免费）        → ECS、RDS 自动集成
日志分析        → 日志服务 SLS（按量付费）    → 与 CloudMonitor 联动
链路追踪        → 应用实时监控 ARMS 或开源    → 可选，根据预算选择
告警通知        → CloudMonitor 告警 + 云消息队列 → 原生集成
```

### 成本估算

```
阿里云原生方案（月均成本）：
├─ CloudMonitor：       免费（ECS/RDS 自带）
├─ 日志服务 SLS：       50-100 RMB（5GB/天日志量）
├─ 消息队列 MQ：        30-50 RMB（告警通知）
└─ 总计：              80-150 RMB/月 (~$12-22/月)

vs. Prometheus + Grafana + Loki：
├─ 4个容器运行成本：    50-80 RMB/月（额外ECS）
└─ 总计：              50-80 RMB/月

vs. 商业方案：
├─ New Relic：         $50-100/月
└─ DataDog：           $100-200/月
```

**年度节省**: 至少 $300-500（vs 商业方案）

---

## 项目现状分析

### 1. 部署架构

```
阿里云部署现状：
┌─────────────────────────────────────────┐
│           阿里云 VPC                    │
├─────────────────────────────────────────┤
│  ECS (Elastic Compute Service)          │
│  ├─ Spring Boot 应用 (端口 8080)      │
│  └─ 运行环境: Java 17                   │
├─────────────────────────────────────────┤
│  RDS (Relational Database Service)      │
│  └─ MySQL 8.0（生产数据）              │
├─────────────────────────────────────────┤
│  Object Storage Service (OSS)           │
│  └─ 日志、文件存储                     │
├─────────────────────────────────────────┤
│  CloudMonitor（云监控）                 │
│  └─ 基础设施自动监控                   │
└─────────────────────────────────────────┘
     ↓ 外部服务集成
  QW / Tech Platform / Allinpay
```

### 2. 现有监控能力

✅ **已有**:
- ECS CPU、内存、网络自动监控
- RDS 连接数、慢查询、磁盘空间自动监控
- 应用日志（stdout/stderr）写入 ECS 本地磁盘
- TraceId 和 RequestId 传播机制已实现

❌ **缺失**:
- 应用性能指标（请求延迟、吞吐量、错误率）
- 业务指标（订单成功率、支付成功率）
- 日志聚合分析
- 自动告警机制
- 链路追踪

---

## 监控需求分析

### 1. 基础设施监控需求

| 监控项 | 目标值 | 告警阈值 | 优先级 |
|--------|--------|---------|--------|
| ECS CPU 使用率 | < 70% | > 80% | P1 |
| ECS 内存使用率 | < 75% | > 85% | P1 |
| RDS CPU 使用率 | < 60% | > 75% | P1 |
| RDS 连接数 | < 50% | > 80% | P1 |
| RDS 慢查询（>1s） | < 10/小时 | > 20/小时 | P2 |
| 磁盘空间 | < 80% | > 90% | P1 |
| 网络出站 | 无限 | > 500Mbps 则告警 | P2 |

**现状**: CloudMonitor 已自动收集以上所有指标 ✓

---

### 2. 应用性能监控需求

| 监控项 | 目标值 | 告警阈值 | 优先级 |
|--------|--------|---------|--------|
| 请求延迟 P50 | < 100ms | - | P1 |
| 请求延迟 P99 | < 1s | > 2s | P1 |
| 请求吞吐量 | > 100 req/s | < 50 req/s | P2 |
| 错误率 | < 0.1% | > 0.5% | P1 |
| JVM 堆内存 | < 80% | > 90% | P1 |
| GC 暂停时间 | < 100ms | > 500ms | P2 |
| 数据库连接池 | < 50% | > 80% | P1 |

**现状**: 需要应用端暴露指标

---

### 3. 业务指标监控需求

| 指标 | 目标值 | 告警阈值 | 数据源 |
|------|--------|---------|--------|
| 订单创建成功率 | > 99% | < 95% | 应用日志 |
| 订单首扣成功率 | > 99% | < 95% | 数据库 |
| 支付回调成功率 | > 99.5% | < 99% | 应用日志 |
| QW 调用成功率 | > 98% | < 95% | 应用日志 |
| Tech Platform 调用成功率 | > 99% | < 95% | 应用日志 |
| 通知消费延迟 | < 5s | > 10s | 应用日志 |
| 订单对账差异率 | < 0.01% | > 0.05% | 数据库 |

**现状**: 完全缺失，需要代码集成

---

### 4. 数据库监控需求

| 监控项 | 目标值 | 告警阈值 | 优先级 |
|--------|--------|---------|--------|
| 查询延迟 P99 | < 100ms | > 500ms | P1 |
| 行锁等待时间 | < 10ms | > 50ms | P2 |
| 事务回滚率 | < 0.1% | > 0.5% | P2 |
| 慢查询数量 | < 10/小时 | > 20/小时 | P1 |
| 主从延迟 | < 1s | > 5s | P1 |
| 备份完成时间 | < 1小时 | > 2小时 | P2 |

**现状**: RDS 控制台可查看，但无自动告警

---

### 5. 外部服务监控需求

| 服务 | 监控项 | 目标值 | 告警阈值 |
|------|--------|--------|---------|
| QW | 调用延迟 P99 | < 500ms | > 1s |
| QW | 成功率 | > 98% | < 95% |
| Tech Platform | 调用延迟 P99 | < 300ms | > 1s |
| Tech Platform | 成功率 | > 99% | < 95% |
| Allinpay Direct | 调用延迟 P99 | < 1s | > 2s |
| Allinpay Direct | 成功率 | > 99% | < 95% |

**现状**: 无监控，仅通过异常捕获

---

## 方案选型与对比

### 方案 1: 纯阿里云原生方案（推荐）

**架构**:
```
应用 → CloudMonitor（指标）→ 仪表板
       日志服务 SLS（日志）→ 分析
       消息队列 MQ（告警）→ Slack/邮件
       ARMS（可选链路追踪）
```

**优点**:
- ✅ 部署最简单（仅需代码集成）
- ✅ 成本最低（免费 + 按量付费）
- ✅ 与 ECS/RDS 原生集成
- ✅ 界面统一，学习曲线平缓
- ✅ 支持中文界面和中文文档
- ✅ 无需额外基础设施投入
- ✅ 完全托管，无维护负担

**缺点**:
- ❌ 定制性不如开源方案灵活
- ❌ 链路追踪功能需要额外费用（ARMS）
- ❌ 日志分析功能不如 ELK 强大

**成本**: 80-150 RMB/月（仅 SLS）

**部署时间**: 1-2 天

**学习曲线**: 2/10（很低）

---

### 方案 2: 阿里云 + 开源补充方案

**架构**:
```
应用 → CloudMonitor（基础指标）
       Prometheus（应用指标）→ Grafana
       Loki（日志）
       Jaeger（链路追踪，可选）
```

**优点**:
- ✅ 定制性强
- ✅ 完整的链路追踪
- ✅ 日志分析功能更强
- ✅ 完全开源免费

**缺点**:
- ❌ 需要额外的 ECS 资源（容器运行）
- ❌ 部署复杂度高
- ❌ 维护成本高（需要 DevOps 知识）
- ❌ 需要学习多个工具

**成本**: 50-80 RMB/月（额外 ECS） + 0（开源）= 50-80 RMB/月

**部署时间**: 3-5 天

**学习曲线**: 6/10（中等）

---

### 方案 3: 阿里云 ARMS（商业方案）

**特点**:
```
应用 → ARMS → 完整的 APM（应用性能管理）
         包括：指标、日志、链路追踪、错误追踪、实时监控
```

**优点**:
- ✅ 功能最完整
- ✅ 与阿里云深度集成
- ✅ 自动采集无需代码改动
- ✅ 链路追踪、智能告警

**缺点**:
- ❌ 成本高（按日志量计费）
- ❌ 预算约 200-500 RMB/月
- ❌ 对小型项目不划算

**成本**: 200-500 RMB/月

**适用场景**: 大型/关键业务系统

---

### 方案 4: 混合方案（折中方案）

**分阶段选择**:
```
第一阶段（立即）：
  CloudMonitor + 日志服务 SLS
  成本: 80-150 RMB/月

第二阶段（2 个月后）：
  + Prometheus + Grafana（如果需要更多定制）
  成本: +50-80 RMB/月

第三阶段（6 个月后）：
  + ARMS 或 Jaeger（如果需要链路追踪）
  成本: +200-500 RMB/月（ARMS）或 +0（Jaeger）
```

---

## 详细方案对比表

| 维度 | 纯阿里云 | 阿里云+开源 | ARMS | 开源 |
|------|--------|-----------|------|------|
| **部署复杂度** | 1/10 | 4/10 | 1/10 | 6/10 |
| **维护复杂度** | 1/10 | 4/10 | 1/10 | 5/10 |
| **功能完整性** | 7/10 | 9/10 | 10/10 | 9/10 |
| **成本（月）** | 80-150 | 50-80 | 200-500 | 50-80 |
| **学习曲线** | 2/10 | 6/10 | 2/10 | 7/10 |
| **文档质量** | 9/10 | 8/10 | 8/10 | 7/10 |
| **中文支持** | 10/10 | 7/10 | 10/10 | 5/10 |
| **定制性** | 6/10 | 9/10 | 7/10 | 10/10 |
| **总体推荐度** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐ |

---

## 推荐方案详解：纯阿里云原生方案

### 架构设计

```
┌─────────────────────────────────────────────────────┐
│              Spring Boot 应用 (ECS)                 │
├─────────────────────────────────────────────────────┤
│ ┌─────────────────────────────────────────────────┐ │
│ │      Micrometer 指标采集                       │ │
│ │  ├─ JVM (CPU, 内存, GC)                        │ │
│ │  ├─ Tomcat (请求数, 延迟, 错误)               │ │
│ │  └─ 自定义业务指标                             │ │
│ └─────────────────────────────────────────────────┘ │
│             ↓ 推送到 CloudMonitor                  │
│ ┌─────────────────────────────────────────────────┐ │
│ │      SLF4J 日志输出                            │ │
│ │  ├─ 应用日志 (INFO/WARN/ERROR)               │ │
│ │  └─ 业务日志 (订单、支付等)                   │ │
│ └─────────────────────────────────────────────────┘ │
│             ↓ 同步到日志服务 SLS                   │
└─────────────────────────────────────────────────────┘
         ↓
┌─────────────────────────────────────────────────────┐
│           阿里云监控中心（云监控）                  │
├─────────────────────────────────────────────────────┤
│ • 指标存储（CloudMonitor）                         │
│ • 日志分析（日志服务 SLS）                        │
│ • 自动告警（CloudMonitor 告警）                   │
│ • 统一仪表板（Grafana 或阿里云 Dashboard）       │
├─────────────────────────────────────────────────────┤
│ 告警规则：                                          │
│ ├─ P99 延迟 > 1s → Slack                          │
│ ├─ 错误率 > 0.5% → 钉钉                          │
│ ├─ RDS 连接数 > 80% → 邮件                       │
│ └─ 订单成功率 < 95% → 钉钉 + 邮件               │
└─────────────────────────────────────────────────────┘
         ↓
┌─────────────────────────────────────────────────────┐
│           通知渠道                                  │
├─────────────────────────────────────────────────────┤
│ ✓ 钉钉（企业内部通知）                            │
│ ✓ Slack（团队协作）                               │
│ ✓ 邮件（记录存档）                                │
│ ✓ 短信（关键告警）                                │
└─────────────────────────────────────────────────────┘
```

---

## 实施方案：分阶段部署

### Phase 1: 快速启动（第 1-2 周）

**目标**: 部署基础监控，覆盖基础设施和应用性能

**任务清单**:

1. **CloudMonitor 配置**（0.5 天）
   - 登录阿里云控制台
   - 配置基础设施监控告警（ECS、RDS）
   - 配置告警通知（钉钉、邮件）
   - 创建基础监控仪表板

2. **应用端集成 Micrometer**（1 天）
   ```xml
   <!-- pom.xml 中添加 -->
   <dependency>
     <groupId>io.micrometer</groupId>
     <artifactId>micrometer-registry-prometheus</artifactId>
   </dependency>
   <dependency>
     <groupId>org.springframework.cloud</groupId>
     <artifactId>spring-cloud-starter-alicloudmonitor</artifactId>
   </dependency>
   ```

3. **业务指标埋点**（1.5 天）
   ```java
   // 在 BenefitOrderServiceImpl 中
   @PostMapping("/orders")
   public CreateBenefitOrderResponse createOrder(...) {
       try {
           CreateBenefitOrderResponse response = service.createOrder(...);
           meterRegistry.counter(
               "benefit.order.created",
               "status", "success"
           ).increment();
           return response;
       } catch (Exception e) {
           meterRegistry.counter(
               "benefit.order.created",
               "status", "failed"
           ).increment();
           throw e;
       }
   }
   ```

4. **日志服务 SLS 配置**（0.5 天）
   - 在阿里云创建 Project 和 Logstore
   - 配置应用日志导出
   - 创建日志查询规则

**预期效果**:
- ✓ 可视化 ECS/RDS 监控数据
- ✓ 看到应用 JVM 指标（堆内存、GC）
- ✓ 看到请求吞吐量和延迟
- ✓ 可配置基础告警规则
- ✓ 日志可搜索、可分析

**成本**: 0 + 50-100 RMB/月（SLS）

**工作量**: 3-4 个开发日

---

### Phase 2: 完整监控（第 3-4 周）

**目标**: 补充业务指标监控和告警

**任务清单**:

1. **业务指标埋点**（2 天）
   - 订单创建成功率
   - 支付处理成功率
   - 外部服务调用成功率和延迟
   - 通知消费延迟
   - 数据库慢查询数量

   ```java
   // 示例：支付服务指标
   public PaymentStatusResponse handleFirstDeductCallback(...) {
       Timer.Sample sample = Timer.start(meterRegistry);
       try {
           PaymentStatusResponse response = service.handle(...);
           meterRegistry.counter(
               "payment.callback.processed",
               "type", "first_deduct",
               "status", "success"
           ).increment();
           return response;
       } catch (Exception e) {
           meterRegistry.counter(
               "payment.callback.processed",
               "type", "first_deduct",
               "status", "failed"
           ).increment();
           throw e;
       } finally {
           sample.stop(Timer.builder("payment.callback.duration")
               .description("支付回调处理耗时")
               .publishPercentiles(0.5, 0.95, 0.99)
               .register(meterRegistry));
       }
   }
   ```

2. **告警规则配置**（1 day）
   - P99 延迟 > 1s → Slack
   - 错误率 > 0.5% → 钉钉
   - 订单成功率 < 95% → 钉钉 + 邮件
   - QW 调用成功率 < 95% → 邮件
   - RDS 慢查询 > 20/小时 → 邮件

   ```
   CloudMonitor 告警规则示例：
   ├─ 规则 1: request.duration.seconds > 1.0
   │          for 5 consecutive data points
   │          → 发送钉钉消息
   ├─ 规则 2: error_rate > 0.005
   │          for 3 consecutive data points
   │          → 发送钉钉 + 邮件
   └─ 规则 3: business.order.created{status=failed} > 0.05
   │          for 10 consecutive data points
   │          → 发送钉钉 + 邮件 + 短信
   ```

3. **告警通知集成**（1 day）
   - 钉钉群机器人集成
   - Slack Webhook 配置
   - 邮件通知配置

4. **仪表板完善**（1 day）
   - 关键指标概览
   - 订单处理流程监控
   - 外部服务健康状态
   - 数据库性能监控

**预期效果**:
- ✓ 看到完整的业务指标
- ✓ 自动告警机制启用
- ✓ 钉钉/Slack 实时通知
- ✓ 完整的监控仪表板
- ✓ 可生成监控报告

**成本**: 80-150 RMB/月

**工作量**: 4-5 个开发日

---

### Phase 3: 高级功能（第 5-8 周）

**目标**: 补充链路追踪和高级分析

**可选任务**:

1. **集成 Jaeger 链路追踪**（2 天）
   - Spring Cloud Sleuth 集成
   - Jaeger Server 部署（ECS）
   - 追踪跨服务请求

2. **升级到 ARMS**（如果需要）（1 day）
   - 评估成本效益
   - 迁移现有配置
   - 对比 Jaeger 功能差异

3. **自适应告警**（1 day）
   - 基于历史数据的异常检测
   - 机器学习算法应用
   - 告警准确度提升

4. **成本优化**（1 day）
   - 日志存储优化
   - 数据采样策略
   - 成本与功能平衡

**成本**: +50-500 RMB/月（取决于选择）

**工作量**: 3-5 个开发日（可选）

---

## 代码改动清单

### 1. pom.xml 依赖添加

```xml
<!-- Micrometer + 阿里云监控 -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
    <version>1.12.0</version>
</dependency>

<!-- 阿里云监控 SDK -->
<dependency>
    <groupId>com.aliyun</groupId>
    <artifactId>aliyun-java-sdk-cms</artifactId>
    <version>7.15.0</version>
</dependency>

<!-- 可选：链路追踪 -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-sleuth</artifactId>
    <version>4.0.2</version>
</dependency>
```

### 2. application.yml 配置

```yaml
management:
  endpoints:
    web:
      exposure:
        include: metrics,health,info,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
      cloudwatch:
        enabled: true

logging:
  pattern:
    console: "[%d{yyyy-MM-dd HH:mm:ss.SSS}] [%thread] %-5level %logger{36} - [traceId:%X{traceId}] - %msg%n"
  level:
    root: INFO
    com.nexusfin: DEBUG
  file:
    name: logs/application.log
    max-size: 1GB
    max-history: 30
```

### 3. 自定义 BusinessMetrics 类

```java
package com.nexusfin.equity.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class BusinessMetrics {

    private final MeterRegistry meterRegistry;

    public BusinessMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    // 订单指标
    public void recordOrderCreated(String status) {
        meterRegistry.counter(
            "business.order.created",
            "status", status
        ).increment();
    }

    // 支付指标
    public Timer.Sample startPaymentCallback() {
        return Timer.start(meterRegistry);
    }

    public void recordPaymentCallback(
        Timer.Sample sample,
        String type,
        String status
    ) {
        sample.stop(Timer.builder("business.payment.callback")
            .tag("type", type)
            .tag("status", status)
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(meterRegistry));
    }

    // 外部服务指标
    public void recordExternalServiceCall(
        String serviceName,
        long durationMs,
        String status
    ) {
        meterRegistry.timer(
            "external.service.call",
            "service", serviceName,
            "status", status
        ).record(durationMs, TimeUnit.MILLISECONDS);
    }
}
```

### 4. 服务类中的指标集成

```java
// BenefitOrderServiceImpl
public CreateBenefitOrderResponse createOrder(String memberId, ...) {
    try {
        CreateBenefitOrderResponse response = ...;
        businessMetrics.recordOrderCreated("success");
        return response;
    } catch (Exception e) {
        businessMetrics.recordOrderCreated("failed");
        throw e;
    }
}

// PaymentServiceImpl
public PaymentStatusResponse handleFirstDeductCallback(...) {
    Timer.Sample sample = businessMetrics.startPaymentCallback();
    try {
        PaymentStatusResponse response = ...;
        businessMetrics.recordPaymentCallback(sample, "first_deduct", "success");
        return response;
    } catch (Exception e) {
        businessMetrics.recordPaymentCallback(sample, "first_deduct", "failed");
        throw e;
    }
}

// QwBenefitClientImpl
public QwMemberSyncResponse syncMemberOrder(QwMemberSyncRequest request) {
    long startTime = System.currentTimeMillis();
    try {
        QwMemberSyncResponse response = ...;
        businessMetrics.recordExternalServiceCall(
            "qw",
            System.currentTimeMillis() - startTime,
            "success"
        );
        return response;
    } catch (Exception e) {
        businessMetrics.recordExternalServiceCall(
            "qw",
            System.currentTimeMillis() - startTime,
            "failed"
        );
        throw e;
    }
}
```

---

## CloudMonitor 告警规则配置

### 告警规则示例（在阿里云控制台配置）

**规则 1: 应用高延迟告警**
```
指标: 请求延迟 P99 > 1000ms
条件: 连续 5 个数据点达到阈值
触发: 立即发送钉钉通知
消息: "订单服务 P99 延迟超过 1 秒，请检查数据库或外部服务"
```

**规则 2: 应用错误率告警**
```
指标: 错误率 > 0.5%
条件: 连续 3 个数据点达到阈值
触发: 发送钉钉 + 邮件
消息: "订单服务错误率升高，错误率: {error_rate}%"
```

**规则 3: 业务指标告警**
```
指标: 订单创建失败率 > 5%
条件: 连续 10 个数据点达到阈值
触发: 发送钉钉 + 邮件 + 短信
消息: "订单创建成功率下降至 {success_rate}%，请立即处理"
```

**规则 4: 数据库性能告警**
```
指标: RDS 慢查询 > 20/小时
条件: 立即触发
触发: 发送邮件
消息: "数据库慢查询超过阈值，当前: {count} 次/小时"
```

---

## Grafana 仪表板配置（可选）

如果选择使用 Grafana 可视化（可与阿里云 CloudMonitor 兼容）：

```json
{
  "dashboard": {
    "title": "NexusFin Equity 监控仪表板",
    "panels": [
      {
        "title": "请求延迟分布",
        "targets": [
          {
            "expr": "histogram_quantile(0.99, request_duration_seconds)"
          }
        ]
      },
      {
        "title": "订单创建成功率",
        "targets": [
          {
            "expr": "increase(order_created{status=\"success\"}[5m]) / increase(order_created[5m])"
          }
        ]
      },
      {
        "title": "外部服务调用成功率",
        "targets": [
          {
            "expr": "external_service_calls{status=\"success\"} / external_service_calls"
          }
        ]
      },
      {
        "title": "JVM 内存使用",
        "targets": [
          {
            "expr": "jvm_memory_used_bytes / jvm_memory_max_bytes"
          }
        ]
      },
      {
        "title": "数据库连接池",
        "targets": [
          {
            "expr": "hikaricp_connections_active / hikaricp_connections"
          }
        ]
      }
    ]
  }
}
```

---

## 日志服务 SLS 配置

### 日志导出配置

**步骤 1**: 在阿里云创建 Project
```
Project 名称: nexusfin-equity
地域: 华东 1（杭州）或根据实际位置选择
```

**步骤 2**: 创建 Logstore
```
Logstore 名称: app-logs
分片数: 1（可根据日志量调整）
数据保留期: 30 天（可根据需要调整）
```

**步骤 3**: 配置日志导出（在应用中）

使用 Log4j2 或 SLF4J 的 Appender:

```xml
<!-- logback-spring.xml -->
<configuration>
  <appender name="ALIYUN_LOG" class="com.aliyun.openservices.log.logback.LoghubAppender">
    <endpoint>https://cn-hangzhou.log.aliyuncs.com</endpoint>
    <accessKeyId>${ALIYUN_ACCESS_KEY_ID}</accessKeyId>
    <accessKeySecret>${ALIYUN_ACCESS_KEY_SECRET}</accessKeySecret>
    <project>nexusfin-equity</project>
    <logStore>app-logs</logStore>
    <topic>application</topic>
  </appender>

  <root level="INFO">
    <appender-ref ref="ALIYUN_LOG" />
  </root>
</configuration>
```

### 日志查询规则

```
# 查询订单创建成功率
source: "BenefitOrderServiceImpl" AND "orderCreated" AND "success"

# 查询支付回调失败
source: "PaymentServiceImpl" AND "handleCallback" AND "status=failed"

# 查询外部服务错误
source: "QwBenefitClient" AND "error"

# 查询慢查询
source: "SQL" AND duration > 1000
```

---

## 成本估算详情

### 第一阶段成本（月）

| 项目 | 单位 | 数量 | 单价 | 合计 |
|------|------|------|------|------|
| CloudMonitor（基础设施） | - | - | 免费 | ¥0 |
| 日志服务 SLS | GB/天 | 5 | ¥20/GB | ¥100 |
| 消息队列（告警）| 条/月 | 1000 | ¥0.001 | ¥1 |
| **月合计** | | | | **¥101** |
| **年合计** | | | | **¥1,212** |

### 与商业方案对比

```
年度成本对比（假设日均 5GB 日志）：

阿里云原生方案：      ¥1,212/年 ($182/年)
Prometheus+开源：     ¥600-960/年 (额外 ECS) + $0
New Relic：          ¥3,600-7,200/年 ($540-1,080/年)
DataDog：            ¥7,200-14,400/年 ($1,080-2,160/年)

节省对比：
vs. New Relic：      $358-898/年
vs. DataDog：        $898-1,978/年
```

---

## 迁移路线图

### 从无监控到完整监控（8 周）

```
Week 1-2: Phase 1 - 快速启动
├─ Day 1: CloudMonitor 基础配置
├─ Day 2-3: 应用 Micrometer 集成
└─ Day 4: 日志服务 SLS 配置
→ 交付: 基础监控仪表板 + 基础告警

Week 3-4: Phase 2 - 完整监控
├─ Day 1-2: 业务指标埋点
├─ Day 3: 告警规则配置
├─ Day 4: 告警通知集成
└─ Day 5: 仪表板完善
→ 交付: 完整的监控和告警系统

Week 5-8: Phase 3 - 高级功能（可选）
├─ Day 1-2: 链路追踪（Jaeger 或 ARMS）
├─ Day 3: 自适应告警
└─ Day 4: 成本优化
→ 交付: 完整的 APM 系统
```

**总工作量**: 10-15 个开发日（基础+完整监控）

---

## 维护和运营

### 日常运维（每月）

**周检查**:
- [ ] 检查告警是否正常工作
- [ ] 查看监控仪表板的数据趋势
- [ ] 验证日志采集和查询功能

**月检查**:
- [ ] 审计告警规则的准确度（误报/漏报）
- [ ] 优化日志存储（降低成本）
- [ ] 分析性能趋势，识别瓶颈

**季度检查**:
- [ ] 更新告警阈值（基于业务增长）
- [ ] 评估是否需要升级到 ARMS 或 Jaeger
- [ ] 计划优化监控架构

**预期时间**: 2-4 小时/月

---

## 成功指标

### Phase 1 完成后

- ✓ CloudMonitor 仪表板可访问
- ✓ 基础设施指标实时可见
- ✓ 应用 JVM 指标可见
- ✓ 基础告警规则生效
- ✓ 请求延迟 P99 可视化

### Phase 2 完成后

- ✓ 订单成功率监控
- ✓ 支付回调监控
- ✓ 外部服务调用监控
- ✓ 告警通知正常
- ✓ 仪表板完整美观

### Phase 3 完成后（可选）

- ✓ 链路追踪端到端完整
- ✓ 自适应告警降低误报
- ✓ 成本优化到位
- ✓ 完整的 APM 系统

---

## 常见问题

### Q1: 为什么选择阿里云而不是开源方案？

**A**:
- 零部署成本（无需额外 ECS）
- 与现有 ECS/RDS 原生集成
- 学习曲线最低（统一界面）
- 中文文档和支持
- 成本更低（开源需要额外基础设施）

---

### Q2: 日志存储成本会很高吗？

**A**:
- 每天 5GB 日志 ≈ ¥100/月 ≈ ¥1,200/年
- 相比 New Relic（¥3,600/年）便宜 66%
- 可通过日志采样降低成本
- 支持按需付费，成本可控

---

### Q3: 能监控第三方服务（QW、Tech Platform）吗？

**A**:
- 可以通过应用端埋点监控调用延迟和成功率
- 可以监控外部服务的可用性
- 无法直接监控第三方服务的内部状态
- 建议与第三方服务方沟通，获取更多监控数据

---

### Q4: 链路追踪必须吗？

**A**:
- 非必须，可选功能
- 基础监控已覆盖 80% 的需求
- 如果需要端到端追踪，可在 Phase 3 加入
- 建议先用 Phase 1 + 2，待问题出现再加入

---

### Q5: 如何降低成本？

**A**:
- 使用日志采样（只采集 10% 的日志）
- 缩短日志保留期（从 30 天降到 7 天）
- 关闭不必要的指标采集
- 根据业务量调整 SLS Shard 数

---

## 推荐行动计划

### 立即（本周）

- [ ] 审批推荐方案
- [ ] 分配项目负责人（1-2 人）
- [ ] 准备阿里云账户权限

### 第一周

- [ ] CloudMonitor 基础配置
- [ ] 应用 Micrometer 集成
- [ ] 部署到测试环境验证

### 第二周

- [ ] 日志服务 SLS 配置
- [ ] 完成 Phase 1 部署到生产
- [ ] 收集反馈和调整

### 第三周

- [ ] 业务指标埋点
- [ ] 告警规则配置
- [ ] Phase 2 部署

### 第四周

- [ ] 告警通知测试
- [ ] 仪表板优化
- [ ] 团队培训

---

## 总结

**推荐方案**: 阿里云原生监控（CloudMonitor + SLS）

**为什么**:
- ✅ 最简单的部署（无需额外基础设施）
- ✅ 最低的成本（¥1,200/年）
- ✅ 最快的上线时间（2 周）
- ✅ 最容易的维护（完全托管）
- ✅ 最小的学习曲线（统一界面）

**预期效果**:
- 故障发现时间：30 分钟 → 2 分钟（自动告警）
- 问题诊断时间：20 分钟 → 5 分钟（日志分析）
- 性能可见性：仅异常捕获 → 完整的 P99 延迟、业务成功率等

**投入**:
- 工作量：10-15 个开发日（2-3 人，2-4 周）
- 成本：¥1,200-1,500/年（运营）
- ROI：高（与商业方案相比节省 70%+ 成本，功能完整度 80%+）

---

**文档完成日期**: 2026-03-31
**下一步**: 等待团队审批和部署授权

