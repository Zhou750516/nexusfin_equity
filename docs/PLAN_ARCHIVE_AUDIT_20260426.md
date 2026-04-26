# Plan 归档后的代码落地核查 · Codex 执行清单

> 类型：审计 + 待执行 TODO · 给 Codex 用
> 日期：2026-04-26
> 触发：Claude Code 归档 `docs/plan/` 后做了一轮交叉验证，发现 2 个 minor TODO 需要 Codex 进一步核查并落地
> 关联：[`docs/DAILY_PLAN_EXECUTION_20260426.md`](./DAILY_PLAN_EXECUTION_20260426.md)（plan 工作流性能分析）

---

## 1. 背景与执行原则

### 1.1 触发链

1. Plan 文件夹归档（132 → 41 个根目录文件，其余移到 `topics/` 与 `archive/`）
2. 用户担心归档把 “未完工事项" 埋了
3. 重点核查 3 个高风险领域：齐为 / 小花科技 / 异步补偿
4. 结果：**3 个领域核心交付物全部已落代码**，但发现 2 个 minor TODO 需要进一步处理

### 1.2 已验证 ✓（不要重做）

| 领域 | 验证项 | 状态 |
|------|--------|------|
| 齐为绑卡 | `GET /api/bank-card/sign-status` / `POST /api/bank-card/sign-apply` / `POST /api/bank-card/sign-confirm` | ✅ 全部已实现，路径与 4-21 doc 占位命名 100% 一致 |
| 齐为绑卡 | `BankCardSignService` / `BankCardSignServiceImpl` / 6 个 DTO | ✅ 完整存在 |
| 小花联合登录 | `JointLoginController` / `JointLoginService(+Impl)` / `XiaohuaAuthClient` / `SkeletonXiaohuaAuthClient` / `JointLoginTargetPageResolver` / `JointLoginRequest/Response/Scene` | ✅ Skeleton 完整（真实 impl 等小花文档，是 4-17 doc 明示的预期状态） |
| 异步补偿 | 3 entity / 3 repository / 3 enum / 5 service+Impl / 2 scheduler / 3 utility | ✅ 完整体系已实现 |

→ Codex 不要在这些已验证项上重新折腾。

### 1.3 执行原则

1. **先核查，再动手**：每个 TODO 先验证我的判断是否准确（我可能在某些路径上没找全）
2. **小步走**：每个 TODO 独立完成，独立验证
3. **不动业务核心**：除非补 mapper 或测试，否则不改 main 业务逻辑
4. **不 commit**：完成后让用户验收再决定 commit 时机
5. **跑测试验证**：每个 TODO 完成后跑相应测试，确保不引入回归

---

## 2. TODO #1：齐为会员同步 mapper · `accountNo` 字段确认

### 2.1 来源

`docs/plan/topics/齐为/20260415_齐为绑卡流程新增接口TODO.md` 第 7.1 节（2026-04-21 跟踪补充）：

> 1. **会员同步接口字段补充**
>    - 新增 `mobile`
>    - 新增 `username`
>    - 新增 `accountNo`
>    - 新增 `cardNo`

### 2.2 当前观察

`src/main/java/com/nexusfin/equity/thirdparty/qw/AllinpayMemberSyncPayloadMapper.java` 第 29-33 行：

```java
node.put("mobile", request.mobile());
node.put("username", request.username());
if (request.cardNo() != null) {
    node.put("cardNo", request.cardNo());
}
```

→ `mobile` / `username` / `cardNo` 已落，**`accountNo` 没看到**。

### 2.3 Codex 需要做的事

#### Step 1：验证我的判断是否准确（重要）

我的 grep 可能没找全。在改动前请先核实 `accountNo` 是否真的缺失：

```bash
# 1. 全文搜 accountNo 在 main 代码里出现的位置
grep -rn 'accountNo' src/main/java | grep -v 'allinpay.*direct.*account-no' | grep -v 'AllinpayDirectProperties'

# 2. 看完整的 mapper
cat src/main/java/com/nexusfin/equity/thirdparty/qw/AllinpayMemberSyncPayloadMapper.java

# 3. 看 mapper 的源 DTO（看 request 对象有没有 accountNo() 方法）
find src/main/java -name 'AllinpayMemberSyncRequest*.java' -o -name 'MemberSyncRequest*.java' | xargs cat

# 4. 看 4-21 齐为会员同步收口方案（在根目录还没归档）
cat docs/plan/20260421_齐为会员同步字段升级收口方案.md
```

**判断点**：

- 如果 mapper 已经在某个分支 / 条件下设了 `accountNo`，但我没看到 → 说明误报，**到此结束**
- 如果 4-21 收口方案明确说 `accountNo` 暂不接 / 走别的路径 → 说明本来就不该接，**到此结束**
- 如果 `accountNo` 确实该接但没接 → 进入 Step 2

#### Step 2：补 `accountNo` 字段

如果 Step 1 确认缺失：

1. 检查 `AllinpayMemberSyncRequest` (或类似 DTO) 是否已有 `accountNo` getter
   - 没有：先在 DTO 里加 `accountNo` 字段（`String accountNo`，含 getter）
   - 有：直接进下一步
