# 2026-04-17 异步消息补偿机制 Checklist

> 适用范围：方案 A（独立补偿任务表 + partition worker + supervisor），覆盖齐为权益购买失败补偿、云卡用信提交超时补偿。

## 决策确认

- [x] 已确认采用方案 A
- [x] 已确认使用本地 MySQL 持久化补偿任务
- [x] 已确认每个 partition 一个独立常驻 worker 进程
- [x] 已确认 supervisor 只做监控 / 回收 / 告警，不直接发送
- [x] 已确认用信提交超时时，前端显示“已提交 / 审核中”
- [x] 已确认齐为权益购买同步失败时，前端仍按失败口径处理

## 表结构

- [x] 新增 `async_compensation_task`
- [x] 新增 `async_compensation_attempt`
- [x] 新增 `async_compensation_partition_runtime`
- [x] 补主库 `schema.sql`
- [x] 补 H2 `schema-h2.sql`
- [x] 补 migration SQL
- [x] 补唯一索引 `uk_task_type_biz_key`
- [x] 补分片查询索引 `idx_partition_status_next_retry`

## 基础模型

- [x] 新增任务状态枚举
- [x] 新增任务类型枚举
- [x] 新增目标系统枚举
- [x] 新增分片配置 `partitionCount`
- [x] 新增重试配置 `maxRetryCount`
- [x] 新增租约配置 `leaseSeconds`
- [x] 新增退避重试配置 `retryInitialDelaySeconds`
- [x] 新增退避上限配置 `retryMaxDelaySeconds`

## 超时识别

- [x] 补 `UpstreamTimeoutException`
- [x] 云卡客户端区分“超时”和“明确失败”
- [x] 齐为客户端区分“超时”和“明确失败”
- [x] 服务层按超时类型走不同返回口径

## 入队能力

- [x] 实现 `AsyncCompensationEnqueueService`
- [x] 落库时完整保存请求路径
- [x] 落库时完整保存请求参数
- [x] 按 `bizKey` 计算稳定分片
- [x] 保证 `task_type + biz_key` 幂等

## 齐为权益购买补偿

- [x] 齐为同步失败时入补偿任务
- [x] `benefit_order.sync_status` 正确落失败/待补偿状态
- [x] 控制器仍返回失败口径
- [x] 补 service 测试
- [ ] 补 controller/integration 测试

## 云卡用信提交补偿

- [x] 云卡调用超时时入补偿任务
- [x] 本地 `loan_application_mapping` 落 `PENDING_REVIEW` 或等价值
- [x] `/api/loan/apply` 返回 `pending`
- [x] 页面 message 返回“已提交 / 审核中”
- [x] 明确业务失败时仍保持失败口径
- [x] 补 Phase 9 集成测试

## Worker 执行链路

- [x] 实现 partition worker 拉取本分片任务
- [x] 实现任务领取与租约更新
- [x] 实现成功后置 `SUCCESS`
- [x] 实现失败后置 `RETRY_WAIT`
- [x] 实现超过阈值后置 `DEAD`
- [x] 记录每次执行到 `async_compensation_attempt`
- [x] 按任务类型路由到不同 executor
- [x] 实现指数退避 + 最大间隔封顶

## Supervisor 治理

- [x] 实现 worker heartbeat
- [x] 实现 expired lease recycle
- [x] 实现分片掉线检查
- [x] 实现积压检查
- [x] 实现死信数量检查

## 幂等与防重

- [x] 齐为补偿前先检查订单是否已同步成功
- [x] 用信补偿前先查本地 mapping / 上游查询结果
- [x] 用信补偿优先“先查后发”
- [x] 避免“同步失败 + 用户重试 + 异步补偿”三方并发重复执行

## 测试

- [x] `AsyncCompensationSchemaIntegrationTest`
- [x] `AsyncCompensationEnqueueServiceTest`
- [x] `AsyncCompensationWorkerServiceTest`
- [x] `AsyncCompensationSupervisorServiceTest`
- [x] `BenefitOrderServiceTest`
- [ ] `BenefitOrderControllerIntegrationTest`
- [x] `Phase9TaskGroupEIntegrationTest`
- [x] 聚焦组合测试跑通
- [ ] MySQL 回归无 schema 冲突

## 文档与运维

- [x] 更新方案文档最终状态
- [ ] 补人工重放说明
- [ ] 补人工取消说明
- [ ] 补死信处理流程
- [ ] 补分片部署说明
- [ ] 补 worker / supervisor 启停说明
