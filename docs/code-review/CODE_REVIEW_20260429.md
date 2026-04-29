# CODE REVIEW · 2026-04-29 重构后整体复核

## 处理状态（2026-04-29 收口）

- 状态：已处理。本文保留为历史复核记录，不再作为当前待办清单。
- 当前结论：本轮 review 提出的有效问题已经完成收口，最新基线已恢复为全绿。
- 已完成项：
  - `§1.1` 已由 `32722e7` 修复，`AllinpayDirectQwBenefitClient` 现仅保留单一 Spring 注入入口，并补了装配回归测试。
  - `§1.2` 已由 `10a5adf` 修复，`LoanApplicationServiceImpl.apply(...)` 的重复 warn 已删除，由 `YunkaCallTemplate` 单点输出。
  - `§1.3` 已由 `ad9d76e` 修复，H5 关键直接渲染 i18n key 已补齐，并补了 locale 完整性测试。
  - `§2.1` / `§2.2` 已由 `38eed7c` 完成，公共 `JsonNodes` / `LoanInputValidator` 已抽出，死代码重载已删除。
  - `§2.4` 已由 `8dfab20` 完成，`QwBenefitClientImpl` 已改为复用构造期创建的 `requestFactory` / `restClient`。
  - `§2.5` 已由 `5993b56` 完成，`useCalculatorPageState` surface 已收窄，页面无用 import 已删除。
  - `§2.6` / `§3.C` 已由 `6bad999` 完成，H5 `test` / `test:watch` scripts 与 README 验证命令已补齐。
  - `§2.3` 已由 `a0b6b41` 处理，采用最小方案对齐文档与测试口径，不改 `Skeleton*` 类名。
  - `§3.A` 已由 `d05ea2e`、`9bebf8d` 回写，计划入口文档已与真实状态对齐。
- 最新验证基线：
  - `mvn test`：`213 tests, 0 failures, 0 errors`
  - `mvn -q -DskipTests compile`：通过
  - `cd H5 && pnpm test`：`15 files / 62 tests` 通过
  - `cd H5 && ./node_modules/.bin/tsc --noEmit`：通过
  - `MYSQL_IT_ENABLED=true MYSQL_IT_DATABASE=nexusfin_equity mvn -Dtest=MySqlAsyncCompensationIntegrationTest test`：`3 tests, 0 failures`
- 非本 review 范围：
  - 当前工作区仍有 `.gitignore` 本地既有修改，该项与本次 review 收口无关，保持不动。

> 入口语境：`docs/plan/CURRENT_STATE.md` 标注重构主线已完成 Phase 0 / 1A / 1B / 2A / 2B / 2C / 2D / 2E / 5 / 3 / 4，但仍把可选 MySQL IT 列为未收口的阻塞。本次 review 重新跑了一遍验证基线、对照核心文件做了走查，目的是把"重构是否真的落到了代码层"和"还剩哪些手尾"讲清楚。

---

## 0. 当时复核基线与后续收口结果

| 命令 | 结果 |
|------|------|
| `mvn test` | ✅ 已更新为 213 tests, 0 failures |
| `cd H5 && ./node_modules/.bin/tsc --noEmit` | ✅ exit 0 |
| `cd H5 && pnpm test` | ✅ 已更新为 15 files / 62 tests pass |
| `MYSQL_IT_ENABLED=true MYSQL_IT_DATABASE=nexusfin_equity mvn -Dtest=MySqlAsyncCompensationIntegrationTest test` | ✅ 3 tests, 0 failures |

结论：本机所有基线全绿；当时识别出的入口文档口径偏差，后续已按 `§3.A` 完成回写。

---

## 1. 高优先级问题

### 1.1 [Java] `AllinpayDirectQwBenefitClient` 双 `@Autowired` 构造器 + 重复延迟初始化

文件：`src/main/java/com/nexusfin/equity/thirdparty/qw/AllinpayDirectQwBenefitClient.java:35-97`

现状：
- 第一个 9 参构造器和第二个 8 参构造器都标了 `@Autowired`，第二个 `this(...)` 调第一个并把 `requestBuilder` 传 `null`。
- `requestBuilder` / `responseVerifier` / `restClient` 既是 final 注入字段（构造器赋值）、又是 `volatile` + `loadMaterialsIfNecessary()` 里的运行时再赋值；同一字段两条写入路径，意图不清。
- 字段上还有 `@SuppressWarnings("unused")`（line 30, 32）但事实上 `loadMaterialsIfNecessary` 还会 `synchronized(this)` 写它们，等于"压抑警告 + 留下半完成代码"两件事并存。
- `CURRENT_STATE.md §3.0 / §6 / §7` 把这件事记成"导致 `MySqlAsyncCompensationIntegrationTest` 启动失败"。本机实测 IT 全部跑通、Spring 上下文正常装配（也没有任何 `Invalid autowire-marked constructor` 异常）。文档当前结论是错的。

