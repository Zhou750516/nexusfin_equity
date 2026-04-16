# 2026-04-13 工作 Checklist

> 今日主线：启动 `Phase 9` 后端实现，先完成任务组 A，再为云卡 gateway 打基础。

## 状态总览

### 今日重点

- [x] 搭好 H5 专用接口层骨架
- [x] 完成任务组 A 三个接口的主体实现
- [x] 预埋云卡 gateway 配置与客户端骨架
- [x] 完成一轮最小测试或本地校验

### 今日边界

- [x] 今天不直接展开全部 10 个接口
- [x] 今天不抢先做 `loan/apply` 复杂编排
- [x] 今天仍不依赖云卡真实环境启动联调

## P0：搭好 H5 专用接口层骨架

- [x] 新增 `LoanController`
- [x] 新增 `BenefitsController`
- [x] 新增 `RepaymentController`
- [x] 新增 `LoanService`
- [x] 新增 `BenefitsService`
- [x] 新增 `RepaymentService`
- [x] 新增任务组 A 对应 DTO
- [x] 确认新接口统一返回 `Result<T>`
- [x] 确认新接口参数使用 `@Valid`

## P0：完成 `GET /api/loan/calculator-config`

- [x] 新增配置类承载金额范围、默认金额、期数、收款账户
- [x] 固定返回 3 期
- [x] 新增响应 DTO
- [x] 完成 Service 实现
- [x] 完成 Controller 暴露

## P0：完成 `GET /api/benefits/card-detail`

- [x] 明确现有 `BenefitProduct` 可复用字段
- [x] 明确需由本地配置补齐的展示字段
- [x] 新增 H5 专用响应 DTO
- [x] 完成 Service 映射逻辑
- [x] 完成 Controller 暴露

## P0：完成 `POST /api/benefits/activate`

- [x] 明确 H5 入参与现有 `CreateBenefitOrderRequest` 的映射方式
- [x] 新增 H5 请求 / 响应 DTO
- [x] 通过适配层复用 `BenefitOrderService.createOrder()`
- [x] 完成 Controller 暴露
- [x] 明确 `activationId`、`status`、`message` 的返回口径

## P1：预埋云卡 gateway 基础设施

- [x] 新增云卡配置类
- [x] 抽取统一 gateway URI
- [x] 抽取 `/loan/trail` 配置项
- [x] 新增云卡客户端接口
- [x] 新增云卡客户端实现骨架

## P1：为后续任务预留基础模型

- [x] 明确 `applicationId` 映射所需最小字段
- [x] 判断是扩表还是新增映射表
- [x] 若时间允许，新增实体 / 仓储骨架

## 收尾检查

- [x] 回看今天实现是否仍符合任务组 A → B 的顺序
- [x] 确认未把旧 `/api/equity/*` 直接暴露给 H5
- [x] 确认新增接口已纳入鉴权路径或完成纳管方案
- [x] 如已完成最小测试，记录测试范围与结果

## 新增事项：H5 多语言方案设计

- [x] 明确支持语言：`zh-CN`、`zh-TW`、`en-US`、`vi-VN`
- [x] 明确“浏览器自动匹配 + 手动切换 + 持久化”的规则
- [x] 明确覆盖 `H5 + 后端 + 协议/富文本内容`
- [x] 确认采用“方案 C：统一文案资源中心”
- [x] 输出方案文档 `plan/20260413_H5多语言支持方案设计.md`

## 新增事项：任务组 B/C 执行

- [x] `POST /api/loan/calculate` 接入云卡 `/loan/trail`
- [x] `GET /api/loan/approval-status/{applicationId}` 接入云卡 `/loan/query`
- [x] `GET /api/loan/approval-result/{applicationId}` 接入云卡 `/loan/query`
- [x] 复用 `applicationId` 本地映射恢复上游 `loanId`
- [x] 补充 `Phase9TaskGroupCIntegrationTest`
- [x] 通过 `mvn -q -Dtest=Phase9TaskGroupCIntegrationTest test`
- [x] 通过 `mvn -q -Dtest=Phase9TaskGroupAIntegrationTest,Phase9TaskGroupCIntegrationTest test`
- [x] 通过 `mvn -q checkstyle:check`

## 新增事项：任务组 D 执行

