# Contract Notes: Page Flow APIs

本文件记录 `004-benefit-loan-flow` 的页面 API 范围边界。

## Current Status

当前实现切片**没有新增页面 HTTP API**。

原因：

- 用户要求优先完成 `thirdparty` 科技平台接口调用实现。
- 当前代码和任务清单中，页面接口属于后续 `US1-US3` 阶段。

## Planned Page APIs

后续计划中的页面 API 包括：

- 艾博生权益页：
  - `/api/equity/info`
  - `/api/equity/rules`
  - `/api/users/payment-card`
- 云卡借款确认/状态：
  - `/api/loan/trial`
  - `/api/loan/status`
  - `/api/users/receiving-card`
- 艾博生还款：
  - `/api/repayment/trial`
  - 还款确认接口

## Contract Rules

- 统一使用 `Result<T>`
- 保持 versionless path
- 写接口使用 `@Valid`
- 内部金额字段优先 `Long` 分；外部第三方契约字段按提供方文档保留原始单位
