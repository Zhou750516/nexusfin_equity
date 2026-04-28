# 2026-04-28 工作 Checklist

> 今日主线：先完成双分支收口，再把 `20260427_重构降摩擦.md` 推进到可执行的 `Phase 1A ~ 2E`，并形成后续阶段的统一执行指令。

## 状态总览

### 已完成前置条件

- [x] `feature/h5-api` 与 `feature/joint-login` 已分别完成主实现
- [x] 双分支灰区 owner 与合并顺序已在前一天明确
- [x] `20260427_重构降摩擦.md` 已成为当天后续主线的唯一重构总纲

### 今日重点

- [x] 完成双分支验证、合并、清理和文档回填
- [x] 补齐 / 修订 `Phase 1A ~ 2E` 的可执行计划
- [x] 通过独立主实现会话完成 `Phase 1A ~ 2E`
- [x] 在协调会话中逐阶段复核 commit 边界与验证结果
- [x] 形成剩余阶段 `Phase 0 / 5 / 3 / 4` 的统一执行总指令

## P0：双分支收口

- [x] 核对 `feature/h5-api` 工作区状态
- [x] 核对 `feature/joint-login` 工作区状态
- [x] 核对灰区文件是否存在真实重叠修改
- [x] 先合并 `feature/h5-api`
- [x] 再合并 `feature/joint-login`
- [x] 执行合并后的集成态验证
- [x] 推送 `main` 到 `origin/main`
- [x] 清理 worktree / 本地分支

## P0：重构计划落地

- [x] 补 `2026-04-28-phase-1a-shared-utils-extraction.md`
- [x] 补 `2026-04-28-phase-1b-yunka-call-template.md`
- [x] 补 `2026-04-28-phase-2a-loan-query-split.md`
- [x] 修订 `2026-04-28-phase-2b-loan-calculator-split.md`
- [x] 补 `2026-04-28-phase-2c-loan-application-split.md`
- [x] 补 `2026-04-28-phase-2d-loan-application-gateway-extraction.md`
- [x] 补 `2026-04-28-phase-2e-async-compensation-typed-payload.md`

## P0：Phase 1 / 2 执行收口

- [x] 完成 `Phase 1A` shared utils 抽取与验证
- [x] 完成 `Phase 1B` Yunka call template 落地与迁移
- [x] 完成 `Phase 2A` loan query split
- [x] 完成 `Phase 2B` loan calculator split
- [x] 完成 `Phase 2C` loan application split
- [x] 完成 `Phase 2D` loan application gateway extraction
- [x] 完成 `Phase 2E` async compensation typed payload

## P1：验证与审查

- [x] 对各阶段关键 commit 执行边界审查
- [x] 对各阶段 focused tests 结果做独立复核
- [x] 对 `Phase 2E` 执行 executor compatibility tests
- [x] 对 `Phase 2E` 执行 combined focused regression
- [x] 执行 `mvn -q -DskipTests compile` 验证关键阶段编译通过
- [x] 通过 `rg` 检查旧 helper、repo 直依赖和手写 JSON 模板是否按阶段移除

## P1：文档补记

- [x] 补 `docs/日报/20260428.md`
- [x] 补 `docs/decision-review/DAILY_DECISION_20260428.md`
- [x] 更新 `docs/plan/20260428_checklist.md`

## 后续待办

- [x] 执行 `Phase 0` 认知半径瘦身
- [x] 执行 `Phase 5` 测试 context 收敛
- [x] 执行 `Phase 3` `thirdparty/qw/` 收敛
- [x] 执行 `Phase 4` H5 页面解构
- [x] 回填 `CURRENT_STATE.md` 的占位状态到真实口径
- [x] 事后补跑 `MySqlAsyncCompensationIntegrationTest`

## 收尾检查

- [x] 双分支收口已完成
- [x] `Phase 1A ~ 2E` 已完成计划、实现与验证闭环
- [x] 后续阶段已执行并收口
- [x] “重构降摩擦”全量阶段已完成
- [ ] 可选 MySQL IT 尚未恢复为全绿，当前存在待修 QW Spring 装配回归