风险：
- Spring 6 在多 `@Autowired` 构造器场景的容忍度并不稳定，迁移版本或换 `BeanDefinitionRegistry` 顺序就可能爆 `BeanCreationException`。
- 未来任何人新增第 4 个构造器，会很容易踩 "我多加了一个 @Autowired，启动炸了"。

建议（从最小到彻底）：
1. 立刻删掉第二个 `@Autowired` 构造器，只保留 9 参那一个；第三个 3 参重载若仍要保留给单元测试，则改为 `package-private` 并显式不带 `@Autowired`。
2. 把 `loadMaterialsIfNecessary` 收窄到"只 lazy-load `merchantKeyStore` / `verifyCertificate`"；`requestBuilder` / `responseVerifier` / `restClient` 让 `AllinpayDirectConfiguration` 走 `@Bean` 装配，构造器一次性收齐。
3. 字段上的 `@SuppressWarnings("unused")` 去掉；`responseVerifier` / `restClient` 真没被读，就直接删掉字段。

### 1.2 [Java] `LoanApplicationServiceImpl.apply` 与 `YunkaCallTemplate` 重复打 warn

文件：
- `src/main/java/com/nexusfin/equity/service/impl/LoanApplicationServiceImpl.java:127-209`
- `src/main/java/com/nexusfin/equity/service/support/YunkaCallTemplate.java:62-106`

现状：`YunkaCallTemplate.execute` 已经在 `BizException` / `UpstreamTimeoutException` / `RuntimeException` 三个 catch 里完整打了 `traceId/bizOrderNo/requestId/memberId/benefitOrderNo/path/scene/elapsedMs/errorNo/errorMsg`；`LoanApplicationServiceImpl.apply` 又在外层重新 catch 一次、再用一段 `log.warn` 把基本相同的字段（差一个 `scene`）打一遍。

风险：
- 同一笔失败 traceId 一致但出现两条 warn，字段还略不一样，报警/日志聚合容易计两次或互相覆盖；这正是抽 `YunkaCallTemplate` 想消掉的散乱日志。
- 行数本身也回升：apply 方法从 127 行到 209 行有 80 多行其实是被三段重复的 `log.warn` 撑起来的。

建议：
- 外层 catch 只保留必要的业务分流（`UpstreamTimeoutException` → `loanApplicationGateway.save(... PENDING_REVIEW ...)` + `asyncCompensationEnqueueService.enqueue(...)` + 返回 pending response；`BizException` / `RuntimeException` → `buildLoanFailedResponse`）。
- 删掉外层 3 段 `log.warn`；如果确实要补 `bizOrderNo / benefitOrderNo` 这种 yunka template 不知道的字段，就把它们经 `YunkaCall.with*` 灌进 template，由 template 单点输出。
- 收益：`LoanApplicationServiceImpl.apply` 主体能从 ~140 行回缩到 ~60 行，结构更接近 `LoanCalculatorServiceImpl.calculate`。

### 1.3 [H5] 非中文 locale 大量缺 key（用户可见回归）

文件：`H5/client/src/i18n/messages/calculator.ts`、`H5/client/src/i18n/messages/approval.ts`

把 zh-CN 的 key 集合和 zh-TW / en-US / vi-VN 对一遍：

| key | zh-CN | zh-TW | en-US | vi-VN |
|------|------|------|------|------|
| `calculator.protocol.loanContract / privacyAuth / userService / privacy / payment` | ✅ | ❌ | ❌ | ❌ |
| `calculator.protocolDrawerTitle / protocolImportant / protocolImportantBody / protocolAgreeButton` | ✅ | ❌ | ❌ | ❌ |
| `calculator.tipsTitle / tip1Prefix / tip1Highlight / tip1Suffix / tip2 / tip3` | ✅ | ❌ | ❌ | ❌ |
| `calculator.partnersTitle / partnersDescription / partnersFootnote / partnersAck` | ✅ | ❌ | ❌ | ❌ |
| `calculator.annualRateMethod / loanProtocol / loanProtocolView / loanPurposeTitle` | ✅ | 部分缺 | 部分缺 | 部分缺 |
| `approvalPending.dismiss.* / confirm.* / matching.*` | ✅ | ❌ | ❌ | ❌ |
| `approvalResult.amountUnit / tip1 / tip2 / tip3 / tip4 / tipTitle 部分` | ✅ | 部分缺 | 部分缺 | 部分缺 |

