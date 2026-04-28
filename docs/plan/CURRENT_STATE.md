# 当前项目状态 · 单点真相

> 这是项目的当前口径入口。开工先看这里，再看当天 plan 和 checklist。

**最后更新**：2026-04-28
**维护人**：Codex 主实现会话
**下次复审**：2026-05-03

---

## 1. 主线状态

- **当前阶段**：重构降摩擦主线已完成 Phase 1A、1B、2A、2B、2C、2D、2E，当前待推进 Phase 0、Phase 5、Phase 3、Phase 4。
- **本周聚焦**：
  1. 收口 `docs/plan` 与 AGENTS 入口，降低会话入场成本
  2. 收敛测试 context 风味，降低 `mvn test` 验证成本
  3. 继续 `thirdparty/qw` 与 H5 大文件拆分
- **本周不做**：
  - 不重做已完成的 Phase 1A / 1B / 2A / 2B / 2C / 2D / 2E
  - 不在存在产品歧义的情况下擅自删除 H5 路由或页面

## 2. 重构主线状态

| Phase | 状态 | 当前结论 |
|------|------|---------|
| **Phase 0 认知半径瘦身** | 🟡 | 结构已基本收口，需保持入口文档真实有效 |
| **Phase 1A shared utils** | ✅ | 已完成，不再重做 |
| **Phase 1B yunka call template** | ✅ | 已完成，不再重做 |
| **Phase 2A loan query split** | ✅ | 已完成，不再重做 |
| **Phase 2B loan calculator split** | ✅ | 已完成，不再重做 |
| **Phase 2C loan application split** | ✅ | 已完成，不再重做 |
| **Phase 2D loan application gateway** | ✅ | 已完成，不再重做 |
| **Phase 2E async compensation typed payload** | ✅ | 已完成，不再重做 |
| **Phase 5 test context convergence** | ⏳ | 下一优先级，需要先测 baseline 再收敛 |
| **Phase 3 QW convergence** | ⏳ | 待 Phase 5 后推进 |
| **Phase 4 H5 decomposition** | ⏳ | 待 Phase 3 / Phase 5 后推进 |

## 3. 当前阻塞与边界

### 3.1 无外部阻塞的工作

| 事项 | 当前策略 | 影响范围 |
|------|---------|---------|
| Phase 0 入口瘦身 | 优先做最小必要修正，不重复搬运已归档内容 | docs / AGENTS |
| Phase 5 测试收敛 | 先做 baseline，再按 slice 和 context 家族拆解 | Java 测试体系 |
| Phase 3 QW 收敛 | 保持对外接口与业务语义不变 | `thirdparty/qw` |
| Phase 4 H5 解构 | 保留现有页面行为，不抢跑产品裁剪 | `H5/` |

### 3.2 需要保守处理的边界

| 边界 | 当前处理原则 |
|------|-------------|
| H5 路由删除 | 有歧义时保留，不为了彻底而擅删 |
| 测试并行参数 | 只引入稳定配置，不制造随机失败 |
| QW Skeleton* 删除 | 先做引用分析，无运行时理由才删 |

## 4. 已收口事项

- ✅ 2026-04-28：`docs/plan` 根目录已收敛到 5 个入口文件
- ✅ 2026-04-28：根 `AGENTS.md` 已收敛为轻入口，后端 / H5 细则已拆到 `docs/AGENTS_BACKEND.md`、`docs/AGENTS_H5.md`
- ✅ 2026-04-28：`specs/004-benefit-loan-flow/` 已迁出主入口扫描面
- ✅ 2026-04-28：`src/main/java/com/nexusfin/equity/thirdparty/qw/demo/` 已移出运行时源码目录
- ✅ 2026-04-28：Loan 查询 / 计算 / 申请 / gateway / async compensation typed payload 重构已完成

## 5. 当前入口导航

- `docs/plan/README.md`：根入口规则
- `docs/plan/20260428.md`：当天计划
- `docs/plan/20260428_checklist.md`：当天 checklist
- `docs/plan/20260427_重构降摩擦.md`：重构总方案
- `docs/plan/topics/重构降摩擦/20260428_phase0_baseline.md`：Phase 0 基线与结果快照
- `docs/test-performance-analysis.md`：Phase 5 测试收敛参考

## 6. 当前验证基线

- Java 基线命令：`mvn test`
- H5 基线命令：`cd H5 && ./node_modules/.bin/tsc --noEmit`
- Phase 5 开始前必须补测：
  - `@SpringBootTest` 数量
  - context 家族数
  - `mvn test` wall-clock

## 7. 维护规则

1. 只保留真实当前状态，不留占位符。
2. 每完成一个 phase，至少更新一次本文件的“当前阶段”和“已收口事项”。
3. 日常执行细节写在当天 plan，不塞进本文件。
