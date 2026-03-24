# NexusFin Equity 标准接口文档

- 生成日期: `2026-03-23`
- 代码基线: `main` 分支当前最新代码
- 服务名称: `nexusfin-equity`
- 默认端口: `8080`
- Base URL: `http://{host}:8080`

## 1. 文档范围

本文档基于当前代码中的 Controller、DTO、异常处理和服务实现整理，覆盖以下已开放接口：

1. 用户静默注册
2. 产品页查询
3. 权益订单创建
4. 权益订单状态查询
5. 行权链接获取
6. 首扣回调
7. 兜底代扣回调
8. 行权结果回调
9. 退款结果回调
10. 放款结果回调
11. 还款状态回调
12. 健康检查

## 2. 通用约定

### 2.1 请求格式

- 请求体格式: `application/json`
- 字符编码: `UTF-8`
- 金额字段: `Long`，单位为`分`
- 时间字段: 当前代码多数按 `String` 接收，建议调用方统一传 ISO-8601 格式，例如 `2026-03-23T20:00:00`

### 2.2 统一响应结构

所有接口统一返回 `Result<T>`：

```json
{
  "code": 0,
  "message": "OK",
  "data": {}
}
```

字段说明：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `code` | `int` | `0` 表示成功，非 `0` 表示失败 |
| `message` | `string` | 成功固定为 `OK`；失败时返回错误信息 |
| `data` | `object/null` | 业务响应体，部分回调接口成功时为 `null` |

### 2.3 错误响应规则

当前代码中主要有三类错误：

| 场景 | `code` | `message` 示例 |
| --- | --- | --- |
| 业务异常 `BizException(String code, String message)` | `-1` | `PRODUCT_NOT_FOUND:Benefit product not found` |
| 参数校验失败 | `400` | `requestId must not be blank` |
| 路径不存在 | `404` | Spring 默认资源不存在消息 |
| 未处理系统异常 | `500` | 异常原始消息 |

失败示例：

```json
{
  "code": -1,
  "message": "ORDER_NOT_FOUND:Benefit order not found",
  "data": null
}
```

### 2.4 签名校验规则

当前代码只对以下路径做签名校验：

- `POST /api/users/register`
- 所有 `/api/callbacks/**` 接口

以下接口当前不做签名校验：

- `/api/equity/products/**`
- `/api/equity/orders/**`
- `/api/equity/exercise-url/**`
- `/api/equity/health`

签名请求头：

| Header | 必填 | 说明 |
| --- | --- | --- |
| `X-App-Id` | 是 | 应用标识，需与服务端配置一致 |
| `X-Timestamp` | 是 | 秒级 Unix 时间戳 |
| `X-Nonce` | 是 | 随机串，建议每次请求唯一 |
| `X-Signature` | 是 | HMAC-SHA256 十六进制小写签名值 |

签名算法：

```text
payload = appId + "|" + timestamp + "|" + nonce
signature = HmacSHA256(secret, payload).hexLowerCase()
```

签名配置来源：

- `nexusfin.signature.app-id`
- `nexusfin.signature.secret`
- `nexusfin.signature.max-skew-seconds`，默认 `300`

校验失败时可能返回：

- `SIGNATURE_MISSING:Missing signature headers`
- `SIGNATURE_INVALID:Invalid app id`
- `SIGNATURE_INVALID:Invalid signature`
- `SIGNATURE_EXPIRED:Signature timestamp expired`

### 2.5 幂等约定

当前代码中，以下接口使用 `requestId` 做幂等控制：

| 接口 | 幂等行为 |
| --- | --- |
| 用户注册 | 同一 `requestId` 或相同 `channelCode + externalUserId` 重复调用时，返回已有会员结果 |
| 订单创建 | 同一 `requestId` 重复调用时，返回同一 `benefitOrderNo` |
| 首扣/兜底代扣回调 | 同一 `requestId` 重复回调时，返回已有支付记录 |
| 通知类回调 | 同一 `requestId` 重复回调时，直接忽略重复处理 |

## 3. 状态枚举

### 3.1 订单状态 `orderStatus`

