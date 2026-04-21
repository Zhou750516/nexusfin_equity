# API_SCHEMA.md — Java 后端 REST API 规范

> 本文件定义 H5 前端所需的全部后端接口。Java 后端使用 Spring Boot，接口路径以 `/api` 为前缀。
> H5 前端开发环境通过 Vite proxy 将 `/api/*` 转发到 `http://localhost:8080`。

## 全局约定

```
Base URL: /api
Content-Type: application/json
认证: Authorization: Bearer <JWT token>（/api/auth/* 除外）

成功响应:
{
  "code": 0,
  "data": { ... }
}

错误响应:
{
  "code": <错误码>,
  "data": null,
  "message": "<中文错误描述>"
}

HTTP 状态码:
  200 → 成功
  400 → 参数校验失败
  401 → 未认证（token 缺失或过期）
  403 → 权限不足
  404 → 资源不存在
  409 → 冲突（重复操作）
  429 → 请求频率超限
  500 → 服务器内部错误
```

## 补充接口：H5 绑卡页接口

> 绑卡页当前尚未进入 H5 页面开发，但后端已预留接口。详细接入说明见：
> `docs/plan/20260421_H5绑卡页接入后端接口说明.md`
> 当前已实现后端使用 `NEXUSFIN_AUTH` Cookie 识别登录态，H5 不需要自行拼接齐为四要素。

| H5 场景 | 方法 | 接口 | 说明 |
|---|---|---|---|
| 查询银行卡是否已签约 | `GET` | `/api/bank-card/sign-status` | 传 `accountNo` |
| 申请绑卡短信 | `POST` | `/api/bank-card/sign-apply` | 传 `accountNo` |
| 确认绑卡签约 | `POST` | `/api/bank-card/sign-confirm` | 传 `accountNo`、`verificationCode` |

---

**Java 统一响应封装建议：**

```java
@Data
public class ApiResult<T> {
    private int code;
    private T data;
    private String message;

    public static <T> ApiResult<T> success(T data) {
        ApiResult<T> result = new ApiResult<>();
        result.setCode(0);
        result.setData(data);
        return result;
    }

    public static <T> ApiResult<T> error(int code, String message) {
        ApiResult<T> result = new ApiResult<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }
}
```

---

## 接口 1: GET /api/loan/calculator-config

**用途：** 获取贷款试算页配置（金额范围、期数选项、利率、收款账户）

**Java Controller：** `LoanController.getCalculatorConfig()`

**响应 (200)：**
```json
{
  "code": 0,
  "data": {
    "amountRange": { "min": 100, "max": 5000, "step": 100, "default": 3000 },
    "termOptions": [
      { "label": "1期", "value": 1 },
      { "label": "3期", "value": 3 },
      { "label": "6期", "value": 6 }
    ],
    "annualRate": 0.18,
    "lender": "XX商业银行",
    "receivingAccount": { "bankName": "招商银行", "lastFour": "8648", "accountId": "acc_001" }
  }
}
```

---

## 接口 2: POST /api/loan/calculate

**用途：** 根据金额和期数计算还款计划

**Java Controller：** `LoanController.calculate()`

**请求体：**
```json
{ "amount": 3000, "term": 3 }
```

**校验规则：**
- `amount`: 整数, >= min, <= max, 为 step 的整数倍
- `term`: 整数, 必须是 termOptions 中的值

**响应 (200)：**
```json
{
  "code": 0,
  "data": {
    "totalFee": 135.00,
    "annualRate": "18.0%",
    "repaymentPlan": [
      { "period": 1, "date": "2026-05-07", "principal": 1000.00, "interest": 45.00, "total": 1045.00 },
      { "period": 2, "date": "2026-06-07", "principal": 1000.00, "interest": 45.00, "total": 1045.00 },
      { "period": 3, "date": "2026-07-07", "principal": 1000.00, "interest": 45.00, "total": 1045.00 }
    ]
  }
}
```

---

## 接口 3: POST /api/loan/apply

**用途：** 提交借款申请

**Java Controller：** `LoanController.apply()`

**请求体：**
```json
{
  "amount": 3000,
  "term": 3,
  "receivingAccountId": "acc_001",
  "agreedProtocols": ["user_agreement", "loan_agreement", "privacy_policy"]
}
```

**响应 (200)：**
```json
{
  "code": 0,
  "data": {
    "applicationId": "APP202604120001",
    "status": "pending",
    "estimatedTime": "30分钟"
  }
}
```

