# Implementation Plan: 惠选卡权益与借款流程 V1 页面交互理解

**Branch**: `004-benefit-loan-flow` | **Date**: 2026-04-24 | **Spec**: `/Users/lixiaokun/Projects/nexusFin/nexusfin-equity/specs/004-benefit-loan-flow/spec.md`
**Input**: Feature specification from `/specs/004-benefit-loan-flow/spec.md`

## Summary

当前代码已经完成艾博生 H5 面向权益、借款、还款三条主线的第一版接口落地，核心入口已经存在：

- `GET /api/benefits/card-detail`
- `POST /api/benefits/activate`
- `GET /api/loan/calculator-config`
- `POST /api/loan/calculate`
- `POST /api/loan/apply`
- `GET /api/loan/approval-status/{applicationId}`
- `GET /api/loan/approval-result/{applicationId}`
- `GET /api/repayment/info/{loanId}`
- `POST /api/repayment/submit`
- `GET /api/repayment/result/{repaymentId}`

但这批实现仍主要是“H5 主链先跑通”的第一版，还没有完全对齐 `科技平台-小花接口文档-艾博生4.22` 与 `2026-04-23` 会议结论。

本轮计划的目标不是再按旧假设继续补 direct tech-platform 能力，而是统一按以下边界收口：

1. **艾博生通过云卡调用小花**
2. **小花通过云卡回调艾博生**
3. **借款主链要兼容新方案：艾博生先调用云卡建放款订单，权益成功后再由云卡触发小花放款**

因此，本轮后续工作的核心是：

- 复盘当前代码已实现基线
- 找出相对小花 `4.22` 文档未实现或对齐错误的部分
- 建立共享的“小花经云卡” typed DTO + facade
- 重排权益、借款、还款、回调四条线的交付顺序
- 将过时的 `T013-T033` 替换为面向当前代码现实的任务清单

## Technical Context

**Language/Version**: Java 17  
**Primary Dependencies**: Spring Boot 3.2.12, Spring Validation, Jackson, MyBatis-Plus, 当前 Yunka gateway 调用层  
**Storage**: MySQL 8.0 / H2（本轮优先做接口对齐与服务编排，不以新增表为前置）  
**Testing**: JUnit 5, Spring Boot integration tests, 文档一致性检查  
**Target Platform**: Linux/macOS Spring Boot service runtime with ABS-owned H5  
**Project Type**: Web service  
**Performance Goals**: H5 查询与提交接口继续以 500ms-1500ms 为参考；上游 Yunka/小花响应遵循现有 SLA 设计  
**Constraints**: 保持 `controller -> service -> repository` 分层；统一 `Result<T>`；接口路径不带版本号；关键日志包含 `traceId + bizOrderNo`；艾博生不直接集成小花协议细节到 controller 层  
**Scale/Scope**: 本轮聚焦“已实现代码与小花 4.22 文档的对齐改造”，不在本轮内直接完成全部云卡新状态机接口改造

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- 继续保持所有新增/改造的 REST 接口返回 `Result<T>`，并使用 `@Valid` 做参数校验。
- 所有业务逻辑继续放在 service 层，controller 只负责参数接收与响应包装。
- Yunka 仍是艾博生访问小花的唯一网关边界，小花协议字段不直接扩散到页面控制器。
- 关键主键至少覆盖 `traceId`、`bizOrderNo`、`applicationId`、`loanId`、`repaymentId`、`requestId` 等链路标识。
- 本轮验证以“共享 facade 单测 + 现有 loan/repay/benefits service/controller 测试补齐”为主，之后再做联调级验证。

## Project Structure

### Documentation (this feature)

```text
specs/004-benefit-loan-flow/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── page-flow-api.md
│   └── tech-platform-outbound.md
└── tasks.md

docs/third-part/科技平台/
└── 科技平台-小花接口文档-艾博生4.22_整理分析.md

docs/plan/
├── 20260423_小花云卡齐为联动会议纪要与行动计划.md
├── 20260424_小花4.22现有实现差异分析与开发计划.md
└── 20260424_小花4.22开发任务拆解.md
```

### Source Code (repository root)

```text
src/
├── main/
│   ├── java/com/nexusfin/equity/
│   │   ├── config/
│   │   ├── controller/
│   │   ├── service/
│   │   ├── service/impl/
│   │   ├── repository/
│   │   ├── entity/
│   │   ├── dto/request/
│   │   ├── dto/response/
│   │   ├── thirdparty/techplatform/
│   │   └── thirdparty/yunka/
│   └── resources/application.yml
└── test/
    └── java/com/nexusfin/equity/
```

**Structure Decision**: 保持现有单体 Spring Boot 结构，但后续不再让 `LoanServiceImpl`、`RepaymentServiceImpl` 直接堆叠原始 Yunka JSON 解析，而是收敛为共享 `XiaohuaGatewayService` 或同等边界服务。

## Current Implementation Baseline

### 已实现能力