- [x] `GET /api/repayment/info/{loanId}` 接入云卡 `/repay/trial`
- [x] `POST /api/repayment/submit` 接入云卡 `/repay/apply`
- [x] `GET /api/repayment/result/{repaymentId}` 接入云卡 `/repay/query`
- [x] `repaymentId` 使用云卡 / 科技平台返回的 `swiftNumber`，不由艾博生本地生成
- [x] 补充 `Phase9TaskGroupDIntegrationTest`
- [x] 通过 `mvn -q -Dtest=Phase9TaskGroupDIntegrationTest test`
- [x] 通过 `mvn -q -Dtest=Phase9TaskGroupAIntegrationTest,Phase9TaskGroupCIntegrationTest,Phase9TaskGroupDIntegrationTest test`
- [x] 通过 `mvn -q checkstyle:check`

## 新增事项：任务组 E 执行

- [x] `POST /api/loan/apply` 先调用齐为购买权益
- [x] `POST /api/loan/apply` 在权益成功后调用云卡 `/loan/apply`
- [x] 艾博生本地生成 `applicationId`
- [x] 保存 `applicationId -> loanId` 映射
- [x] 若“权益成功、云卡失败”，返回借款失败且明确权益已购买成功
- [x] 补充 `Phase9TaskGroupEIntegrationTest`
- [x] 通过 `mvn -q -Dtest=Phase9TaskGroupEIntegrationTest test`
- [x] 通过 `mvn -q -Dtest=Phase9TaskGroupAIntegrationTest,Phase9TaskGroupCIntegrationTest,Phase9TaskGroupDIntegrationTest,Phase9TaskGroupEIntegrationTest test`
- [x] 通过 `mvn -q checkstyle:check`

## 今天结束时的完成标准

1. H5 专用接口层骨架已完成。
2. `calculator-config`、`benefits/card-detail`、`benefits/activate` 至少完成主体实现。
3. 云卡 gateway 基础设施已有配置与客户端骨架，可承接后续任务组 B。

## 新增事项：H5 多语言执行

- [x] 新增 `H5` 语言定义与归一化工具
- [x] 新增四语 `messages` / `content` 文案资源
- [x] 新增 `I18nProvider` 与 `LanguageSwitcher`
- [x] 在 `App.tsx` 注册全局 Provider
- [x] 在 `MobileLayout.tsx` 暴露语言切换入口
- [x] 完成 6 个主页面静态文案替换
- [x] 调整协议标题在英文/越南文下的展示分隔
- [x] 新增后端 `Accept-Language` 解析入口
- [x] 新增后端 `LocaleContext` / 文案资源服务骨架
- [x] 将 `benefits/card-detail`、`loan/*` 审批展示字段、`repayment/*` 提示字段接入语言资源骨架
- [x] 新增英文回归测试：`benefits/card-detail`
- [x] 新增英文回归测试：`repayment/info`
- [x] 通过 `mvn -q -Dtest=Phase9TaskGroupAIntegrationTest,Phase9TaskGroupDIntegrationTest test`
- [x] 通过 `mvn -q -Dtest=Phase9TaskGroupAIntegrationTest,Phase9TaskGroupCIntegrationTest,Phase9TaskGroupDIntegrationTest,Phase9TaskGroupEIntegrationTest test`
- [x] 通过 `mvn -q checkstyle:check`
- [x] 通过 `H5` 类型检查：`./node_modules/.bin/tsc --noEmit`
- [x] 完成 `H5` 依赖安装：`npm exec --yes pnpm@10.4.1 -- install --frozen-lockfile`

## 新增事项：H5 接口联通执行

- [x] 新增 `H5` 统一请求层：`api.ts` / `loan-api.ts`
- [x] 新增 `LoanContext`，沉淀申请编号、借据编号、还款流水号等跨页状态
- [x] 新增 H5 格式化与路由辅助：金额/日期格式化、查询参数拼接
- [x] `CalculatorPage` 接入 `calculator-config` / `calculate` / `apply`
- [x] `ApprovalPendingPage` 接入 `approval-status` 轮询
- [x] `BenefitsCardPage` 接入 `card-detail` / `activate`
- [x] `ApprovalResultPage` 接入 `approval-result`
- [x] `ConfirmRepaymentPage` 接入 `repayment/info` / `repayment/submit`
- [x] `RepaymentSuccessPage` 接入 `repayment/result`
- [x] 支持通过查询参数恢复 `applicationId` / `loanId` / `repaymentId`
- [x] H5 编译校验通过：`cd H5 && ./node_modules/.bin/tsc --noEmit`
