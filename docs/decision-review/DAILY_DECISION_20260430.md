# 每日决策审查 - 2026年4月30日

**日期**: 2026-04-30  
**审查人**: 技术负责人 + 协调 / 审查会话  
**状态**: ✅ 已完成（callback P0 闭合 + Yunka 协议对齐 + 联登/权益跳转收口 + `benefiturl` 主链接线）

---

## 今日决策摘要

**本日形成 5 个关键决策 / 结论**

### 快速概览

| # | 决策标题 | 分类 | 优先级 | 状态 |
|---|---------|------|--------|------|
| 1 | callback P0 线在 `B-P0-3.4` 闭合后整体收口，不再重复证明 | 测试边界 / 执行节奏 | ⭐⭐⭐⭐ | ✅ 已决策 |
| 2 | ABS -> Yunka 请求协议必须按 `2026-04-10` 文档对齐，并在同一 trace 下打印完整 JSON | 协议对齐 / 排障基线 | ⭐⭐⭐⭐ | ✅ 已决策 |
| 3 | `push / exercise / refund` 联登入口正式拆开，不再复用“已有权益单”假设 | 场景建模 / 前后端边界 | ⭐⭐⭐⭐ | ✅ 已决策 |
| 4 | `redrect_benefit_url` 先作为独立接口能力收口，避免和主链接线混淆 | 接线边界 / 风险隔离 | ⭐⭐⭐⭐ | ✅ 已决策 |
| 5 | `benefiturl` 接到 Yunka/Xiaohua 权益同步主链，并按强依赖中断失败 | 主链接线 / 协议收口 | ⭐⭐⭐⭐ | ✅ 已决策 |

---

## 决策详情

### DECISION-073: callback P0 线在 `B-P0-3.4` 闭合后整体收口，不再重复证明

**决策日期**: 2026-04-30  
**决策人**: 技术负责人 + 协调 / 审查会话  
**所属分类**: 测试边界 / 执行节奏  
**优先级**: ⭐⭐⭐⭐ 高  
**影响范围**: 中

#### 问题背景

callback P0 线此前已连续闭合到 `B-P0-3.4`，其中坏签名路径已验证：

- 请求被签名层拒绝
- 没有进入 callback 业务推进链
- 没有任何新增副作用

继续围绕同一条线重复扩测，新增信息价值已经很低。

#### 最终决策

**选择**: 将 callback P0 线明确标记为已闭合，后续不再重复投入在 `B-P0-3.1 ~ B-P0-3.4` 的旧证明上。

#### 理由

1. 运行态证据已覆盖关键 happy / reject / no-side-effect 判断。
2. 后续更值得投入的是 QW 签约异常线和 Yunka 联调主线。
3. 保持“闭合后退出”的节奏，避免测试资源在旧 case 上空转。

#### 实施结果

- [x] `B-P0-3.4` 坏签名无副作用已验证
- [x] callback P0 线整体标记为已闭合
- [x] 后续主线切到 Yunka 和 joint-login / benefit redirect

---

### DECISION-074: ABS -> Yunka 请求协议必须按 `2026-04-10` 文档对齐，并在同一 trace 下打印完整 JSON

**决策日期**: 2026-04-30  
**决策人**: 技术负责人 + 开发负责人  
**所属分类**: 协议对齐 / 排障基线  
**优先级**: ⭐⭐⭐⭐ 高  
**影响范围**: 大

#### 问题背景

在进入真实 Yunka 联调前，现有 ABS -> Yunka 协议与文档存在偏差：

- header 不完整
- `loan/trial` path 曾有漂移
- backend 日志不能在单一 trace 下直接看到完整 request / response body

这会导致后续联调即使失败，也很难快速确认是 ABS 报文问题还是对方环境问题。

#### 最终决策

**选择**: 以 `2026-04-10` 云卡接口文档为准收口协议，并把 request / response JSON 日志作为排障基线固定下来。

#### 理由

1. 先把 ABS 出站协议打实，后续才能明确把问题归因给对方环境。
2. 联调期最缺的不是更多功能，而是更清晰的证据。
3. 完整 JSON 日志能直接支撑群内同步和对方排障。

#### 实施结果