2. 在 `AllinpayMemberSyncPayloadMapper` 里参照 `cardNo` 的 null-safe 写法补：
   ```java
   if (request.accountNo() != null) {
       node.put("accountNo", request.accountNo());
   }
   ```
3. 检查 `accountNo` 的来源——它从哪个本地字段读？
   - 可能是 `MemberInfo` 的某个字段（绑卡成功后回写的银行卡号 / 账户号）
   - 可能是 `MemberPaymentProtocol` 里
   - 可能是从绑卡确认接口的响应链路传下来的
   - **不确定就先确认源**：grep `setAccountNo\|getAccountNo`，看现有数据流是怎么走的
4. 把数据源串起来到 mapper 入参（这一步可能涉及上层 Service / Controller，**保守推进**——只补必要的链路，不重构）

#### Step 3：测试

```bash
# 单测层（应该有 mapper test 或 client test）
mvn test -Dtest='*AllinpayMemberSync*' -o

# 全单测回归（确保没影响）
mvn test -o

# IT 层（看 NotificationCallback / BenefitOrder / Phase9 集成测试是否仍绿）
mvn failsafe:integration-test -o -Dit.test='Phase9TaskGroupCIntegrationTest,Phase9TaskGroupDIntegrationTest'
```

#### Step 4：验收标准

- [ ] `AllinpayMemberSyncPayloadMapper` 在 `accountNo != null` 时正确写入 JSON node
- [ ] DTO 有对应 `accountNo` getter（null-safe）
- [ ] 如果上层数据源做了改动，串通路径无 NPE 风险
- [ ] `mvn test` 全过
- [ ] 相关 IT 测试全过

### 2.4 风险与边界

- ⚠️ **不要扩大改动**：如果发现 `accountNo` 的真实数据源链路涉及多个 Service 改造，停下来报告，不要一路修
- ⚠️ **不要去问齐为**：齐为 4-21 已经给了文档，根据 4-21 收口方案落地即可
- ⚠️ **不要改测试期望值**：如果有现存测试断言 mapper 输出，确认是新增还是替换语义；新增字段一般不影响现有断言

---

## 3. TODO #2：`BenefitOrderControllerIntegrationTest` 补异步补偿子测试

### 3.1 来源

`docs/plan/topics/异步补偿/20260417_异步消息补偿机制_checklist.md`：

```
## 齐为权益购买补偿
...
- [ ] 补 controller/integration 测试

## 测试
...
- [ ] BenefitOrderControllerIntegrationTest
```

→ 4-17 checklist 自己标的 TODO，至今未补。

### 3.2 当前观察

```bash
grep 'AsyncCompensation\|补偿' src/test/java/com/nexusfin/equity/controller/BenefitOrderControllerIntegrationTest.java
# (无输出 — 证实未补)
```

### 3.3 Codex 需要做的事

#### Step 1：理解既有补偿测试的写法（参考样本）

`Phase9TaskGroupEIntegrationTest` 已经测了 “齐为权益购买超时进补偿任务" 的场景（checklist 第 66 行 `[x] 补 Phase 9 集成测试`）。先读它学习模式：

```bash
cat src/test/java/com/nexusfin/equity/controller/Phase9TaskGroupEIntegrationTest.java
```

重点看：

1. `@MockBean` 哪些 bean（`AsyncCompensationEnqueueService` 是否被 mock 还是用真实实现观察 enqueue 行为）
2. 怎么触发 “齐为同步失败" 这个 condition
3. 怎么 assert 任务入队（是查 `async_compensation_task` 表？还是 verify mock？）

#### Step 2：在 `BenefitOrderControllerIntegrationTest` 补对应场景

**目标场景**（来自 4-17 checklist 第 53-57 行）：

> ## 齐为权益购买补偿
> - [x] 齐为同步失败时入补偿任务
> - [x] `benefit_order.sync_status` 正确落失败/待补偿状态
> - [x] 控制器仍返回失败口径
> - [x] 补 service 测试
> - [ ] 补 controller/integration 测试  ← 要补这个

至少补 **3 个 test method**：

1. `shouldEnqueueAsyncCompensationWhenQwSyncFails`
   - 模拟齐为下游同步失败（`@MockBean` 齐为同步 service 抛 `UpstreamTimeoutException` 或失败）
   - 调控制器接口
   - assert：`async_compensation_task` 表有新记录 + 字段（task_type / biz_key / status / partition_id）正确
2. `shouldUpdateBenefitOrderSyncStatusWhenSyncFails`
   - 同上 trigger
   - assert：`benefit_order.sync_status` 字段进入失败 / 待补偿状态（具体值看现有 enum）
3. `shouldReturnFailureCodeToControllerEvenWhenCompensationEnqueued`
   - 同上 trigger
   - assert：HTTP 响应 `code` 是失败码（不是 0），即使后台已经入了补偿任务

#### Step 3：决定基类

如果 `BenefitOrderControllerIntegrationTest` 当前没继承 `AbstractIntegrationTest`：

