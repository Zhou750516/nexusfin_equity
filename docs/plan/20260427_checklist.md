# 2026-04-27 工作 Checklist

> 今日主线：继续承接 `2026-04-26` 的方案收口结果，把 H5 对接优先级与 `docs/plan` 新治理方式真正落成可执行入口。

## 状态总览

### 已完成前置产出

- [x] 产出 `docs/plan/20260427_H5前后端接口对接清单.md`
- [x] 新增 `docs/plan/CURRENT_STATE.md`
- [x] 新增 `scripts/archive_plan.sh`
- [x] 完成 H5 新入口页、路由调整与 mock server 首版接入

### 今日重点

- [x] 明确 H5 对接第一批实施顺序
- [x] 明确 `purpose` 字段为第一优先级最小切片的改动范围
- [ ] 补齐 `CURRENT_STATE.md` 的真实项目口径
- [ ] 核对 `docs/plan` 新归档规则是否可持续使用
- [ ] 明确 H5 本地联调与 mock 的固定入口

### 今日边界

- [ ] 不同时启动 `purpose`、权益短信、资方匹配、协议详情四条实现
- [ ] 不再次发起第二轮大规模 `docs/plan` 搬运
- [ ] 不把 `CURRENT_STATE.md` 写成新的日报替代品
- [ ] 不把 mock 便利项和真实主链改造混在同一轮推进

## P0：H5 前后端接口第一批实施顺序

- [x] 从 `docs/plan/20260427_H5前后端接口对接清单.md` 中确认第一优先级项
- [x] 明确 `purpose` 涉及的 FE / BE / DB / test 改动点
- [x] 明确 `purpose` 的接口验收标准与兼容策略
- [x] 明确“权益开通短信验证闭环”是否直接进入下一轮实现
- [x] 将 `matching-status`、协议详情、非主链 mock 补齐归入后续 backlog

## P0：`CURRENT_STATE.md` 收口

- [ ] 填掉“当前阶段”占位
- [ ] 填掉“本周聚焦 / 本周不做”占位
- [ ] 更新五方依赖状态表
- [ ] 更新阻塞项分类
- [ ] 回填本周核心决定

## P1：`docs/plan` 新治理方式核对

- [ ] 运行 `scripts/archive_plan.sh` dry-run 看拟归档清单
- [ ] 确认当日计划 / checklist / 活跃专题继续留在根目录
- [ ] 确认历史专题优先进入 `topics/`
- [ ] 确认过期日计划优先进入 `archive/`
- [ ] 记录一版后续维护规则

## P1：H5 本地联调边界

- [ ] 确认 `/landing` → `/calculator` 为当前默认入口
- [ ] 确认 `pnpm dev` + `pnpm dev:mock` 的联调方式
- [ ] 评估是否补齐联合登录 / 分发 / 退款的 mock 路由
- [ ] 确认 mock 补齐项不与真实接口实现混做

## 收尾检查

- [x] 若第一批实施顺序已定，回写 `docs/plan/20260427_H5前后端接口对接清单.md`
- [ ] 若 `CURRENT_STATE.md` 已收口，后续开工优先从该文件进入
- [ ] 若形成关键决策，补 `docs/decision-review/DAILY_DECISION_20260427.md`
- [ ] 若当天完成明显推进，次日补 `docs/日报/20260427.md`
