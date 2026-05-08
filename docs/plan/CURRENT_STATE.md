# 当前项目状态 · 单点真相

> 这是项目的当前口径入口。开工先看这里，再看当天 plan 和 checklist。

**最后更新**：2026-05-01
**维护人**：协调 / 审查会话
**下次复审**：2026-05-03

---

## 1. 主线状态

- **当前阶段**：重构降摩擦主线已完成；当前处于“本地联调主链稳定 + 联调前硬化收尾 + 外部阻塞隔离”阶段。
- **本周聚焦**：
  1. 保持 Yunka local stub + QW MOCK + tech-user stub 的唯一联调基线
  2. 保持 `joint-login / benefit redirect / benefiturl / loan / repayment` 夜间回归结论稳定
  3. 通过 stub 契约测试和前端小型收尾，降低 2026-05-07 对外联调前回归风险
  4. 将云卡公网问题和电子签章待定项继续与内部异常线隔离管理
- **本周不做**：
  - 不回滚已完成的重构阶段
  - 不把公网云卡阻塞与本地 mock 结论混用

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
| **Phase 3 QW convergence** | ✅ | Allinpay request flow 与 `QwProperties` 已收敛，`Skeleton*` 已收敛为占位实现并保留当前注入边界 |
| **Phase 4 H5 decomposition** | ✅ | `CalculatorPage`、i18n catalog 与 `components/ui` 已收敛，路由边界保留 |

## 3. 当前阻塞与边界

### 3.0 当前真实阻塞

当前无重构主线阻塞，但有 4 条独立边界待继续处理：

1. callback P0 线已闭合：
   - `B-P0-3.1 ~ B-P0-3.4` 已形成运行时证据
   - 坏签名拒绝且无副作用已验证
2. joint-login / benefit redirect 接口级能力已通过运行态验证：
   - `push` 不再依赖 `benefitOrderNo`
   - `exercise / refund` 缺单号会受控失败
   - `POST /api/auth/redrect_benefit_url` 已可正常返回 URL，timeout / reject 不落通用 500
3. 当前未闭合项：
   - `benefiturl` 的字段语义仍偏向当前 QW exercise redirect runtime source，不能自动泛化为“所有权益场景通用联登 URL”
   - H5 仍有低优先级浏览器 issue：部分表单控件需继续补 `id/name`
4. 云卡真实联调仍存在外部阻塞：
   - real Yunka 已可真实接收 ABS 请求
   - 当前真实阻塞为 `KJ_NOT_READY / kj.private-key 配置无效`
   - 电子签章接入方案也尚未定稿

`2026-04-29 ~ 2026-04-30` 已确认：

- `32722e7` 已修复 QW Spring 构造器装配回归
- `1abcf4a` 已修复 clean 用户 `QW_SIGN` mock 协议号唯一性问题
- `896c4c2` 已补第二个隔离 clean 用户 `mock-tech-token-clean-2`
- callback P0 线已闭合
- `794de0e` 已将 ABS -> Yunka 协议对齐到 `2026-04-10` 文档并补齐完整 JSON 出站日志
- `e768257` / `edbb7f2` 已完成 joint-login 场景拆分、`redrect_benefit_url` 能力和本地 stub 收口
- `b4b0fd5` 已将 `benefiturl` 接到 `BenefitsServiceImpl.activate -> XiaohuaGatewayService.syncBenefitOrder` 主链，并强制要求 `activate` 请求携带 token
- `2026-05-01` 夜间自动化回归 Round 12 已确认：
  - `joint-login`、`redrect_benefit_url`、`loan/calculate`、`benefits/card-detail`、`benefits/activate`、`repayment` 代表性链路本地稳定
  - invalid token 运行态语义已闭环
  - `benefiturl` 主链运行态实发证据已拿到
- `4a8e5b9` 已完成联调前硬化：
  - Yunka stub 契约测试防漂移
  - `benefiturl` 语义注释 / 日志硬化
  - H5 关键表单 `id/name` 补齐第一轮收口

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
- ✅ 2026-04-29：`32722e7` 已修复 QW Spring 构造器装配回归，`MySqlAsyncCompensationIntegrationTest` 恢复通过

## 5. 当前入口导航

- `docs/plan/README.md`：根入口规则
- `docs/plan/20260501.md`：当天计划
- `docs/plan/20260501_checklist.md`：当天 checklist
- `docs/plan/20260429.md`：上一日计划记录
- `docs/plan/20260427_重构降摩擦.md`：重构总方案
- `docs/plan/topics/重构降摩擦/20260428_phase0_baseline.md`：Phase 0 基线与结果快照
- `docs/test-performance-analysis.md`：Phase 5 测试收敛参考

## 6. 当前验证基线

- Java 基线命令：`mvn test`
- H5 测试命令：`cd H5 && pnpm test`
- H5 基线命令：`cd H5 && ./node_modules/.bin/tsc --noEmit`
- 当前已确认：
  - `mvn test`：通过，`213 tests`
  - `cd H5 && pnpm test`：通过，`15 files / 62 tests`
  - `cd H5 && ./node_modules/.bin/tsc --noEmit`：通过
  - `MYSQL_IT_ENABLED=true MYSQL_IT_DATABASE=nexusfin_equity mvn -Dtest=MySqlAsyncCompensationIntegrationTest test`：通过

## 7. 维护规则

1. 只保留真实当前状态，不留占位符。
2. 每完成一个 phase，至少更新一次本文件的“当前阶段”和“已收口事项”。
3. 日常执行细节写在当天 plan，不塞进本文件。
