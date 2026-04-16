# Implementation Plan: 惠选卡权益与借款流程 V1 页面交互理解

**Branch**: `004-benefit-loan-flow` | **Date**: 2026-03-30 | **Spec**: [/Users/lixiaokun/Projects/nexusFin/nexusfin-equity/specs/004-benefit-loan-flow/spec.md](/Users/lixiaokun/Projects/nexusFin/nexusfin-equity/specs/004-benefit-loan-flow/spec.md)
**Input**: Feature specification from `/specs/004-benefit-loan-flow/spec.md`

## Summary

本轮实现先遵循“abs-h5 + yunka-gateway-first”策略：所有 H5 页面统一由艾博生承载，艾博生服务端不再直接对接科技平台，而是先定义并落稳“艾博生 -> 云卡 gateway”这一层统一契约，为后续借款状态同步、还款结果同步和页面编排提供稳定底座。

当前切片聚焦：

- 固化“艾博生提供全部 H5，云卡仅做 gateway”的系统边界
- 产出艾博生调用云卡 gateway 的接口契约文档
- 调整页面 API 规划，使借款确认与审核页面均归属艾博生
- 在恢复代码实现前，以新网关边界为准重排后续实现顺序
- 明确实施顺序为：先完成艾博生 H5 页面与艾博生后端接口联通，再推进艾博生 -> 云卡 -> 科技平台链路开发与联调

## Technical Context

**Language/Version**: Java 17  
**Primary Dependencies**: Spring Boot 3.2.12, Spring Validation, Jackson, 文档先行的云卡 gateway 契约  
**Storage**: MySQL 8.0 / H2（本轮以文档与边界重排为主，不新增持久化）  
**Testing**: 文档评审、一致性检查；恢复代码实现后再补自动化测试  
**Target Platform**: Linux/macOS Spring Boot service runtime with ABS-owned H5  
**Project Type**: Web service  
**Performance Goals**: H5 页面由艾博生统一承载，核心查询/提交流程仍以接口 SLA 500ms-1500ms 为参考  
**Constraints**: 保持现有 controller -> service -> repository 分层；云卡仅承担 gateway 与记录职责；艾博生不直接调用科技平台；本轮不新增数据库表  
**Scale/Scope**: 本轮先完成云卡 gateway 契约与页面职责重排，不在本轮完成全部页面控制器与业务编排

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- `Result<T>` response envelope, `@Valid` request validation, and versionless API paths are identified for后续艾博生 H5 API；本轮主要新增外部接口文档，不直接暴露新的代码接口。
- Business logic placement remains `controller -> service -> repository`；云卡 gateway 作为外部边界被隔离，不把科技平台协议细节直接扩散到页面层。
- Domain data rules are preserved；本轮未新增数据库实体，跨系统主关联键以 `uid`、`benefitOrderNo`、`applyId`、`loanId` 等为主。
- Operational rules are covered：云卡 gateway 文档明确要求记录 `traceId + requestId + bizOrderNo`，后续实现时再补具体日志与审计表设计。
- Verification plan includes plan/spec/contract consistency review，恢复代码实现后再执行 `mvn test`、`mvn clean package -DskipTests`、`mvn checkstyle:check`。

## Project Structure

### Documentation (this feature)

```text
specs/004-benefit-loan-flow/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── page-flow-api.md
└── tasks.md

docs/third-part/云卡/
└── 20260410_艾博生调用云卡接口文档.md
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
│   │   └── thirdparty/
│   └── resources/application.yml
└── test/
    └── resources/
```

**Structure Decision**: 保持单体 Spring Boot 服务结构，不把外部协议细节散落到 service 层。后续如恢复代码实现，统一收敛在 `src/main/java/com/nexusfin/equity/thirdparty/` 下，以“艾博生 -> 云卡 gateway”为第一层封装边界。

## Delivery Sequence

1. 第一阶段：优先完成艾博生 H5 页面与艾博生后端接口的联通性开发、页面冒烟验证和前后端联调。
2. 第二阶段：在第一阶段稳定后，再完成艾博生调用云卡 gateway、由云卡转发科技平台接口的开发与联调。
3. 排障顺序保持“先内部、后外部”，避免在页面链路未稳定时过早引入跨系统联调噪音。

## Phase 0: Research Summary

- `2026-04-10` 决策已将所有 H5 页面统一收敛到艾博生，因此原“云卡页面”假设不再成立。
- 艾博生与科技平台之间不再直连，统一改为调用云卡 gateway，再由云卡转发科技平台。
- 云卡当前只承担 gateway 职责，因此最先需要定版的是接口清单、统一 header、统一响应结构、幂等与日志要求。
- 在网关真实协议未确认前，不继续推进直接面向科技平台的代码实现，以避免边界返工。

## Phase 1: Design Outputs

- 页面职责重排：权益页、借款确认页、借款审核页、还款页均由艾博生提供
- 网关契约文档：`docs/third-part/云卡/20260410_艾博生调用云卡接口文档.md`
- 页面边界文档：`specs/004-benefit-loan-flow/contracts/page-flow-api.md`
- 第一阶段实现入口：优先落艾博生 H5 所需页面接口与内部服务编排
- 第二阶段实现入口：待云卡 gateway 契约确认后，再拆分 `auth`、`credit`、`loan`、`repay` 客户端模型

## Post-Design Constitution Check

- 没有引入跨系统职责越界：ABS 仅承载 H5 与自身业务，云卡仅做 gateway，科技平台继续作为能力提供方。
- 没有新增数据库耦合或绕过 service 的捷径。
- 页面责任与服务端调用责任已在文档中重新隔离。
- 在真实接口未定前，先做文档定版而不是推进错误边界下的实现，符合低返工原则。

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| 先更新文档而不恢复代码实现 | 新技术边界刚调整，优先避免错误实现继续扩散 | 直接沿旧方案继续开发会放大“页面归属”和“调用方向”的返工成本 |