`CalculatorProtocolDrawer.tsx` 直接 `t(key)` 渲染 PROTOCOL_KEYS 里的常量；切语言后 UI 上会显示 `calculator.protocol.loanContract` 这种 raw key 字符串。

建议：
1. 短期：把上面 PROTOCOL_KEYS / LOAN_PURPOSE_KEYS / `protocolDrawerTitle` / `protocolImportantBody` / `protocolAgreeButton` 这一批"会被组件直接 `t()` 的"key 在四个 locale 全部补齐——这条是用户可见 bug，应进入下一轮迭代。
2. 中期：在 `H5/client/src/i18n/messages/index.ts` 顶部加一段开发期校验（`if (process.env.NODE_ENV !== "production")` 时对比 `Object.keys` 差集，缺的 console.error 出来），或者写一个 vitest 锁定 `PROTOCOL_KEYS / LOAN_PURPOSE_KEYS / approvalPending.confirm.*` 在四种 locale 都存在。

---

## 2. 中优先级问题

### 2.1 [Java] 跨服务的工具/校验方法重复

| 重复项 | 出现位置 |
|------|------|
| `validateAmountAndTerm` | `LoanApplicationServiceImpl:239-252` 与 `LoanCalculatorServiceImpl:98-111` 一字不差 |
| `readText(JsonNode, fieldName, fallback)` | `LoanApplicationServiceImpl:293`、`RepaymentServiceImpl:348`、`LoanApprovalQueryServiceImpl:262` |
| `readLong` / `readRemark` 系列 | `RepaymentServiceImpl` / `LoanApplicationServiceImpl` 分别带一套 |
| `lastFour` | `BankCardSignServiceImpl:212`、`RepaymentServiceImpl:374`、`QwBenefitClientImpl:336` |
| `defaultText / defaultInt / defaultLong` | `RepaymentServiceImpl`、`LoanApprovalQueryServiceImpl` |

建议：抽两个无状态工具类
- `com.nexusfin.equity.util.JsonNodes`（`readText/readLong/readRemark`）。
- `com.nexusfin.equity.util.LoanInputValidator`（amount/term 校验，注入 `H5LoanProperties`）。

收益是 4 个 service 类各自能再瘦 10-30 行，且未来调整校验只动一处。

### 2.2 [Java] `LoanApplicationServiceImpl.readRemark(JsonNode data)` 单参重载是死代码

`src/main/java/com/nexusfin/equity/service/impl/LoanApplicationServiceImpl.java:266-268`

只被自己（同名两参版本的 fallback 默认值）引用，外部调用者只用两参版本。属于 surgical 重构残留，删掉。

### 2.3 [Java] `Skeleton*` 命名与实际行为不一致

- `service/impl/SkeletonRefundClient.java` 实际返回了完整的 `RefundApplyResponse / RefundResultResponse` 占位值。
- `service/impl/SkeletonTechPlatformBenefitStatusClient.java` 在 mode = MOCK 时静默通过，否则抛 `TECH_PLATFORM_STATUS_PUSH_NOT_READY`。

`CURRENT_STATE.md §2` 写的是"Phase 3 ... `Skeleton*` 已清空"。从代码看显然没清空，类还以 `@Service` 注册参与到所有 `RefundService`/`BenefitStatusPushService` 的装配里。

建议二选一：
- 改名（`MockRefundClient` / `DisabledTechPlatformBenefitStatusClient`），并把状态推送的 client 加上 `@ConditionalOnProperty(name="nexusfin.third-party.tech-platform.mode", havingValue="MOCK")`，让命名和行为一致。
- 或者把 `CURRENT_STATE.md` 里"已清空"改成"已收敛为占位实现，待真实下游接入后替换"。

### 2.4 [Java] `QwBenefitClientImpl.invoke` 每次都重建 `RestClient`

`src/main/java/com/nexusfin/equity/thirdparty/qw/QwBenefitClientImpl.java:113-115` 每次 `invoke` 都 `RestClient.builder().requestFactory(requestFactory()).build()`，`requestFactory()` 自身也 `new SimpleClientHttpRequestFactory()`。

风险：
- 这是 RoutingQwBenefitClient 在 `MOCK / HTTP / QWEIMOBILE_HTTP` 三种模式的兜底 client，目前线上未必走，但仍是热路径写法。
- 同一类问题在 `AllinpayDirectQwBenefitClient` 已经通过 `restClientFactory` 单例化，这边没对齐。

