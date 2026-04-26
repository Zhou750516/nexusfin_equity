# Contract Notes: Yunka / Xiaohua Gateway Boundary

本文件记录当前实现中 **艾博生 -> 云卡 -> 小花** 的出站能力，以及 **小花 -> 云卡 -> 艾博生** 的回调入口边界。

## Current Implemented Slice

当前代码已形成两层能力：

1. **Yunka 基础网关调用**
   - 借款试算 / 申请 / 查询
   - 还款试算 / 提交 / 查询
2. **Xiaohua-via-Yunka typed facade**
   - 动态协议
   - 用户查询
   - 绑卡列表
   - 还款计划
   - 短信发送 / 确认
   - 图片查询
   - 权益同步

核心入口：

- `src/main/java/com/nexusfin/equity/service/XiaohuaGatewayService.java`
- `src/main/java/com/nexusfin/equity/service/impl/XiaohuaGatewayServiceImpl.java`
- `src/main/java/com/nexusfin/equity/config/YunkaProperties.java`

## Outbound Capability Matrix

### Shared Gateway Paths

当前 `YunkaProperties.Paths` 已覆盖以下路径：

| Capability | Path Key |
| --- | --- |
| 借款试算 | `loanCalculate` |
| 放款查询 | `loanQuery` |
| 借款申请 | `loanApply` |
| 还款试算 | `repayTrial` |
| 主动还款 | `repayApply` |
| 还款结果查询 | `repayQuery` |
| 动态协议 | `protocolQuery` |
| 用户鉴权 | `userToken` |
| 用户资料查询 | `userQuery` |
| 还款计划查询 | `loanRepayPlan` |
| 短信发送 | `cardSmsSend` |
| 短信确认 | `cardSmsConfirm` |
| 已绑卡列表 | `cardUserCards` |
| 图片查询 | `creditImageQuery` |
| 权益同步 | `benefitSync` |

### Typed Models

当前已为以下能力建立独立 request/response 模型：

- `ProtocolQueryRequest` / `ProtocolQueryResponse`
- `UserTokenRequest` / `UserTokenResponse`
- `UserQueryRequest` / `UserQueryResponse`
- `UserCardListRequest` / `UserCardListResponse`
- `LoanRepayPlanRequest` / `LoanRepayPlanResponse`
- `CardSmsSendRequest` / `CardSmsSendResponse`
- `CardSmsConfirmRequest` / `CardSmsConfirmResponse`
- `CreditImageQueryRequest` / `CreditImageQueryResponse`
- `BenefitOrderSyncRequest` / `BenefitOrderSyncResponse`

## Inbound Callback Boundary

当前回调入口为 Yunka forward 语义，而不是小花直接打到艾博生：

| Callback | Local Endpoint | Local DTO |
| --- | --- | --- |
| 放款结果通知 | `/api/callbacks/grant/forward` | `LoanResultCallbackRequest` |
| 还款结果通知 | `/api/callbacks/repayment/forward` | `RepaymentResultCallbackRequest` |

处理原则：

- 回调以 `bizOrderNo` 做本地业务主键
- 维持幂等消费
- 统一记录 `traceId + bizOrderNo + requestId`
- 兼容 Yunka 转发后的字段语义，不要求 H5 感知原始回调结构

## Mapping Decisions

### 放款状态

当前明确采用：

- `7001 = success`
- `7002 = processing`
- `7003 = failure`

这是本轮对齐中的关键修正点，避免把 `7003` 继续误读成成功。

### 还款结果

当前本地服务对外统一收敛为：

- `pending`
- `success`
- `failure`

并保留 `swiftNumber`、银行卡上下文、成功时间等 H5 需要的补充字段。

## Deferred / External Dependencies

以下内容仍不在本轮硬编码落地范围内：

1. “云卡先建放款订单 -> 权益成功 -> 触发小花放款”的最终主链重排
2. Yunka 内部状态机与 H5 页面阶段的最终枚举定稿
3. `2.15` 权益同步真实 path 与最终字段要求
4. Yunka 是否对小花回调增加统一 envelope 的最终联调口径

对应方案与待确认项见：

- `docs/plan/20260426_用信新链路改造方案.md`
- `docs/plan/20260426_云卡状态查询与H5状态映射方案.md`
- `docs/plan/20260426_外部待确认项与本侧可执行项拆分.md`
