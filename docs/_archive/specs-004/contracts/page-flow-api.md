# Contract Notes: Page Flow APIs

本文件记录 `004-benefit-loan-flow` 当前 **H5 对艾博生后端** 的页面接口口径，以及已落地的字段边界。

## Current Status

当前实现不是“新增一套页面 API”，而是在既有 H5 接口形状上，逐步对齐“小花 `4.22` + 艾博生 -> 云卡 -> 小花”的最新边界。

已落地原则：

- H5 仍只调用艾博生后端接口
- Controller 统一返回 `Result<T>`
- 页面接口保持当前 versionless path，不额外引入 `/v1`
- 页面层不直接感知云卡 / 小花的原始报文

## Implemented Page APIs

### 权益页

| Endpoint | Method | Purpose | Current Notes |
| --- | --- | --- | --- |
| `/api/benefits/card-detail` | `GET` | 查询权益卡详情 | 返回本地展示配置 + 动态协议列表 + 用户绑卡摘要 + `protocolReady` |
| `/api/benefits/activate` | `POST` | 开通权益 | 校验卡类型、协议 readiness，并在成功后触发权益订单同步 |

`/api/benefits/card-detail` 当前已聚合：

- 本地权益文案与价格配置
- `protocol/queryProtocolAggregationLink` 动态协议
- `card/userCards` 用户绑卡列表
- 后端协议 readiness 判断结果

### 借款页

| Endpoint | Method | Purpose | Current Notes |
| --- | --- | --- | --- |
| `/api/loan/calculator-config` | `GET` | 获取试算配置 | 保持 H5 当前试算页所需配置形状 |
| `/api/loan/calculate` | `POST` | 借款试算 | 通过 Yunka 调用借款试算并回填 H5 结果 |
| `/api/loan/apply` | `POST` | 提交借款申请 | 保持现有 H5 入口，已扩展 richer Xiaohua fields |
| `/api/loan/approval-status/{applicationId}` | `GET` | 查询审批中页状态 | 映射 Yunka / Xiaohua 状态为 H5 页面阶段 |
| `/api/loan/approval-result/{applicationId}` | `GET` | 查询审批结果页 | 已按 `7001/7002/7003` 重新映射，并在成功时可补 repayment plan |

`/api/loan/apply` 当前已支持的扩展字段包括：

- `loanReason`
- `bankCardNum`
- `basicInfo`
- `idInfo`
- `contactInfo`
- `supplementInfo`
- `optionInfo`
- `imageInfo`
- `platformBenefitOrderNo`

### 还款页

| Endpoint | Method | Purpose | Current Notes |
| --- | --- | --- | --- |
| `/api/repayment/info/{loanId}` | `GET` | 查询还款信息 | 返回应还金额、绑卡信息、短信确认所需上下文 |
| `/api/repayment/sms-send` | `POST` | 发送短信验证码 | 对接 `card/smsSend` |
| `/api/repayment/sms-confirm` | `POST` | 确认短信验证码 | 对接 `card/smsConfirm` |
| `/api/repayment/submit` | `POST` | 发起主动还款 | 保持 H5 还款确认入口 |
| `/api/repayment/result/{repaymentId}` | `GET` | 查询还款结果 | 对齐 `swiftNumber`、卡上下文、pending/success/failure 语义 |

## H5-Facing Mapping Rules

### 借款状态映射

当前后端已按小花 `4.22` 语义对齐：

| Upstream Status | Meaning | H5 Interpretation |
| --- | --- | --- |
| `7001` | 放款成功 | success / approved |
| `7002` | 放款中 | processing |
| `7003` | 放款失败 | failure / rejected |

### 还款状态映射

当前后端对 H5 暴露统一语义：

- `pending`
- `success`
- `failure`

其中会兼容 Yunka / Xiaohua 不同返回字段名和状态码口径。

## Callback Entry APIs

虽然以下接口不是 H5 页面入口，但与页面最终状态闭环直接相关：

| Endpoint | Method | Purpose |
| --- | --- | --- |
| `/api/callbacks/grant/forward` | `POST` | 接收 Yunka 转发的小花放款结果通知 |
| `/api/callbacks/repayment/forward` | `POST` | 接收 Yunka 转发的小花还款结果通知 |

回调处理要求：

- 保持幂等
- 关键日志打印 `traceId + bizOrderNo + requestId`
- 不把 Yunka 原始 envelope 直接泄漏给 H5

## Boundary Notes

当前有意 **未** 在本轮中直接改造以下高返工项：

- `/api/loan/apply` 重排为“云卡先建放款订单”的最终主链
- 云卡状态机真实接口定稿前的状态流硬编码
- 法大大真实签章接入
- H5 自动埋点全量代码改造

这些内容以 `docs/plan/20260426_*.md` 方案文档继续承接。
