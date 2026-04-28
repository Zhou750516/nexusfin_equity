# Data Model: 惠选卡权益与借款流程 V1 页面交互理解

## 1. TechPlatformProperties

表示科技平台 outbound client 的运行配置。

### Key Fields

- `enabled`: 是否启用科技平台集成
- `mode`: `MOCK` / `HTTP`
- `baseUrl`: 平台基础地址
- `channelId`: 请求头渠道号
- `version`: 请求头版本
- `signSecret`: 签名密钥
- `signAlgorithm`: 签名算法
- `aesKeyBase64`: 加密密钥
- `aesAlgorithm`: AES 模式
- `paths.creditStatusNotice`
- `paths.loanInfoNotice`
- `paths.repayInfoNotice`

## 2. TechPlatformRequestEnvelope

表示对科技平台发起请求时的统一报文外壳。

### Key Fields

- `param`: 业务报文加密后的字符串
- 请求头：
  - `channelId`
  - `timestamp`
  - `sign`
  - `version`

## 3. TechPlatformNotifyResponse

表示通知类接口的统一响应。

### Key Fields

- `code`: 平台处理结果码
- `msg`: 平台处理描述

## 4. CreditStatusNoticeRequest

表示授信/进件结果通知报文。

### Key Fields

- `orderId`
- `userId`
- `code`
- `msg`
- `approveTime`
- `periods`
- `periodUnit`
- `repaymentMethod`
- `loanOption`
- `creditLimit`

## 5. LoanInfoNoticeRequest

表示放款结果通知报文。

### Key Fields

- `orderId`
- `loanNo`
- `loanAmount`
- `period`
- `loanBeginDate`
- `loanEndDate`
- `orderStatus`
- `loanDate`
- `contractNo`
- `loanBankCard`
- `loanBank`
- `reason`
- `repayDate`
- `outAccountDays`
- `isRepayInfo`
- `isFirstLoan`

## 6. RepayInfoNoticeRequest

表示还款结果通知报文。

### Key Fields

- `repayList`

### Nested Entity: RepayItem

- `orderId`
- `repayNo`
- `period`
- `repayType`
- `repayTime`
- `repayStatus`
- `remark`
- `overDueDays`
- `shouldRepayAmount`
- `repayedAmount`
- `repayOverDueFee`
- `dueOverDueFee`
- `repayPrincipal`
- `duePrincipal`
- `repayInterest`
- `dueRepayInterest`
- `repayTotalFee`
- `dueRepayTotalFee`
- `feePlan`
- `repayBankCard`
- `repayBank`

### Nested Entity: FeePlanItem

- `feeCode`
- `feeType`
- `dueRepayAmt`
- `repayAmt`

## 7. Current Boundaries

- 本轮没有新增数据库实体或表结构。
- 金额字段仅在科技平台边界保留 `BigDecimal` 元，不进入本地持久化。
- 页面视图实体和内部业务实体在本轮尚未进入实现阶段，因此只保留在 spec 中，不在本数据模型中展开。