建议：构造期一次性 `restClient = RestClient.builder()....build()`、`requestFactory` 也只造一次；`invoke` 直接复用。

### 2.5 [H5] `useCalculatorPageState` 公共 surface 偏宽

`H5/client/src/pages/useCalculatorPageState.ts:165-225`、`H5/client/src/pages/CalculatorPage.tsx:15-56`

- 返回值里 `setViewedProtocols`（line 198）、`setPurposeKey`（line 200）没有任何调用方使用——它们已经被内部 `viewProtocol` / `selectPurpose` 闭包封装。
- `CalculatorPage.tsx:10` 还 import 了 `type ProtocolKey`，但文件里没出现一次。

建议：
- 删掉返回值里上面两个 setter；删掉 `CalculatorPage.tsx` 多余的 type import。
- 顺手把 hook 返回值拆成 `{ state, actions }` 两个对象，让 `CalculatorPage` 解构成两段，可读性更高，且未来再加 setter 不用每次同步两处。

### 2.6 [H5] vitest 没有 npm script 入口

`H5/package.json` 只有 `dev / dev:mock / build / start / preview / check / format`，`vitest` 是直接装的 dev dep；现在 60 个 `*.logic.test.ts` / `*.test.ts` 只能 `./node_modules/.bin/vitest run` 手敲。

建议在 scripts 里补：

```json
"test": "vitest run",
"test:watch": "vitest"
```

并把 `pnpm test` 写进根 `README.md` 的 "Build And Verify" 段，跟 `mvn test` 平级。

---

## 3. 低优先级 / 文档收口

### 3.A `CURRENT_STATE.md` / `20260429.md` 与代码现状对不上

该项已处理，历史要求如下，现已完成回写：
- `§3.0 当前真实阻塞` 的"`MySqlAsyncCompensationIntegrationTest` 启动失败"已不复现，至少要改成"曾经怀疑 `AllinpayDirectQwBenefitClient` 双 `@Autowired` 会阻断装配，目前 IT 已能通过，但代码本身仍需收口"。
- `§6 当前验证基线` 的 `MYSQL_IT_ENABLED=...` 改成 ✅。
- `§6` 里"`mvn test`：通过，207 tests" → 实测 208 tests，对齐数字。
- `20260429.md §0 当前已知新增问题` 同步更新；不要让"双 @Autowired 阻塞 IT" 这个错误结论留在入口文档里、误导后续会话再去回滚已完成的重构。

### 3.B 重构后的目录边界仍可压缩

| 目录 | 现状 | 备注 |
|------|------|------|
| `service/impl` | 35 个文件、`Skeleton*` × 2、`AsyncCompensation*` × 4 | 异步补偿 4 个 impl 都 < 230 行，能保持。 |
| `thirdparty/qw` | 45 个文件，且大量 `AllinpayDirect*` 单文件 < 30 行 | 重构后粒度偏碎，可以挑 `Mapper` 系列（4 个 PayloadMapper）合到一个 `package-info` + `final class AllinpayPayloadMappers` 静态工厂里。这条不紧急，仅记下。 |
| `H5/client/src/components/calculator` | 12 个文件 | 已经合理，保持。 |
| `H5/client/src/i18n/messages` | 6 个 catalog 文件 | 拆分思路对，缺一个完整性校验（见 §1.3）。 |

### 3.C README "Build And Verify" 段落

当前 README 的命令列表有 `mvn test / mvn -Dtest=MySql... / mvn clean package / mvn checkstyle:check`，缺：
- H5 `tsc --noEmit`
- H5 `vitest run`

该项已处理：已补 `H5/package.json` `test` / `test:watch` script，并将 README 验证命令与当前基线对齐。

---

## 4. 推荐的下一轮收口顺序

1. `§3.A` 文档纠偏。
2. `§1.1` 删第二个 `@Autowired`、收敛构造器注入。
3. `§1.2` 收掉 `LoanApplicationServiceImpl.apply` 外层 3 段 warn。
4. `§1.3` i18n 缺 key 补齐 + 完整性校验。
5. `§2.1` / `§2.2` / `§2.5` 三组小清理。
6. `§2.6` vitest script、`§3.C` README 同步。
7. `§2.3` / `§2.4` 命名口径 / 性能微调收尾。

以上顺序已执行完毕。整体没有发现需要回滚已完成 phase 的结构性问题；本轮有效 review 项已全部进入已完成状态。
