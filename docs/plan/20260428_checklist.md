# 2026-04-28 工作 Checklist

> 今日主线：完成双分支验证、主线合并、推送清理和文档回填，把本轮并行开发从“完成候选”收口为“真实交付态”。

## 状态总览

### 已完成前置条件

- [x] `feature/h5-api` 已形成功能提交
- [x] `feature/joint-login` 已形成功能提交
- [x] 已明确灰区 owner 与合并顺序

### 今日重点

- [x] 核对两个工作区的真实完成状态
- [x] 执行 H5/API 线分支级验证
- [x] 执行联合登录线分支级验证
- [x] 按顺序合并到 `main`
- [x] 在合并后的 `main` 上执行集成态验证
- [x] 推送 `main` 到远端
- [x] 清理 worktree / 本地功能分支
- [x] 回填昨日与今日文档

## P0：分支完成度核对

- [x] 核对 `feature/h5-api` 工作区状态
- [x] 核对 `feature/joint-login` 工作区状态
- [x] 核对灰区文件是否存在真实重叠修改
- [x] 确认唯一共享文件 `H5/client/src/i18n/messages.ts` 可自动合并

## P0：H5/API 线验证

- [x] 执行 `H5` 类型检查
- [x] 执行 H5/API 相关页面逻辑测试
- [x] 执行 `LoanServiceTest`
- [x] 执行 `Phase9TaskGroupCIntegrationTest`
- [x] 执行 `Phase9TaskGroupEIntegrationTest`
- [x] 执行 `Phase9TaskGroupECompensationIntegrationTest`

## P0：联合登录线验证

- [x] 执行 `H5` 类型检查
- [x] 执行联合登录相关前端测试
- [x] 执行 `JointLoginServiceTest`
- [x] 执行 `JointLoginControllerIntegrationTest`
- [x] 执行 `JwtAuthenticationFilterTest`
- [x] 执行 `XiaohuaGatewayServiceTest`
- [x] 执行 `NexusfinEquityApplicationTests`

## P0：主线合并与集成验证

- [x] 先合并 `feature/h5-api`
- [x] 再合并 `feature/joint-login`
- [x] 合并后执行 `H5` 类型检查
- [x] 合并后执行前端测试集
- [x] 合并后执行 Java 目标测试集

## P1：仓库清理

- [x] 推送 `main` 到 `origin/main`
- [x] 移除 `feature/h5-api` worktree
- [x] 移除 `feature/joint-login` worktree
- [x] 删除本地分支 `feature/h5-api`
- [x] 删除本地分支 `feature/joint-login`
- [x] 将 `.gstack/` 与 `CLAUDE.md` 加入本地 `git exclude`

## P1：文档补记

- [x] 补 `docs/日报/20260427.md`
- [x] 重写 `docs/decision-review/DAILY_DECISION_20260427.md`
- [x] 更新 `docs/plan/20260427_checklist.md`
- [x] 新增 `docs/plan/20260428.md`
- [x] 新增 `docs/plan/20260428_checklist.md`

## 收尾检查

- [x] `main` 与 `origin/main` 已同步
- [x] 仓库 `git status` 已恢复干净
- [x] 本轮并行开发的验证、合并、清理、补记已全部收口
