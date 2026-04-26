# 2026-04-26 Shared Foundation 第一批落地项

## 1. 文档目标

本文档用于把“Shared Foundation 收口”从概念层推进到可执行层，明确：

1. 第一批最小可落地范围是什么；
2. 哪些事项可以立即开工；
3. 哪些事项仍然依赖外部字段 / 接口确认；
4. 建议的落地顺序和产出标准。

本轮不追求一次把全部小花 `4.22` 差距做完，而是优先收口后续所有故事都会依赖的基础层。

---

## 2. 为什么 Shared Foundation 必须先做

当前权益、借款、还款、回调四条链路虽然都已有第一版骨架，但仍存在共同问题：

1. Yunka 路径配置覆盖不全；
2. `LoanServiceImpl`、`RepaymentServiceImpl` 仍存在直接拼 `JsonNode` 的情况；
3. 缺少统一的小花经云卡 facade；
4. 回调 DTO 语义仍偏旧；
5. 多条链路会各自定义字段和映射，后续容易再次分裂。

因此 Shared Foundation 不是“加一个公共类”这么简单，而是后续所有对齐工作的统一底座。

---

## 3. 第一批最小落地范围

建议第一批只做四块：

1. **Yunka 路径配置补齐**
2. **typed DTO 第一批建模**
3. **`XiaohuaGatewayService` facade 入口收口**
4. **放款 / 还款回调 DTO 第一版模型**

不建议第一批就直接扩散到：

1. 权益页真实动态聚合
2. 借款主链重构
3. 还款完整短信流程
4. 法大大接入

原因是这些都依赖基础层先稳定。

---

## 4. 第一批可立即开工项

## 4.1 Yunka 路径配置补齐

### 目标

把当前只覆盖 `loan/trail`、`loan/query`、`loan/apply`、`repay/*` 的路径配置，扩展到后续所需能力。

### 建议范围

优先补：

1. `protocolQuery`
2. `userToken`
3. `userQuery`
4. `loanRepayPlan`
5. `cardSmsSend`
6. `cardSmsConfirm`
7. `cardUserCards`
8. `creditImageQuery`
9. `benefitSync`

### 说明

这部分可以立即开工，因为：

1. 路径名称本地可先约定；
2. 即便外部字段未完全定稿，也不影响先把配置层补齐；
3. 后续 facade 和 service 实现都会直接依赖这些键位。

## 4.2 typed DTO 第一批建模

### 目标

先建立“能支撑后续 facade 收口”的第一批 DTO，而不是等业务层再各自拼字段。

### 建议第一批优先覆盖

1. 协议查询：
   - `ProtocolQueryRequest`
   - `ProtocolQueryResponse`
2. 用户鉴权与信息：
   - `UserTokenRequest`
   - `UserTokenResponse`
   - `UserQueryRequest`
   - `UserQueryResponse`
3. 绑卡列表：
   - `UserCardListRequest`
   - `UserCardListResponse`
4. 还款计划：
   - `LoanRepayPlanRequest`
   - `LoanRepayPlanResponse`
5. 短信发送 / 确认：
   - `CardSmsSendRequest`
   - `CardSmsSendResponse`
   - `CardSmsConfirmRequest`
   - `CardSmsConfirmResponse`
6. 图片查询：
   - `CreditImageQueryRequest`
   - `CreditImageQueryResponse`
7. 权益同步：
   - `BenefitOrderSyncRequest`
   - `BenefitOrderSyncResponse`

### 说明

这部分也可以立即开工，因为：

1. DTO 可以先按当前文档和本地理解建一版；
2. 后续字段增减比“完全没有统一模型”更容易维护；
3. 先有类型，再有 facade，再有业务接入，会更稳。

## 4.3 `XiaohuaGatewayService` facade 入口收口

### 目标

把散落在不同 service 里的 Yunka 调用，先统一收敛到一层 facade。

### 第一批建议只做什么

1. 统一路径选择；
2. 统一请求发送入口；
3. 统一响应解包；
4. 统一错误码与异常口径；
5. 统一基础日志点。

### 第一批不必做什么

1. 不要求一次把所有链路完全接入 facade；
2. 不要求一次把所有业务逻辑都重写；
3. 不要求与新主链状态机完全绑定。

### 说明

这部分可以立即开工，因为：

1. 当前已有 `XiaohuaGatewayService` / `XiaohuaGatewayServiceImpl` 基础文件；
2. 真正需要的是先把接口和模型边界收紧；
3. 后续 Benefits / Loan / Repayment 都能复用。

## 4.4 回调 DTO 第一版模型

### 目标

先统一“小花经云卡回调艾博生”的基础字段模型。

### 第一批建议覆盖

1. 放款结果回调 DTO
2. 还款结果回调 DTO
3. 必要的主键字段：
   - `bizOrderNo`
   - `traceId`
   - `yunkaOrderNo`
   - `loanId` / `repaymentId`
   - `resultCode`
   - `resultMessage`

### 说明

这部分可以先做第一版基础模型，但某些字段仍需等待云卡最终 envelope 定义。

---

## 5. 第一批仍待外部确认项

以下事项不建议在第一批里强行做死：

1. 云卡状态机真实字段名与完整状态值；
2. `2.15` 权益订单同步真实路径；
3. `2.12 smsConfirm` 的 `loanId` 传参规则；
4. `imageInfo` 的最终结构和必填范围；
5. Yunka 转发回调的完整 envelope 结构；
6. 云卡建放款订单、触发放款、关单判断接口的真实字段。

处理建议：

1. 当前先留 TODO、占位字段和扩展位；
2. 不把这些未知项硬编码到业务层逻辑里；
3. 在文档中持续标记为“待外部确认”。

---

## 6. 推荐落地顺序

建议第一批按以下顺序推进：

### 步骤 1：路径配置

先补 Yunka 路径配置与配置测试。

### 步骤 2：DTO 建模

先建协议、用户、绑卡、还款计划、短信、图片、权益同步 DTO。

### 步骤 3：facade 收口

先让 `XiaohuaGatewayService` 提供统一调用入口。

### 步骤 4：回调模型

补放款 / 还款回调 DTO 和基础 handler 入口。

### 步骤 5：逐条链路接入

再由：

1. 权益页动态化
2. 借款链路对齐
3. 还款链路对齐

逐步消费 Shared Foundation。

---

## 7. 第一批完成标准

如果第一批完成，至少应达到以下结果：

1. `application.yml` 和 `YunkaProperties` 已具备完整键位；
2. 小花 `4.22` 主要能力已有统一 DTO；
3. 业务 service 不再需要各自临时拼装一批原始 JSON；
4. 放款 / 还款回调模型不再混用旧语义；
5. 后续权益 / 借款 / 还款任务可以在统一底座上继续推进。

---

## 8. 建议对应的首批任务清单

建议后续真实执行时，第一批优先按以下任务开工：

1. 补 `YunkaProperties` 与配置测试；
2. 建协议 / 用户 / 卡 / 还款计划 / 短信 / 图片 / 权益同步 DTO；
3. 补 `XiaohuaGatewayService` 第一版统一方法；
4. 建放款 / 还款回调 DTO；
5. 再开始接入权益页和还款页中最不依赖外部状态机的部分。

---

## 9. 一句话结论

> Shared Foundation 第一批最重要的不是“一次做完所有能力”，而是先把路径、DTO、facade、回调模型这四块底座收紧，让后续所有链路都站在同一套契约之上继续推进。
