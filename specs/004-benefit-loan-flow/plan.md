# Implementation Plan: 惠选卡权益与借款流程 V1 页面交互理解

**Branch**: `004-benefit-loan-flow` | **Date**: 2026-03-30 | **Spec**: [/Users/lixiaokun/Projects/nexusFin/nexusfin-equity/specs/004-benefit-loan-flow/spec.md](/Users/lixiaokun/Projects/nexusFin/nexusfin-equity/specs/004-benefit-loan-flow/spec.md)
**Input**: Feature specification from `/specs/004-benefit-loan-flow/spec.md`

## Summary

本轮实现先遵循“thirdparty-first”策略，不直接铺开全部页面流程，而是先落地科技平台主动调用接口的 ABS 侧第三方客户端能力，为后续借款状态同步、还款结果同步和页面编排提供稳定底座。

当前切片聚焦：

- 新增 `thirdparty/techplatform` 客户端与请求模型
- 补齐 `nexusfin.third-party.tech-platform` 配置
- 用自动化测试验证加密、签名、请求封装和响应解包
- 产出便于 review 的实现说明和设计文档

## Technical Context

**Language/Version**: Java 17  
**Primary Dependencies**: Spring Boot 3.2.12, spring-web `RestClient`, Jackson, Spring Boot Test  
**Storage**: MySQL 8.0 / H2（本切片本身不新增持久化）  
**Testing**: JUnit 5, AssertJ, Spring `MockRestServiceServer`, Maven Surefire  
**Target Platform**: Linux/macOS Spring Boot service runtime  
**Project Type**: Web service  
**Performance Goals**: 第三方通知接口单次调用维持在文档 SLA 500ms-1500ms 量级，当前以客户端正确性为主  
**Constraints**: 保持现有 controller -> service -> repository 分层；本轮不新增数据库表；外部接口字段遵循科技平台文档，金额单位保留外部约定的 `BigDecimal` 元  
**Scale/Scope**: 仅完成基础科技平台 outbound client 和设计文档，不在本轮完成全部页面控制器与业务编排

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- `Result<T>` response envelope, `@Valid` request validation, and versionless API paths are identified for后续页面接口；本轮 thirdparty client 不直接暴露新的 HTTP API。
- Business logic placement remains `controller -> service -> repository`，本轮仅新增 `thirdparty` 集成层和配置层，不引入 controller 写业务逻辑问题。
- Domain data rules are preserved；本轮未新增数据库实体。第三方边界金额字段因外部接口规范保留 `BigDecimal` 元，并在文档中显式说明。
- Operational rules are covered：新增客户端日志打印 `traceId + techPlatformPath + techPlatformCode`，后续业务层接入时再补 `bizOrderNo`。
- Verification plan includes `mvn -q -Dtest=TechPlatformClientTest test`、`mvn -q test`、`mvn -q clean package -DskipTests`、`mvn -q checkstyle:check`。

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
│   │   └── thirdparty/techplatform/
│   └── resources/application.yml
└── test/
    ├── java/com/nexusfin/equity/thirdparty/techplatform/
    └── resources/
```

**Structure Decision**: 保持单体 Spring Boot 服务结构，不把科技平台调用实现散落到 service 层，统一收敛在 `src/main/java/com/nexusfin/equity/thirdparty/techplatform/`。

## Phase 0: Research Summary

- 科技平台标准接口文档第 5 章属于“机构 -> 平台”方向，适合通过本仓库 `thirdparty` 客户端封装。
- 当前 ABS 仓库在业务上最先需要的是通知类接口，因此优先实现：
  - `creditStatusNotice`
  - `loanInfoNotice`
  - `repayInfoNotice`
- 文档未给出完整签名算法与 AES 模式细节，因此当前实现采用“可配置签名算法 + 可配置 AES 算法”的策略，默认值用于本地联调和测试，不把默认值当成最终联调定论。

## Phase 1: Design Outputs

- 统一配置类：`TechPlatformProperties`
- 统一加密/签名 helper：`TechPlatformPayloadCodec`
- 统一 client 接口：`TechPlatformClient`
- 统一 client 实现：`TechPlatformClientImpl`
- 外部通知请求模型：
  - `CreditStatusNoticeRequest`
  - `LoanInfoNoticeRequest`
  - `RepayInfoNoticeRequest`
- 基础响应模型：`TechPlatformNotifyResponse`

## Post-Design Constitution Check

- 没有引入跨系统职责越界：ABS 仍然只封装对科技平台的 outbound 调用。
- 没有新增数据库耦合或绕过 service 的捷径。
- 外部接口字段与本地内部金额规则冲突已在文档中显式隔离。
- 验证方案明确，且已有专门测试覆盖第三方 client 的关键行为。

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| 外部金额使用 `BigDecimal` 元 | 科技平台接口文档本身以元为单位 | 强行改成 `Long` 分会扭曲外部协议并增加误转风险 |
| 可配置签名/加密算法 | 上游文档未完全固定实现细节 | 直接写死单一算法会让联调时风险集中在代码改动而不是配置调整 |