| 枚举值 | 说明 |
| --- | --- |
| `AGREEMENT_PENDING` | 协议待完成 |
| `FIRST_DEDUCT_PENDING` | 首扣待处理 |
| `FIRST_DEDUCT_SUCCESS` | 首扣成功 |
| `FIRST_DEDUCT_FAIL` | 首扣失败 |
| `FALLBACK_DEDUCT_PENDING` | 兜底代扣待处理 |
| `FALLBACK_DEDUCT_SUCCESS` | 兜底代扣成功 |
| `FALLBACK_DEDUCT_FAIL` | 兜底代扣失败 |
| `EXERCISE_PENDING` | 行权处理中 |
| `EXERCISE_SUCCESS` | 行权成功 |
| `EXERCISE_FAIL` | 行权失败 |
| `REFUND_SUCCESS` | 退款成功 |
| `REFUND_FAIL` | 退款失败 |
| `SYNC_PENDING` | 下游同步待处理 |
| `SYNC_SUCCESS` | 下游同步成功 |
| `SYNC_FAIL` | 下游同步失败 |

### 3.2 支付状态

| 枚举值 | 说明 |
| --- | --- |
| `NONE` | 未发生 |
| `PENDING` | 待处理 |
| `SUCCESS` | 成功 |
| `FAIL` | 失败 |

### 3.3 支付类型 `paymentType`

| 枚举值 | 说明 |
| --- | --- |
| `FIRST_DEDUCT` | 首扣 |
| `FALLBACK_DEDUCT` | 兜底代扣 |

### 3.4 其他常见状态

| 字段 | 可见值 | 说明 |
| --- | --- | --- |
| `registerStatus` | `SUCCESS` / `DUPLICATE` | 注册结果 |
| `grantStatus` | `PENDING` / `SUCCESS` / `FAIL` | 放款状态 |
| `processStatus` | `RECEIVED` / `PROCESSED` / `FAILED` | 通知日志处理状态 |

## 4. 接口清单

---

## 4.1 用户静默注册

- 接口名称: 用户静默注册
- 请求方式: `POST`
- 请求路径: `/api/users/register`
- 是否验签: 是

### 请求体

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `requestId` | `string` | 是 | 请求唯一标识，幂等键 |
| `channelCode` | `string` | 是 | 渠道编码 |
| `userInfo` | `object` | 是 | 用户信息 |
| `userInfo.externalUserId` | `string` | 是 | 渠道侧外部用户 ID |
| `userInfo.mobileEncrypted` | `string` | 是 | 手机号密文或上游加密值 |
| `userInfo.idCardEncrypted` | `string` | 是 | 身份证号密文或上游加密值 |
| `userInfo.realNameEncrypted` | `string` | 是 | 姓名密文或上游加密值 |

### 请求示例

```json
{
  "requestId": "req-register-001",
  "channelCode": "KJ",
  "userInfo": {
    "externalUserId": "user-10001",
    "mobileEncrypted": "13600000000",
    "idCardEncrypted": "310101199001011236",
    "realNameEncrypted": "李四"
  }
}
```

### 成功响应

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `memberId` | `string` | 会员 ID |
| `registerStatus` | `string` | `SUCCESS` 或 `DUPLICATE` |

### 成功响应示例

```json
{
  "code": 0,
  "message": "OK",
  "data": {
    "memberId": "mem202603230001",
    "registerStatus": "SUCCESS"
  }
}
```

### 业务说明

- 同一 `channelCode + externalUserId` 重复注册会返回已存在会员。
- 同一 `requestId` 重复调用会回放已处理结果。
- 敏感信息入库时会加密存储，并生成 hash 用于归并查询。

---

## 4.2 产品页查询

- 接口名称: 产品页查询
- 请求方式: `GET`
- 请求路径: `/api/equity/products/{productCode}`
- 是否验签: 否

### 路径参数

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `productCode` | `string` | 是 | 产品编码 |

### Query 参数

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `memberId` | `string` | 否 | 会员 ID，用于回显会员信息 |

### 成功响应

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `productCode` | `string` | 产品编码 |
| `productName` | `string` | 产品名称 |
| `feeRate` | `int` | 费率 |
| `feeAmount` | `long/null` | 当前代码固定返回 `null` |
| `loanAmount` | `long/null` | 当前代码固定返回 `null` |
| `agreements` | `string[]` | 当前固定返回 `EQUITY_AGREEMENT`、`DEFERRED_AGREEMENT` |
| `memberId` | `string/null` | 若传入有效 `memberId` 则回显 |
| `externalUserId` | `string/null` | 若传入有效 `memberId` 则回显 |