- [x] 补齐 `X-Trace-Id`、`X-Request-Id`、`X-Biz-Order-No`、`X-Timestamp`、`X-Channel-Code`、`X-Signature`
- [x] `loan-calculate` path 收口到 `/loan/trial`
- [x] backend 可打印 `requestBodyJson / responseBodyJson`
- [x] 真实 `/api/loan/calculate` 已打到 Yunka
- [x] 外部阻塞收敛为 `KJ_NOT_READY / kj.private-key 配置无效`

---

### DECISION-075: `push / exercise / refund` 联登入口正式拆开，不再复用“已有权益单”假设

**决策日期**: 2026-04-30  
**决策人**: 技术负责人 + 开发负责人  
**所属分类**: 场景建模 / 前后端边界  
**优先级**: ⭐⭐⭐⭐ 高  
**影响范围**: 大

#### 问题背景

原实现把 `push` 和 `exercise` 都收口到 `joint-dispatch`，隐含了“已有 `benefitOrderNo`”的前提。但真实业务里：

- `push` 发生在导流阶段
- 导流时还没有创建权益订单

这意味着原方案在业务语义上是错位的。

#### 最终决策

**选择**: 正式拆开三类 scene：

- `push`：允许无 `benefitOrderNo`，成功后进 `/landing`
- `exercise`：必须有 `benefitOrderNo`
- `refund`：必须有 `benefitOrderNo`

#### 理由

1. `push` 和“已有权益单后的行权分发”不是一个业务阶段。
2. 缺失单号时，应由业务场景差异解决，而不是继续复用 fallback 掩盖问题。
3. 场景拆开后，联登入口语义更稳定，也更适合后续外部联调。

#### 实施结果

- [x] `push` 无 `benefitOrderNo` 运行态联登通过
- [x] `exercise / refund` 缺单号时受控失败
- [x] `exercise / refund` 带单号时运行态联登通过

---

### DECISION-076: `redrect_benefit_url` 先作为独立接口能力收口，避免和主链接线混淆

**决策日期**: 2026-04-30  
**决策人**: 技术负责人 + 协调 / 审查会话  
**所属分类**: 接线边界 / 风险隔离  
**优先级**: ⭐⭐⭐⭐ 高  
**影响范围**: 大

#### 问题背景

当天已实现：

- `POST /api/auth/redrect_benefit_url`

在当天前半段，运行态验证已经证明：

- `POST /api/auth/redrect_benefit_url` 可正常工作
- timeout / reject 收口稳定

但当时继续核查主链发现：

- 当前主链还没有任何一条“权益订单信息同步”真正调用这个能力
- 同步请求对象和 mapper 里也都还没有 `benefiturl`

如果不拆开处理，很容易把“接口能力已存在”误说成“主链已接通”。

#### 最终决策

**选择**: 先承认 `redrect_benefit_url` 已完成接口级收口，并要求在主链接线完成前，不得把接口能力误报成主链闭环。

#### 理由

1. 这能保住今天已经拿到的联调能力，不必返工回滚。
2. 同时也避免对外口径过度乐观，误称“benefiturl 主链已闭环”。
3. 下一步任务会更聚焦：只做主链接线，不重做联登和 redirect 生成本身。

#### 实施结果

- [x] `redrect_benefit_url` 正常链路通过
- [x] timeout / reject 收口通过
- [x] 当前真实语义明确为“QW exercise redirect URL”
- [x] 在主链接线前，状态被明确标记为 `NOT_INTEGRATED`

---

### DECISION-077: `benefiturl` 接到 Yunka/Xiaohua 权益同步主链，并按强依赖中断失败

**决策日期**: 2026-04-30  
**决策人**: 技术负责人 + 开发负责人  
**所属分类**: 主链接线 / 协议收口  
**优先级**: ⭐⭐⭐⭐ 高  
**影响范围**: 大

#### 问题背景

在 DECISION-076 明确“接口能力”和“主链接线”不可混淆后，晚间开发已将 `benefiturl` 正式接入真实主链：

- 主链确定为 `BenefitsServiceImpl.activate(...) -> XiaohuaGatewayService.syncBenefitOrder(...)`
- 主链会先调用 `BenefitRedirectUrlService.generate(...)`
- 然后把返回值放入 Yunka/Xiaohua 出站对象 `BenefitOrderSyncRequest.benefiturl`

同时，`/api/benefits/activate` 的请求契约已补成必须带 `token`，H5 也已从 joint-login session 取 token 随请求提交。