- 看它的 `@MockBean` 集合
- 如果只 mock 业务 service，**不强制迁移基类**——保持原样，避免和 T1.2 重构冲突
- 如果以后多个 IT 都需要 mock `AsyncCompensationEnqueueService`，再考虑做 `AbstractAsyncCompensationIT` 基类

→ 保守原则：**新建测试方法，不重构既有结构**。

#### Step 4：测试

```bash
# 只跑这一类
mvn failsafe:integration-test -o -Dit.test=BenefitOrderControllerIntegrationTest

# 完整回归（确认其它 IT 没受影响）
mvn verify -o
```

#### Step 5：验收标准

- [ ] `BenefitOrderControllerIntegrationTest` 至少有 3 个新 `@Test` 方法覆盖 “齐为同步失败 → 入队 / sync_status / 返回失败码" 三件事
- [ ] 新方法跑通（0 失败 0 错误）
- [ ] `mvn verify` 全套 0 失败（只允许 5 skipped 是已知的）
- [ ] 4-17 checklist 第 57 行、第 101 行的两个 `[ ]` 可以打钩（**Codex 也顺手把 checklist 的勾打上**）

### 3.4 风险与边界

- ⚠️ **不要新建 SpringContext 风味**：尽量复用既有 mock 集合，避免 Spring TestContext 缓存键再分裂
- ⚠️ **不要动数据库 schema**：任务表已经存在（验证过），不需要新加字段
- ⚠️ **不要改 Phase9TaskGroupEIntegrationTest**：那是参考样本，不是要修的对象
- ⚠️ **小心 H2 vs MySQL 行为差异**：如果断言依赖某个 SQL 行为，跑 H2 通过不代表 MySQL 通过；如果不放心可以加 mysql-it tag

---

## 4. 不在范围内（明确划界）

Codex 不要做以下事，避免越界：

- ❌ 不要去补 “MySQL 回归无 schema 冲突" 那一项的测试 → 需要 MySQL 环境，由用户决定
- ❌ 不要把 4-17 checklist 里其它已勾选项再 “优化"（已落地的别动）
- ❌ 不要把 `topics/小花科技/` 下的真实 impl 写出来（等小花正式文档）
- ❌ 不要扩大重构 `AllinpayMemberSyncPayloadMapper`，只补字段
- ❌ 不要 commit；完成后让用户审

---

## 5. 完成后产出

跑完两个 TODO 后，请回写本文档第 6 节（追加），包含：

1. 每个 TODO 的实际改动文件清单
2. 测试运行结果（class / tests / failures / time）
3. 发现的额外问题（如果有）
4. `mvn verify` 总耗时（对比当前 ~62.4s）

---

## 6. 执行结果（Codex 完成后追加）

> 已按“先验证、为真才动手”的原则执行；以下为实际结果。

### 6.1 TODO #1 · accountNo 字段

- 验证结果：`误报，不做`
- 结论依据：
  - `AllinpayMemberSyncPayloadMapper` 当前确实未映射 `accountNo`，但 4-21 收口方案已明确：当前阶段不直接改生产会员同步主链路。
  - `accountNo` 本地来源尚未稳定，当前仍保留 `payProtocolNo` 主路径；满足 §2.3 Step 1 中“若 4-21 方案写明暂不接/走别的路径，则到此结束”的停止条件。
- 实际改动文件：
  - `无`
- 测试结果：
  - `无（未改代码，未新增验证）`

### 6.2 TODO #2 · BenefitOrderIT 补偿子测试

- 实际改动文件：
  - `src/test/java/com/nexusfin/equity/controller/BenefitOrderControllerIntegrationTest.java`
  - `docs/plan/topics/异步补偿/20260417_异步消息补偿机制_checklist.md`（勾选 2 项）
- 新增测试方法：
  - `shouldEnqueueAsyncCompensationWhenQwSyncFails`
  - `shouldUpdateBenefitOrderSyncStatusWhenSyncFails`
  - `shouldReturnFailureCodeToControllerEvenWhenCompensationEnqueued`
- 测试结果：
  - 定向 IT：`mvn failsafe:integration-test -o -Dit.test=BenefitOrderControllerIntegrationTest` -> `tests=9 errors=0 failures=0`，总耗时 `17.137 s`
  - 完整回归：`mvn verify -o` 通过；`Tests run: 65, Failures: 0, Errors: 0, Skipped: 5`，总耗时 `01:21 min`
  - 备注：执行过程中曾出现过 `RestYunkaGatewayClientTest` 本地端口绑定假失败，但当前完整复跑未复现；仍建议按环境问题跟踪，而非归因本次改动。

### 6.3 总耗时与下一步

- 实际投入工时：`约 1 小时`
- 是否发现额外问题：`是`
- 额外问题说明：
  - `mvn verify -o` 在沙箱环境下存在本地端口绑定假失败，建议后续遇到同类测试优先区分“环境失败”和“业务失败”。
- 建议 commit 拆分（给用户参考）：
  ```
  1. test: cover async compensation in BenefitOrderControllerIntegrationTest
  2. docs: sync async compensation checklist and audit result
  ```