### 成功响应示例

```json
{
  "code": 0,
  "message": "OK",
  "data": {
    "productCode": "P-001",
    "productName": "权益产品-P-001",
    "feeRate": 299,
    "feeAmount": null,
    "loanAmount": null,
    "agreements": [
      "EQUITY_AGREEMENT",
      "DEFERRED_AGREEMENT"
    ],
    "memberId": "mem-001",
    "externalUserId": "user-product"
  }
}
```

### 业务说明

- 产品不存在或状态非 `ACTIVE` 时返回 `PRODUCT_NOT_FOUND`。
- 当前实现为展示聚合接口，不计算实际费用金额。

---

## 4.3 权益订单创建

- 接口名称: 权益订单创建
- 请求方式: `POST`
- 请求路径: `/api/equity/orders`
- 是否验签: 否

### 请求体

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `requestId` | `string` | 是 | 请求唯一标识，幂等键 |
| `memberId` | `string` | 是 | 会员 ID |
| `productCode` | `string` | 是 | 产品编码 |
| `loanAmount` | `long` | 是 | 借款金额，单位分，必须大于 0 |
| `agreementSigned` | `boolean` | 是 | 是否已完成协议签署 |

### 请求示例

```json
{
  "requestId": "req-order-create-001",
  "memberId": "mem-001",
  "productCode": "P-002",
  "loanAmount": 800000,
  "agreementSigned": true
}
```

### 成功响应

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `benefitOrderNo` | `string` | 权益订单号 |
| `orderStatus` | `string` | 初始为 `FIRST_DEDUCT_PENDING` |
| `redirectUrl` | `string` | H5 跳转地址，当前返回相对路径 |

### 成功响应示例

```json
{
  "code": 0,
  "message": "OK",
  "data": {
    "benefitOrderNo": "ord202603230001",
    "orderStatus": "FIRST_DEDUCT_PENDING",
    "redirectUrl": "/h5/equity/orders/ord202603230001"
  }
}
```

### 业务说明

- 若 `agreementSigned=false`，返回 `AGREEMENT_REQUIRED`。
- 同一 `requestId` 重复提交时，返回同一个 `benefitOrderNo`。
- 创建成功后会自动补齐签约任务和协议归档数据。
- 订单初始状态：
  - `orderStatus = FIRST_DEDUCT_PENDING`
  - `qwFirstDeductStatus = PENDING`
  - `qwFallbackDeductStatus = NONE`
  - `qwExerciseStatus = NONE`
  - `grantStatus = PENDING`

---

## 4.4 权益订单状态查询

- 接口名称: 权益订单状态查询
- 请求方式: `GET`
- 请求路径: `/api/equity/orders/{benefitOrderNo}`
- 是否验签: 否

### 路径参数

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `benefitOrderNo` | `string` | 是 | 权益订单号 |

### 成功响应

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `benefitOrderNo` | `string` | 权益订单号 |
| `orderStatus` | `string` | 订单主状态 |
| `qwFirstDeductStatus` | `string` | 首扣状态 |
| `qwFallbackDeductStatus` | `string` | 兜底代扣状态 |
| `qwExerciseStatus` | `string` | 行权状态 |
| `grantStatus` | `string` | 放款状态 |

### 成功响应示例

```json
{
  "code": 0,
  "message": "OK",
  "data": {
    "benefitOrderNo": "ord202603230001",
    "orderStatus": "FIRST_DEDUCT_PENDING",
    "qwFirstDeductStatus": "PENDING",
    "qwFallbackDeductStatus": "NONE",
    "qwExerciseStatus": "NONE",
    "grantStatus": "PENDING"
  }
}
```

### 业务说明

- 订单不存在时返回 `ORDER_NOT_FOUND`。

---

## 4.5 行权链接获取

- 接口名称: 行权链接获取
- 请求方式: `GET`
- 请求路径: `/api/equity/exercise-url/{benefitOrderNo}`
- 是否验签: 否

### 路径参数

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `benefitOrderNo` | `string` | 是 | 权益订单号 |

