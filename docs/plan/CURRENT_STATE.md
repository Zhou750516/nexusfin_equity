# 当前项目状态 · 单点真相

> 这是项目的当前口径入口。开工先看这里，再看当天 plan 和 checklist。

**最后更新**：2026-04-29
**维护人**：协调 / 审查会话
**下次复审**：2026-05-03

---

## 1. 主线状态

- **当前阶段**：重构降摩擦主线已完成 Phase 0、1A、1B、2A、2B、2C、2D、2E、5、3、4；当前进入结果收口与缺陷清单阶段。
- **本周聚焦**：
  1. 维护 `docs/plan` 与 AGENTS 入口的真实状态，避免回填失真
  2. 保持收敛后的测试 context 与 H5 结构，不回退到高摩擦形态
  3. 后续新需求优先沿已拆出的 service / gateway / H5 section 边界扩展
- **本周不做**：
  - 不重做已完成的重构阶段
  - 不在存在产品歧义的情况下擅自删除 H5 路由或页面

## 2. 重构主线状态

| Phase | 状态 | 当前结论 |
|------|------|---------|
| **Phase 0 认知半径瘦身** | ✅ | `docs/plan`、AGENTS 入口与 archive 结构已收口 |
| **Phase 1A shared utils** | ✅ | 已完成，不再重做 |
| **Phase 1B yunka call template** | ✅ | 已完成，不再重做 |
| **Phase 2A loan query split** | ✅ | 已完成，不再重做 |
| **Phase 2B loan calculator split** | ✅ | 已完成，不再重做 |
| **Phase 2C loan application split** | ✅ | 已完成，不再重做 |
| **Phase 2D loan application gateway** | ✅ | 已完成，不再重做 |
| **Phase 2E async compensation typed payload** | ✅ | 已完成，不再重做 |
| **Phase 5 test context convergence** | ✅ | `@SpringBootTest` 已从 18 收敛到 15，`mvn test` wall-clock 从 14.544s 降到 13.582s |
| **Phase 3 QW convergence** | ✅ | Allinpay request flow 与 `QwProperties` 已收敛，`Skeleton*` 已清空 |
| **Phase 4 H5 decomposition** | ✅ | `CalculatorPage`、i18n catalog 与 `components/ui` 已收敛，路由边界保留 |

## 3. 当前阻塞与边界

### 3.0 当前真实阻塞

| 阻塞项 | 状态 | 影响范围 |
|------|------|---------|
| `MySqlAsyncCompensationIntegrationTest` 启动失败 | 🔴 未收口 | MySQL IT / QW Spring 装配 |

当前已确认的直接原因：

- `AllinpayDirectQwBenefitClient` 存在两个带 `@Autowired` 标记的构造器
- Spring 在 MySQL IT context 中创建 `routingQwBenefitClient` 依赖链时失败
- 因此当前可以认为：
  - 单测与 H5 类型检查通过
  - 可选 MySQL IT 未通过，尚不能宣称“所有验证闭环已完成”

### 3.1 无外部阻塞的工作

| 事项 | 当前策略 | 影响范围 |
|------|---------|---------|
| 已完成 phase 的日常维护 | 保持当前边界，不回退到高摩擦结构 | Java / H5 / docs |
| MySQL IT 修复前的常规开发 | 可继续基于单测与 H5 基线推进 | Java / H5 |

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
- ✅ 2026-04-28：测试 context 已完成第一轮收敛，controller-only 场景已切到更薄的 standalone MockMvc
- ✅ 2026-04-28：QW / Allinpay direct 收敛完成，`QwProperties.java` 已从 359 行降到 81 行
- ✅ 2026-04-29：H5 `CalculatorPage.tsx` 已拆到 161 行，`messages.ts` 已拆为按域目录，`components/ui` 已收敛到 8 个保留文件
- ⚠️ 2026-04-29：补跑 `MySqlAsyncCompensationIntegrationTest` 时发现 QW Spring 装配回归，尚待修复后再宣称验证全绿

## 5. 当前入口导航

- `docs/plan/README.md`：根入口规则
- `docs/plan/20260429.md`：当天计划
- `docs/plan/20260428.md`：上一日已执行完成的计划记录
- `docs/plan/20260428_checklist.md`：上一日 checklist
- `docs/plan/20260427_重构降摩擦.md`：重构总方案
- `docs/plan/topics/重构降摩擦/20260428_phase0_baseline.md`：Phase 0 基线与结果快照
- `docs/test-performance-analysis.md`：Phase 5 测试收敛参考

## 6. 当前验证基线

- Java 基线命令：`mvn test`
- H5 基线命令：`cd H5 && ./node_modules/.bin/tsc --noEmit`
- 当前已确认：
  - `mvn test`：通过，`207 tests`
  - `cd H5 && ./node_modules/.bin/tsc --noEmit`：通过
  - `MYSQL_IT_ENABLED=true MYSQL_IT_DATABASE=nexusfin_equity mvn -Dtest=MySqlAsyncCompensationIntegrationTest test`：失败
- MySQL IT 当前失败原因：
  - `AllinpayDirectQwBenefitClient` duplicated `@Autowired` constructors
  - 这是收口后新暴露的集成缺陷，不影响“已完成 phase 的结构结论”，但影响“可选 IT 全绿”结论

## 7. 维护规则

1. 只保留真实当前状态，不留占位符。
2. 每完成一个 phase，至少更新一次本文件的“当前阶段”和“已收口事项”。
3. 日常执行细节写在当天 plan，不塞进本文件。