**错误 (409)：**
```json
{ "code": 409, "data": null, "message": "您已有进行中的借款申请，请勿重复提交" }
```

---

## 接口 4: GET /api/loan/approval-status/{applicationId}

**用途：** 轮询审批进度（前端每 10 秒调用一次）

**Java Controller：** `LoanController.getApprovalStatus()`

**响应 (200)：**
```json
{
  "code": 0,
  "data": {
    "applicationId": "APP202604120001",
    "status": "reviewing",
    "steps": [
      { "name": "提交申请", "status": "completed", "description": "申请已提交成功" },
      { "name": "审批中", "status": "in_progress", "description": "正在进行资质审核..." },
      { "name": "等待放款", "status": "pending", "description": "审批通过后即可放款" }
    ],
    "benefitsCard": {
      "available": true,
      "price": 300,
      "features": ["热门影音会员，出行礼包", "享最高300元优惠", "专属优先通道，不成功不收费"]
    }
  }
}
```

**实现建议：** 此接口被频繁轮询，建议使用 Redis 缓存（TTL: 5s）优化读性能。

---

## 接口 5: GET /api/benefits/card-detail

**用途：** 获取惠选卡完整权益信息

**Java Controller：** `BenefitsController.getCardDetail()`

**响应 (200)：**
```json
{
  "code": 0,
  "data": {
    "cardName": "惠选卡",
    "price": 300,
    "totalSaving": 448,
    "features": [
      { "title": "先享后付，无压力消费", "description": "支持多种消费场景..." },
      { "title": "费用预估¥100/月，共3期", "description": "分期付款..." },
      { "title": "匹配优质权益，不成功不收费", "description": "智能匹配..." }
    ],
    "categories": [
      {
        "name": "影音会员", "icon": "tv",
        "benefits": [
          { "discount": "5折", "title": "每月4选1影视VIP会员", "description": "腾讯视频、优酷、爱奇艺、芒果TV任选", "validity": "有效期30天/次", "originalPrice": 30, "saving": 15 }
        ]
      },
      { "name": "出行服务", "icon": "car", "benefits": [...] },
      { "name": "生活服务", "icon": "life", "benefits": [...] },
      { "name": "日常购物", "icon": "shop", "benefits": [...] }
    ],
    "tips": ["权益服务费基于惠聚会员服务收取...", "本费用为互联网增值服务...", "您充分知晓服务内容..."],
    "protocols": [
      { "name": "用户服务协议", "url": "/protocols/user-service" },
      { "name": "隐私条款声明", "url": "/protocols/privacy" },
      { "name": "委托扣款协议", "url": "/protocols/debit" },
      { "name": "权益服务协议", "url": "/protocols/benefits" }
    ]
  }
}
```

---

## 接口 6: POST /api/benefits/activate

**用途：** 开通惠选卡

**Java Controller：** `BenefitsController.activate()`

**请求体：**
```json
{ "applicationId": "APP202604120001", "cardType": "huixuan_card" }
```

**响应 (200)：**
```json
{ "code": 0, "data": { "activationId": "BEN202604120001", "status": "activated", "message": "惠选卡开通成功" } }
```

---

## 接口 7: GET /api/loan/approval-result/{applicationId}

**用途：** 获取审批最终结果

**Java Controller：** `LoanController.getApprovalResult()`

**响应 (200 — 通过)：**
```json
{
  "code": 0,
  "data": {
    "applicationId": "APP202604120001",
    "status": "approved",
    "approvedAmount": 3000,
    "estimatedArrivalTime": "30分钟",
    "steps": [
      { "name": "提交申请", "status": "completed", "description": "申请已提交成功" },
      { "name": "审批完成", "status": "completed", "description": "资质审核已通过" },
      { "name": "准备放款", "status": "completed", "description": "资金将在30分钟内到账" }
    ],
    "benefitsCardActivated": true,
    "tip": "您已成功开通惠选卡，放款完成后，您可自主领取并生效对应的权益。",
    "loanId": "LOAN202604120001"
  }
}
```

---

## 接口 8: GET /api/repayment/info/{loanId}

**用途：** 获取还款详情

**Java Controller：** `RepaymentController.getInfo()`

**响应 (200)：**
```json
{
  "code": 0,
  "data": {
    "loanId": "LOAN202604120001",
    "repaymentAmount": 1018.50,
    "repaymentType": "提前还款",
    "bankCard": { "bankName": "招商银行", "lastFour": "8648", "accountId": "acc_001" },
    "tip": "还款后将立即生效，剩余期数对应的利息将不再收取。请确认银行卡余额充足。"
  }
}
```