### 成功响应

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `exerciseUrl` | `string` | 当前返回示例域名拼接的行权地址 |
| `expireTime` | `string` | 过期时间，当前为“当前时间 + 1 天” |

### 成功响应示例

```json
{
  "code": 0,
  "message": "OK",
  "data": {
    "exerciseUrl": "https://abs.example.com/exercise/ord202603230001",
    "expireTime": "2026-03-24T20:00:00"
  }
}
```

### 业务说明

- 订单不存在时返回 `ORDER_NOT_FOUND`。
- 当前链接未带签名 token，仅为代码现状说明。

---

## 4.6 首扣回调

- 接口名称: 首扣回调
- 请求方式: `POST`
- 请求路径: `/api/callbacks/first-deduction`
- 是否验签: 是

### 请求体

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `requestId` | `string` | 是 | 回调唯一标识，幂等键 |
| `benefitOrderNo` | `string` | 是 | 权益订单号 |
| `qwTradeNo` | `string` | 是 | 渠道交易流水号 |
| `deductStatus` | `string` | 是 | 扣款结果，代码按 `SUCCESS/其他` 识别 |
| `deductAmount` | `long` | 是 | 扣款金额，单位分，必须大于 0 |
| `failReason` | `string` | 否 | 失败原因 |
| `deductTime` | `string` | 否 | 扣款时间，建议 ISO-8601 |

### 请求示例

```json
{
  "requestId": "req-first-success-001",
  "benefitOrderNo": "ord-first-success",
  "qwTradeNo": "qw-req-first-success-001",
  "deductStatus": "SUCCESS",
  "deductAmount": 680000,
  "failReason": null,
  "deductTime": "2026-03-23T20:00:00"
}
```

### 成功响应

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `paymentNo` | `string` | 支付流水号 |
| `benefitOrderNo` | `string` | 权益订单号 |
| `paymentType` | `string` | 固定为 `FIRST_DEDUCT` |
| `paymentStatus` | `string` | `SUCCESS` 或 `FAIL` |
| `failReason` | `string/null` | 失败原因 |

### 成功响应示例

```json
{
  "code": 0,
  "message": "OK",
  "data": {
    "paymentNo": "pay202603230001",
    "benefitOrderNo": "ord-first-success",
    "paymentType": "FIRST_DEDUCT",
    "paymentStatus": "SUCCESS",
    "failReason": null
  }
}
```

### 业务说明

- 同一 `requestId` 重复回调时，返回第一次生成的支付记录。
- 扣款成功后：
  - `orderStatus = FIRST_DEDUCT_SUCCESS`
  - `qwFirstDeductStatus = SUCCESS`
  - `syncStatus = SYNC_SUCCESS`
- 扣款失败后：
  - `orderStatus = FIRST_DEDUCT_FAIL`
  - `qwFirstDeductStatus = FAIL`
  - `syncStatus = SYNC_PENDING`
- 订单不存在时返回 `ORDER_NOT_FOUND`。

---

## 4.7 兜底代扣回调

- 接口名称: 兜底代扣回调
- 请求方式: `POST`
- 请求路径: `/api/callbacks/fallback-deduction`
- 是否验签: 是

### 请求体

请求字段与首扣回调一致。

### 成功响应

返回结构与首扣回调一致，`paymentType` 固定为 `FALLBACK_DEDUCT`。

### 成功响应示例

```json
{
  "code": 0,
  "message": "OK",
  "data": {
    "paymentNo": "pay202603230002",
    "benefitOrderNo": "ord-fallback-success",
    "paymentType": "FALLBACK_DEDUCT",
    "paymentStatus": "SUCCESS",
    "failReason": null
  }
}
```

### 业务说明

- 成功后：
  - `orderStatus = FALLBACK_DEDUCT_SUCCESS`
  - `qwFallbackDeductStatus = SUCCESS`
- 失败后：
  - `orderStatus = FALLBACK_DEDUCT_FAIL`
  - `qwFallbackDeductStatus = FAIL`

---

## 4.8 行权结果回调

- 接口名称: 行权结果回调
- 请求方式: `POST`
- 请求路径: `/api/callbacks/exercise-equity`
- 是否验签: 是