1. H5 权益、借款、还款 API 入口已存在。
2. 借款和还款核心上游调用已经走 `YunkaGatewayClient`。
3. `NotificationCallbackController` 已有放款/还款 forward callback 基础入口。
4. `TechPlatformClient` 已存在，但目前只保留旧的 3 个 notify 类接口，不是小花 `4.22` 主链。

### 已识别主要缺口

1. Yunka 路径配置只覆盖：
   - `/loan/trail`
   - `/loan/query`
   - `/loan/apply`
   - `/repay/trial`
   - `/repay/apply`
   - `/repay/query`
2. 还缺以下小花 `4.22` 关键能力：
   - `/protocol/queryProtocolAggregationLink`
   - `/user/token`
   - `/user/query`
   - `/loan/repayPlan`
   - `/card/smsSend`
   - `/card/smsConfirm`
   - `/card/userCards`
   - `/credit/image/query`
   - `2.15 权益订单同步`
3. 当前 `LoanServiceImpl` 对 `loan/query` 的状态映射与小花 `4.22` 文档冲突：
   - 当前代码将 `7003` 映射为成功
   - 小花文档要求：`7001=成功`、`7002=处理中`、`7003=失败`
4. 权益页仍以本地静态展示逻辑为主，未接动态协议和绑卡列表。
5. 还款链路缺少短信发送、短信确认、动态绑卡列表。
6. 回调链路仍是旧 forward callback 语义，未显式定义“小花通过云卡回调艾博生”的标准 DTO。

## Gap Summary

### Gap 1：共享基础层缺失

- 没有面向小花 `4.22` 的 typed request/response 模型层
- 没有统一的“小花经云卡 facade/service”
- 没有统一的新放款/还款回调 DTO 与状态映射

### Gap 2：权益页动态能力不足

- 未接协议列表
- 未接用户已绑卡列表
- 未做权益订单同步
- 协议阅读完成状态未形成稳定后端约束

### Gap 3：借款链路与新主线不一致

- `loan/apply` 字段不完整
- `loan/query` 状态映射错误
- 缺少 `loan/repayPlan`
- 仍未显式适配“云卡先建放款订单，再触发小花放款”的新方案

### Gap 4：还款链路缺少前置步骤

- 没有 `smsSend`
- 没有 `smsConfirm`
- 没有 `userCards` 动态列表
- 还款回调模型仍待标准化

### Gap 5：旧 tasks 已过期

- `T013-T033` 基于旧边界，不能直接继续执行
- 需按当前代码实际完成度和新文档边界重写

## Delivery Sequence

1. 第一阶段：共享基础收口
   - 扩展 Yunka path 配置
   - 建立 typed DTO
   - 建立共享 facade/service
   - 固化放款/还款回调 DTO
2. 第二阶段：权益页动态化
   - 接协议列表
   - 接绑卡列表
   - 补权益同步
3. 第三阶段：借款链路对齐
   - 修状态映射
   - 补申请字段
   - 补 `loan/repayPlan`
   - 接放款回调
4. 第四阶段：还款链路对齐
   - 接 `smsSend`
   - 接 `smsConfirm`
   - 接 `userCards`
   - 接还款回调
5. 第五阶段：待云卡新接口定稿后，再推进“云卡建放款订单 / 云卡状态查询 / 放弃权益关单”主链改造

## Phase 0: Research Summary

- `docs/third-part/科技平台/科技平台-小花接口文档-艾博生4.22_整理分析.md` 已明确当前项目实际方向：艾博生通过云卡调用小花，小花通过云卡回调艾博生。
- `docs/plan/20260423_小花云卡齐为联动会议纪要与行动计划.md` 已确认借款主链变化：云卡先建放款订单，权益成功后再触发放款。
- 当前代码已落地 H5 主入口，因此本轮不是从零开发，而是“在现有入口上做文档和协议对齐”。
- 旧的 `specs/004-benefit-loan-flow/tasks.md` 未完成部分已经落后于当前代码与外部文档边界，必须重排。

## Phase 1: Design Outputs

- 对齐分析文档：`docs/plan/20260424_小花4.22现有实现差异分析与开发计划.md`
- 任务拆解文档：`docs/plan/20260424_小花4.22开发任务拆解.md`
- 小花 `4.22` Markdown 整理文档：`docs/third-part/科技平台/科技平台-小花接口文档-艾博生4.22_整理分析.md`
- 本 feature 新版 `plan.md`
- 本 feature 新版 `tasks.md`

## Post-Design Constitution Check

- 没有回退到“艾博生直连小花”的旧边界。
- 没有把云卡网关协议细节直接散落到 controller。
- 没有忽略当前代码已实现基线，而是按“增量对齐”方式规划。
- 没有继续沿用已过期的 `T013-T033` 任务顺序。

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| 保留现有 H5 API 入口并做增量对齐，而不是整体推倒重写 | 当前代码已存在完整第一版主链，整体重写风险高且回归范围大 | 从零重写会丢失现有 H5 联通成果，并放大联调风险 |
| 暂不直接落“云卡先建放款订单”真实主链代码 | 云卡新接口尚未定稿 | 过早编码会把未确认状态机固化进代码 |