#### 最终决策

**选择**: 将 `benefiturl` 的主链接线正式落在 Yunka/Xiaohua 的权益订单同步链上，并采用强依赖策略；如果 redirect URL 生成失败，则中断同步，不允许静默降级发送空字段。

#### 理由

1. 科技平台文档已将 `benefiturl` 视为必填，再继续不带字段同步只会扩大协议漂移。
2. `redrect_benefit_url` 已经存在稳定能力，没有必要在主链里再造第二套 URL 逻辑。
3. 强依赖虽然会带来部分前置动作已执行、主链同步被拦住的语义，但比“继续发送不完整协议”更可控。

#### 实施结果

- [x] `BenefitsServiceImpl.activate(...)` 已先生成 `benefitUrl` 再发起 `syncBenefitOrder(...)`
- [x] `BenefitOrderSyncRequest` 已新增 `@JsonProperty("benefiturl")`
- [x] `BenefitsActivateRequest` 已强制要求 `token`
- [x] H5 `BenefitsCardPage` 已从 joint-login session 读取 token 并随激活请求提交
- [x] 定向测试已覆盖：
  - 主链调用 `BenefitRedirectUrlService`
  - Yunka/Xiaohua 出站 payload 包含 `benefiturl`
  - redirect URL 生成失败时不会静默降级继续同步

---

## 今日工作进度

### 已完成的工作

- ✅ callback P0 线闭合
- ✅ ABS -> Yunka 协议与日志基线对齐
- ✅ 真实 Yunka 请求到达上游并拿到明确外部 blocker
- ✅ joint-login 场景拆分运行态回归通过
- ✅ `redrect_benefit_url` 接口级运行态回归通过
- ✅ `benefiturl` 已接入 Yunka/Xiaohua 的权益订单信息同步主链

### 进行中的工作

- 🔄 `confirmSign timeout` 仍待最终收口
- 🔄 invalid token 语义仍缺 stub fault 注入入口
- 🔄 `benefiturl` 主链仍待补运行态实发证据与字段语义确认

### 遇到的问题与解决

| 问题 | 严重度 | 解决方案 | 状态 |
|------|--------|---------|------|
| 真实 Yunka 联调前 ABS 出站协议与日志不完整 | 高 | 先按文档收口协议，再做真实请求 | ✅ 已处理 |
| `push` 错误依赖 `benefitOrderNo` | 高 | 将 `push / exercise / refund` 场景正式拆开 | ✅ 已处理 |
| joint-login 运行态被本地 stub 缺 path 阻断 | 中 | 补齐 `/user/token`、`/user/query` stub | ✅ 已处理 |
| `benefiturl` 只有接口能力、主链未接线 | 高 | 先拆成独立接口能力，再单列主链接线任务 | ✅ 已处理 |
| `benefiturl` 已接线但字段语义仍偏向 QW exercise URL | 中 | 保持“已接主链”与“语义风险仍在”两条口径并行 | 🔄 持续跟踪 |

---

## 对上次决策的评估

### DECISION-070 ~ DECISION-072 评估 [来自 2026-04-29]

**决策内容**: 固定本地联调基线，按 Layer 逐条推进异常线，并将公网阻塞与本地异常线隔离  
**实际效果**: 2026-04-30 没有再混用公网 / 本地结论，且今天围绕 Yunka 与 joint-login 的运行态回归也继续复用了统一 baseline  
**评估结果**: ✅ 符合预期

**改进建议**: 继续保持“接口级联调通过”和“主链接线完成”分开汇报，避免证据层级再次混淆。

---

## 明日关注点

1. 给 joint-login 增加 invalid token / session expired 语义 fault，闭合 `JOINT_LOGIN_TOKEN_INVALID` 运行态
2. 为 `benefiturl` 主链补运行态实发 payload 证据
3. 单独评估 `benefiturl` 当前来源于 QW `getExerciseUrl` 的字段语义风险

---

## 一句话结论

> 2026-04-30 的关键不是继续扩 case，而是把 callback P0、Yunka 协议、joint-login、benefit redirect 和 `benefiturl` 主链接线逐步打实；当前真正剩下的已不再是“有没有接线”，而是 invalid token 运行态证据与 `benefiturl` 字段语义风险。