### 请求体

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `requestId` | `string` | 是 | 回调唯一标识，幂等键 |
| `benefitOrderNo` | `string` | 是 | 权益订单号 |
| `exerciseStatus` | `string` | 是 | 行权结果，代码按 `SUCCESS/其他` 识别 |
| `exerciseTime` | `string` | 否 | 行权时间 |
| `exerciseDetail` | `string` | 否 | 行权说明 |

### 请求示例

```json
{
  "requestId": "req-exercise-001",
  "benefitOrderNo": "ord-exercise",
  "exerciseStatus": "SUCCESS",
  "exerciseTime": "2026-03-23T20:13:00",
  "exerciseDetail": "exercise ok"
}
```

### 成功响应示例

```json
{
  "code": 0,
  "message": "OK",
  "data": null
}
```

### 业务说明

- 回调日志先记为 `RECEIVED`，处理完成后更新为 `PROCESSED` 或 `FAILED`。
- 行权成功后：
  - `orderStatus = EXERCISE_SUCCESS`
  - `qwExerciseStatus = SUCCESS`
- 行权失败后：
  - `orderStatus = EXERCISE_FAIL`
  - `qwExerciseStatus = FAIL`
- 若订单不存在，接口仍返回成功响应，但内部通知日志会标记为 `FAILED`，且不会写入幂等成功记录。

---

## 4.9 退款结果回调

- 接口名称: 退款结果回调
- 请求方式: `POST`
- 请求路径: `/api/callbacks/refund`
- 是否验签: 是

### 请求体

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `requestId` | `string` | 是 | 回调唯一标识，幂等键 |
| `benefitOrderNo` | `string` | 是 | 权益订单号 |
| `refundStatus` | `string` | 是 | 退款结果，代码按 `SUCCESS/其他` 识别 |
| `refundAmount` | `long` | 是 | 退款金额，单位分，必须大于 0 |
| `refundTime` | `string` | 否 | 退款时间 |
| `refundReason` | `string` | 否 | 退款原因 |

### 请求示例

```json
{
  "requestId": "req-refund-001",
  "benefitOrderNo": "ord-refund",
  "refundStatus": "SUCCESS",
  "refundAmount": 680000,
  "refundTime": "2026-03-23T20:14:00",
  "refundReason": "manual"
}
```

### 成功响应示例

```json
{
  "code": 0,
  "message": "OK",
  "data": null
}
```

### 业务说明

- 退款成功后订单状态更新为 `REFUND_SUCCESS`。
- 退款失败后订单状态更新为 `REFUND_FAIL`。
- 若订单不存在，接口仍返回成功响应，但通知日志标记为 `FAILED`。

---

## 4.10 放款结果回调

- 接口名称: 放款结果回调
- 请求方式: `POST`
- 请求路径: `/api/callbacks/grant/forward`
- 是否验签: 是

### 请求体

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `requestId` | `string` | 是 | 回调唯一标识，幂等键 |
| `benefitOrderNo` | `string` | 是 | 权益订单号 |
| `grantStatus` | `string` | 是 | 放款结果，代码按 `SUCCESS/其他` 识别 |
| `actualAmount` | `long` | 是 | 实际放款金额，单位分，允许 `0` |
| `loanOrderNo` | `string` | 否 | 下游借据号 |
| `failReason` | `string` | 否 | 失败原因 |
| `grantTime` | `string` | 否 | 放款时间 |
| `timestamp` | `long` | 是 | 业务时间戳 |

### 请求示例

```json
{
  "requestId": "req-grant-success-001",
  "benefitOrderNo": "ord-grant-fallback",
  "grantStatus": "SUCCESS",
  "actualAmount": 680000,
  "loanOrderNo": "loan-req-grant-success-001",
  "failReason": null,
  "grantTime": "2026-03-23T20:10:00",
  "timestamp": 1711195800
}
```

### 成功响应示例

```json
{
  "code": 0,
  "message": "OK",
  "data": null
}
```

### 业务说明

- 回调成功处理后会更新：
  - `grantStatus`
  - `loanOrderNo`
