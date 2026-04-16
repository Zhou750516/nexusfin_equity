# Contract Notes: Page Flow APIs

本文件记录 `004-benefit-loan-flow` 的页面 API 范围边界。

## Current Status

基于 `2026-04-10` 技术方案调整，所有用户可见 H5 页面统一由艾博生提供；云卡不再承接用户页面，只承担 gateway 服务。

当前实现切片**仍没有新增页面 HTTP API**，但页面职责边界已更新。

原因：

- 需要先统一页面归属与外部调用边界，再恢复页面 API 实现。
- 当前后端优先固化“艾博生 -> 云卡 gateway”契约，而不是继续沿旧边界实现页面接口。

## Planned Page APIs

后续计划中的页面 API 包括：

- 艾博生权益页：
  - `/api/equity/info`
  - `/api/equity/rules`
  - `/api/users/payment-card`
- 艾博生借款确认/状态：
  - `/api/loan/trial`
  - `/api/loan/status`
  - `/api/users/receiving-card`
- 艾博生还款：
  - `/api/repayment/trial`
  - 还款确认接口

## Gateway Boundary

页面层不直接调用科技平台；后续服务端统一通过云卡 gateway 获取：

- 联合登录用户校验
- 授信 / 审批结果
- 借款试算 / 确认 / 放款结果
- 已绑卡列表 / 还款试算 / 还款结果

对应业务接口文档见：

- `docs/third-part/云卡/20260410_艾博生调用云卡接口文档.md`

## Contract Rules

- 统一使用 `Result<T>`
- 保持 versionless path
- 写接口使用 `@Valid`
- 内部金额字段优先 `Long` 分；外部第三方契约字段按提供方文档保留原始单位