---

## 接口 9: POST /api/repayment/submit

**用途：** 提交还款请求

**Java Controller：** `RepaymentController.submit()`

**请求体：**
```json
{ "loanId": "LOAN202604120001", "amount": 1018.50, "bankCardId": "acc_001", "repaymentType": "early" }
```

**响应 (200)：**
```json
{ "code": 0, "data": { "repaymentId": "REP202604120001", "status": "processing", "message": "还款请求已提交，正在处理中" } }
```

**幂等性：** 使用 `loanId + repaymentType` 作为幂等键，防止重复扣款。

---

## 接口 10: GET /api/repayment/result/{repaymentId}

**用途：** 查询还款结果

**Java Controller：** `RepaymentController.getResult()`

**响应 (200)：**
```json
{
  "code": 0,
  "data": {
    "repaymentId": "REP202604120001",
    "status": "success",
    "amount": 1018.50,
    "repaymentTime": "2026-03-19T14:32:00+08:00",
    "bankCard": { "bankName": "招商银行", "lastFour": "8648" },
    "interestSaved": 26.50,
    "tips": [
      "还款金额已从您的银行卡扣除，请注意查收银行通知",
      "提前还款后，您的信用额度将即时恢复",
      "如需查看还款记录，可前往\"我的\"-\"账单明细\"",
      "若有任何疑问，请联系客服：400-888-8888"
    ]
  }
}
```

---

## 数据库 Schema（建议）

```sql
-- 用户表
CREATE TABLE users (
  id          VARCHAR(36) PRIMARY KEY,
  phone       VARCHAR(11) UNIQUE NOT NULL,
  real_name   VARCHAR(50),
  verified    BOOLEAN DEFAULT FALSE,
  created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 银行卡表
CREATE TABLE bank_cards (
  id          VARCHAR(36) PRIMARY KEY,
  user_id     VARCHAR(36) REFERENCES users(id),
  bank_name   VARCHAR(50) NOT NULL,
  last_four   VARCHAR(4) NOT NULL,
  is_default  BOOLEAN DEFAULT FALSE,
  created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 借款申请表
CREATE TABLE loan_applications (
  id              VARCHAR(20) PRIMARY KEY,
  user_id         VARCHAR(36) REFERENCES users(id),
  amount          DECIMAL(10,2) NOT NULL,
  term            INT NOT NULL,
  annual_rate     DECIMAL(5,4) NOT NULL,
  status          VARCHAR(20) NOT NULL DEFAULT 'pending',
  bank_card_id    VARCHAR(36) REFERENCES bank_cards(id),
  approved_at     TIMESTAMP,
  created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 惠选卡表
CREATE TABLE benefits_cards (
  id              VARCHAR(20) PRIMARY KEY,
  user_id         VARCHAR(36) REFERENCES users(id),
  application_id  VARCHAR(20) REFERENCES loan_applications(id),
  card_type       VARCHAR(30) NOT NULL,
  status          VARCHAR(20) NOT NULL DEFAULT 'activated',
  price           DECIMAL(10,2) NOT NULL,
  created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 贷款表（审批通过后创建）
CREATE TABLE loans (
  id              VARCHAR(20) PRIMARY KEY,
  application_id  VARCHAR(20) REFERENCES loan_applications(id),
  user_id         VARCHAR(36) REFERENCES users(id),
  amount          DECIMAL(10,2) NOT NULL,
  term            INT NOT NULL,
  status          VARCHAR(20) NOT NULL DEFAULT 'active',
  disbursed_at    TIMESTAMP,
  created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 还款表
CREATE TABLE repayments (
  id              VARCHAR(20) PRIMARY KEY,
  loan_id         VARCHAR(20) REFERENCES loans(id),
  user_id         VARCHAR(36) REFERENCES users(id),
  amount          DECIMAL(10,2) NOT NULL,
  type            VARCHAR(20) NOT NULL,
  status          VARCHAR(20) NOT NULL DEFAULT 'processing',
  bank_card_id    VARCHAR(36) REFERENCES bank_cards(id),
  interest_saved  DECIMAL(10,2) DEFAULT 0,
  completed_at    TIMESTAMP,
  created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 索引
CREATE INDEX idx_applications_user ON loan_applications(user_id);
CREATE INDEX idx_applications_status ON loan_applications(status);
CREATE INDEX idx_loans_user ON loans(user_id);
CREATE INDEX idx_repayments_loan ON repayments(loan_id);
```