- 当 `grantStatus=SUCCESS` 且订单当前为 `FIRST_DEDUCT_SUCCESS` 时，订单进入 `EXERCISE_PENDING`。
- 当 `grantStatus=SUCCESS` 且订单当前为 `FIRST_DEDUCT_FAIL` 时，会自动触发一次兜底代扣：
  - 新增一笔 `FALLBACK_DEDUCT` 支付记录，状态为 `PENDING`
  - 订单更新为 `FALLBACK_DEDUCT_PENDING`
- 若订单不存在，接口仍返回成功响应，但通知日志标记为 `FAILED`。

---

## 4.11 还款状态回调

- 接口名称: 还款状态回调
- 请求方式: `POST`
- 请求路径: `/api/callbacks/repayment/forward`
- 是否验签: 是

### 请求体

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `requestId` | `string` | 是 | 回调唯一标识，幂等键 |
| `benefitOrderNo` | `string` | 是 | 权益订单号 |
| `loanOrderNo` | `string` | 是 | 下游借据号 |
| `termNo` | `int` | 是 | 期次，必须大于 0 |
| `repaymentStatus` | `string` | 是 | 还款状态 |
| `paidAmount` | `long` | 否 | 已还金额，允许 `0` |
| `paidTime` | `string` | 否 | 还款时间 |
| `overdueDays` | `int` | 否 | 逾期天数，允许 `0` |
| `timestamp` | `long` | 是 | 业务时间戳 |

### 请求示例

```json
{
  "requestId": "req-repayment-001",
  "benefitOrderNo": "ord-repayment",
  "loanOrderNo": "loan-repayment",
  "termNo": 1,
  "repaymentStatus": "PAID",
  "paidAmount": 680000,
  "paidTime": "2026-03-23T20:12:00",
  "overdueDays": 0,
  "timestamp": 1711195920
}
```

### 成功响应示例

```json
{
  "code": 0,
  "message": "OK",
  "data": null
}
```

### 业务说明

- 当前实现只记录通知日志和幂等记录，不直接修改 `benefit_order.order_status`。
- 若订单不存在，接口仍返回成功响应，但通知日志标记为 `FAILED`，且不会写入幂等成功记录。

---

## 4.12 健康检查

- 接口名称: 健康检查
- 请求方式: `GET`
- 请求路径: `/api/equity/health`
- 是否验签: 否

### 成功响应

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `service` | `string` | 服务名，固定为 `nexusfin-equity` |
| `status` | `string` | 固定为 `UP` |
| `timestamp` | `string` | 当前时间戳 |

### 成功响应示例

```json
{
  "code": 0,
  "message": "OK",
  "data": {
    "service": "nexusfin-equity",
    "status": "UP",
    "timestamp": "2026-03-23T20:15:00Z"
  }
}
```

## 5. 典型链路说明

### 5.1 主流程

1. 调用`用户静默注册`拿到 `memberId`
2. 调用`产品页查询`
3. 调用`权益订单创建`生成 `benefitOrderNo`
4. 由渠道回调`首扣结果`
5. 查询`权益订单状态`
6. 若已放款成功且进入行权阶段，可调用`行权链接获取`
7. 后续接收放款、还款、行权、退款等回调

### 5.2 当前代码中的关键状态迁移

| 触发事件 | 订单状态变化 |
| --- | --- |
| 创建订单 | `FIRST_DEDUCT_PENDING` |
| 首扣成功 | `FIRST_DEDUCT_SUCCESS` |
| 首扣失败 | `FIRST_DEDUCT_FAIL` |
| 放款成功且原状态为首扣成功 | `EXERCISE_PENDING` |
| 放款成功且原状态为首扣失败 | `FALLBACK_DEDUCT_PENDING` |
| 兜底代扣成功 | `FALLBACK_DEDUCT_SUCCESS` |
| 兜底代扣失败 | `FALLBACK_DEDUCT_FAIL` |
| 行权成功 | `EXERCISE_SUCCESS` |
| 行权失败 | `EXERCISE_FAIL` |
| 退款成功 | `REFUND_SUCCESS` |
| 退款失败 | `REFUND_FAIL` |

## 6. 说明

- 本文档按当前代码真实行为整理，不额外引入尚未落地的设计假设。
- 若后续新增 OpenAPI/Swagger，可基于本文档进一步生成机器可读接口规范。
- 当前代码已覆盖 H2 与 MySQL 回归场景，接口文档中的字段和值优先以代码行为为准。
