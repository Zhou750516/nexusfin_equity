# PLAN.md — H5 前端开发执行计划

> 本文件定义 `H5/` 目录下前端应用的分阶段开发计划。所有文件路径均相对于项目根目录（`nexusfin-equity/`）。
> Java 后端接口由 `src/` 目录下的 Spring Boot 服务提供，H5 前端通过 Vite 代理对接。

---

## Phase 0: 项目清理与环境验证

**目标：** 移除平台专属代码，确认 H5 项目可独立运行。

### Tasks

- [ ] **0.1** 打开 `H5/vite.config.ts`，移除 `vite-plugin-manus-runtime` 的 import 和 plugins 引用
- [ ] **0.2** 同一文件中，移除 `debugCollectorPlugin` 函数定义及其在 plugins 中的引用
- [ ] **0.3** 删除 `H5/client/public/__manus__/` 目录
- [ ] **0.4** 打开 `H5/client/index.html`，移除引用 `%VITE_ANALYTICS_ENDPOINT%/umami` 的 `<script>` 标签
- [ ] **0.5** 在 `H5/vite.config.ts` 中添加 API 代理配置，将 `/api/*` 代理到 Java 后端 `http://localhost:8080`
- [ ] **0.6** 运行 `cd H5 && pnpm install && pnpm check` — 必须 0 errors
- [ ] **0.7** 运行 `cd H5 && pnpm dev` — 开发服务器在 `http://localhost:3000` 正常启动
- [ ] **0.8** 浏览器验证 6 个路由均可正常渲染

### 验收标准
```
✅ pnpm check → 0 errors
✅ pnpm dev → 服务启动，无控制台错误
✅ 6 个路由均可渲染: /, /approval-pending, /benefits-card, /approval-result, /confirm-repayment, /repayment-success
✅ grep -r "manus" H5/client/src/ → 0 结果
✅ /api/* 请求被正确代理到 localhost:8080
```

---

## Phase 1: API 请求层与类型定义

**目标：** 创建 HTTP 请求基础设施和 TypeScript 接口定义。

### Tasks

- [ ] **1.1** 将 `H5/TYPES.ts` 复制到 `H5/client/src/types/loan.types.ts`
- [ ] **1.2** 创建 `H5/client/src/lib/api.ts` — axios 实例：
  - `baseURL: "/api"`（通过 Vite 代理转发到 Java 后端）
  - `timeout: 15000`
  - 请求拦截器：注入 `Authorization: Bearer <token>`
  - 响应拦截器：解包 `res.data`，401 跳转登录
  - 错误拦截器：通过 `sonner` 弹出错误提示
- [ ] **1.3** 创建 `H5/client/src/lib/loan-api.ts` — 10 个类型化 API 函数：
  - `getCalculatorConfig()`, `calculateLoan()`, `applyLoan()`
  - `getApprovalStatus()`, `getBenefitsCardDetail()`, `activateBenefitsCard()`
  - `getApprovalResult()`, `getRepaymentInfo()`, `submitRepayment()`, `getRepaymentResult()`

### 验收标准
```
✅ cd H5 && pnpm check → 0 errors
✅ api.ts 导出配置好的 axios 实例
✅ loan-api.ts 导出 10 个类型化异步函数，无 any 类型
```

---

## Phase 2: 全局状态管理

**目标：** 创建 React Context 在页面间共享贷款申请状态。

### Tasks

- [ ] **2.1** 创建 `H5/client/src/contexts/LoanContext.tsx`：
  ```typescript
  interface LoanState {
    amount: number;              // 默认 3000
    term: number;                // 默认 3
    applicationId: string | null;
    approvalStatus: "idle" | "pending" | "reviewing" | "approved" | "rejected";
    benefitsCardActivated: boolean;
    loanId: string | null;
    repaymentId: string | null;
  }
  ```
- [ ] **2.2** 提供 actions: `setAmount`, `setTerm`, `setApplicationId`, `setApprovalStatus`, `setBenefitsCardActivated`, `setLoanId`, `setRepaymentId`, `reset`
- [ ] **2.3** 在 `H5/client/src/App.tsx` 中用 `<LoanProvider>` 包裹 `<Router />`

### 验收标准
```
✅ LoanContext 导出 useLoan() hook
✅ App.tsx 中 Router 被 LoanProvider 包裹
✅ 6 个路由仍正常渲染
```

---

## Phase 3: Loading 与 Error UI 组件

**目标：** 创建可复用的加载骨架屏和错误状态组件。

### Tasks

- [ ] **3.1** 创建 `H5/client/src/components/LoadingSkeleton.tsx` — 支持 5 种 variant
- [ ] **3.2** 创建 `H5/client/src/components/ErrorState.tsx` — 错误图标 + 消息 + "重试" 按钮
- [ ] **3.3** 创建 `H5/client/src/components/PageContainer.tsx` — 统一处理 loading/error/success 状态

### 验收标准
```
✅ 组件渲染无报错
✅ pnpm check → 0 errors
```

---

## Phase 4: 改造试算页（CalculatorPage）

**目标：** 将 `H5/client/src/pages/CalculatorPage.tsx` 中所有硬编码数据替换为 Java 后端 API 调用。

### Tasks

- [ ] **4.1** 页面加载时调用 `getCalculatorConfig()` 获取配置
- [ ] **4.2** 切换金额/期数时调用 `calculateLoan()` 动态计算（300ms 防抖）
- [ ] **4.3** "确认借款" 按钮调用 `applyLoan()` → 存储 applicationId → 跳转 `/approval-pending`
- [ ] **4.4** "修改金额" 按钮：打开底部抽屉（shadcn Drawer），支持金额输入
- [ ] **4.5** 加载中显示 `LoadingSkeleton variant="calculator"`
- [ ] **4.6** 加载失败显示 `ErrorState`

### 验收标准
```
✅ 页面数据来自 API（或显示 loading/error 状态）
✅ 切换期数触发 API 重新计算
✅ "确认借款" 调用 API 并跳转
✅ pnpm check → 0 errors
```

---

## Phase 5: 改造审批中页（ApprovalPendingPage）

**目标：** 将 `H5/client/src/pages/ApprovalPendingPage.tsx` 的步骤状态替换为轮询 API。

### Tasks

- [ ] **5.1** 从 LoanContext 读取 `applicationId`
- [ ] **5.2** 每 10 秒轮询 `getApprovalStatus(applicationId)`
- [ ] **5.3** 根据 API 返回的 `steps[]` 动态渲染 3 步进度
- [ ] **5.4** `status === "approved"` 时停止轮询，自动跳转 `/approval-result`
- [ ] **5.5** 惠选卡广告数据来自同一 API 响应
- [ ] **5.6** 组件卸载时清理 interval

### 验收标准
```
✅ 步骤状态从 API 动态更新
✅ 轮询正常运行并在卸载时清理
✅ 审批通过后自动跳转
```

---

## Phase 6: 改造惠选卡页（BenefitsCardPage）

**目标：** 将 `H5/client/src/pages/BenefitsCardPage.tsx` 的权益数据替换为 API 调用。

### Tasks

- [ ] **6.1** 页面加载时调用 `getBenefitsCardDetail()`
- [ ] **6.2** 替换 `FEATURES`、`BENEFITS_DATA`、`TIPS` 为 API 数据
- [ ] **6.3** "立即开通" 调用 `activateBenefitsCard()` → 更新 LoanContext → 跳转

### 验收标准
```
✅ 所有内容来自 API
✅ Tab 切换正常
✅ "立即开通" 调用激活 API
```

---

## Phase 7: 改造审批结果页（ApprovalResultPage）

**目标：** 将 `H5/client/src/pages/ApprovalResultPage.tsx` 替换为 API 数据。

### Tasks

- [ ] **7.1** 调用 `getApprovalResult(applicationId)`
- [ ] **7.2** 动态显示审批金额、步骤状态、惠选卡提示
- [ ] **7.3** 存储 `loanId` 到 LoanContext

### 验收标准
```
✅ 金额和步骤来自 API
✅ 惠选卡提示条件显示
```

---

## Phase 8: 改造还款流程（ConfirmRepaymentPage + RepaymentSuccessPage）

**目标：** 将两个还款页面对接 Java 后端 API。

### Tasks

- [ ] **8.1** `ConfirmRepaymentPage`: 调用 `getRepaymentInfo(loanId)` 获取还款信息
- [ ] **8.2** "确认支付" 调用 `submitRepayment()` → 跳转还款成功页
- [ ] **8.3** `RepaymentSuccessPage`: 调用 `getRepaymentResult(repaymentId)` 获取结果
- [ ] **8.4** "返回首页" 调用 `reset()` 重置全局状态 → 跳转 `/`

### 验收标准
```
✅ 两个页面数据来自 API
✅ 支付提交有 loading 状态和错误处理
✅ "返回首页" 重置状态
```

---

## Phase 9: Java 后端接口实现

**目标：** 在 `src/` 目录下实现 10 个 REST API 端点。

### 已实现接口对齐结论

经检查当前仓库已有 Controller，现状如下：

- 已有认证接口：
  - `GET /api/auth/sso-callback`
  - `GET /api/users/me`
- 已有权益主链接口：
  - `GET /api/equity/products/{productCode}`
  - `POST /api/equity/orders`
  - `GET /api/equity/orders/{benefitOrderNo}`
  - `GET /api/equity/exercise-url/{benefitOrderNo}`
- 已有回调接口：
  - `/api/callbacks/*`

对齐结论：

1. **Phase 9 定义的 10 个 H5 后端接口当前均未直接实现。**
2. 现有 `/api/equity/*` 接口属于权益订单基线能力，与 H5 所需的试算、审批进度、惠选卡详情、还款详情接口**功能不等价**，不能直接视为已完成。
3. 现有 `/api/auth/*` 与 `/api/users/me` 可继续保留复用，但**不计入** Phase 9 的 10 个接口清单。
4. 因此 Phase 9 仍需新增以下 10 个接口，不从清单中删除任何项。

### Phase 9 接口清单（经对齐后）

- [ ] `GET /api/loan/calculator-config`
- [ ] `POST /api/loan/calculate`
- [ ] `POST /api/loan/apply`
- [ ] `GET /api/loan/approval-status/{applicationId}`
- [ ] `GET /api/benefits/card-detail`
- [ ] `POST /api/benefits/activate`
- [ ] `GET /api/loan/approval-result/{applicationId}`
- [ ] `GET /api/repayment/info/{loanId}`
- [ ] `POST /api/repayment/submit`
- [ ] `GET /api/repayment/result/{repaymentId}`

### Phase 9 接口决策实现口径

| H5 接口 | 实现方式 | 上游 / 数据来源 | 备注 |
| --- | --- | --- | --- |
| `GET /api/loan/calculator-config` | 本地配置实现 | 本地配置项 | 不走云卡 |
| `POST /api/loan/calculate` | 调用云卡 gateway 转发 | 参考 `20260410_艾博生调用云卡接口文档.md`，转发参数参考 `20260404_科技平台接口与改造需求_修订版.md` 中“借款试算 `/loan/trail`” | `path` 通过配置项承载，默认值 `/loan/trail` |
| `POST /api/loan/apply` | 先调齐为购买权益，再调云卡 gateway | 齐为权益开通接口 + 云卡 `/loan/apply` | 这是唯一一个串联“齐为 + 云卡”的接口 |
| `GET /api/loan/approval-status/{applicationId}` | 调用云卡 gateway 转发 | 转发参数参考 `20260404_科技平台接口与改造需求_修订版.md` 中“放款结果查询 `/loan/query`” | 按页面“审批中”语义做前端结果映射 |
| `GET /api/benefits/card-detail` | 调齐为侧接口 | 齐为权益信息接口 | 不走云卡 |
| `POST /api/benefits/activate` | 调齐为侧接口 | 齐为权益开通接口 | 不走云卡 |
| `GET /api/loan/approval-result/{applicationId}` | 调用云卡 gateway 转发 | 转发参数参考 `20260404_科技平台接口与改造需求_修订版.md` 中“放款结果查询 `/loan/query`” | 与 `approval-status` 共用上游接口，不同在页面结果映射 |
| `GET /api/repayment/info/{loanId}` | 调用云卡 gateway 转发 | 转发参数参考 `20260404_科技平台接口与改造需求_修订版.md` 中“还款试算 `/repay/trial`” | 返回值需映射成 H5 还款详情视图 |
| `POST /api/repayment/submit` | 调用云卡 gateway 转发 | 转发参数参考 `20260404_科技平台接口与改造需求_修订版.md` 中“主动还款 `/repay/apply`” | 返回上游还款结果标识，供科技平台后续跳转携带 `repaymentId` |
| `GET /api/repayment/result/{repaymentId}` | 调用云卡 gateway 转发 | 转发参数参考 `20260404_科技平台接口与改造需求_修订版.md` 中“还款结果查询 `/repay/query`” | 使用科技平台跳转携带的 `repaymentId` 作为查询入口，不本地生成 |

### 实施边界调整

基于上述决策，Phase 9 的实现边界调整为：

1. 以**聚合 / 转发 / 结果映射**为主，不按原计划先做完整数据库建模。
2. 优先复用现有认证链路与 `Result<T>` 响应包装，不在本阶段新起一套认证方案。
3. 本阶段重点新增：
   - H5 所需的 Controller
   - 本地配置读取
   - 齐为接口适配
   - 云卡 gateway 客户端与请求 / 响应映射
   - 必要的本地关联键保存能力（围绕 `applicationId` 及其上游查询参数最小化保存）
4. Flyway / Liquibase、完整数据库实体、完整领域建模不再作为本阶段必选项；仅在确实需要保存映射关系时最小化补充。

### 已确认执行前置条件

1. `/loan/trail` 使用配置项承载，默认值为 `/loan/trail`。
2. `GET /api/loan/approval-status/{applicationId}` 与 `GET /api/loan/approval-result/{applicationId}` 均走上游 `/loan/query`。
3. `applicationId` 允许由艾博生本地生成并维护映射关系。
4. `repaymentId` 使用科技平台跳转携带参数，不由艾博生本地生成。
5. `POST /api/loan/apply` 如出现“齐为成功、云卡失败”，需返回借款 / 放款失败，但明确告知权益购买成功。

### Tasks

- [ ] **9.1** 新增 H5 Controller：
  - `LoanController`：处理 `/api/loan/*`
  - `BenefitsController`：处理 `/api/benefits/*`
  - `RepaymentController`：处理 `/api/repayment/*`
- [ ] **9.2** 实现 `GET /api/loan/calculator-config`
  - 使用本地配置返回金额范围、期数选项、利率、收款账户
  - 默认按“固定 3 期”口径实现
- [ ] **9.3** 实现齐为侧接口适配
  - `GET /api/benefits/card-detail` → 齐为权益信息接口
  - `POST /api/benefits/activate` → 齐为权益开通接口
- [ ] **9.4** 实现云卡 gateway 客户端
  - 按 `20260410_艾博生调用云卡接口文档.md` 统一走 gateway URI
  - 统一封装 `path + data + requestId + bizOrderNo`
- [ ] **9.5** 实现贷款链路接口
  - `POST /api/loan/calculate` → 云卡转发 `/loan/trail`
  - `GET /api/loan/approval-status/{applicationId}` → 云卡转发 `/loan/query`
  - `GET /api/loan/approval-result/{applicationId}` → 云卡转发 `/loan/query`
- [ ] **9.6** 实现借款申请编排接口
  - `POST /api/loan/apply`
  - 先调用齐为购买权益
  - 权益成功后再调用云卡 `/loan/apply`
  - 对外返回 H5 需要的 `applicationId` 和状态
- [ ] **9.7** 实现还款链路接口
  - `GET /api/repayment/info/{loanId}` → 云卡转发 `/repay/trial`
  - `POST /api/repayment/submit` → 云卡转发 `/repay/apply`
  - `GET /api/repayment/result/{repaymentId}` → 使用科技平台跳转携带的 `repaymentId` 调用云卡转发 `/repay/query`
- [ ] **9.8** 创建 DTO 与映射层
  - 输入 DTO 对齐 `H5/client/src/types/loan.types.ts`
  - 输出 DTO 对齐 `H5/API_SCHEMA.md`
  - 增加齐为 / 云卡响应到 H5 视图模型的转换
- [ ] **9.9** 实现请求校验和统一响应
  - `@Valid` 参数校验
  - 复用现有认证链路
  - 保持统一 `Result<T>` / H5 约定响应格式
- [ ] **9.10** 补充最小化本地持久化或映射关系（如需要）
  - 围绕 `applicationId` 与上游查询参数新增最小数据落库
  - 若可无状态完成，则不新增表
- [ ] **9.11** 编写测试
  - 本地配置接口测试
  - 齐为接口适配测试
  - 云卡转发请求封装测试
  - `loan/apply` 编排顺序测试（先齐为、后云卡）

### 验收标准
```
✅ 10 个端点返回符合 API_SCHEMA.md 规范的响应
✅ 请求校验拒绝非法输入（400）
✅ 复用现有认证链路，未授权请求被拒绝（401）
✅ `loan/apply` 满足“先买权益、后调云卡”的业务顺序
✅ `calculator-config` 由本地配置返回固定 3 期相关配置
✅ 转发型接口按云卡 gateway 文档组装 `path + data`
✅ mvn test → 全部通过
```

---

## Phase 10: 用户认证

**目标：** 添加登录页面和认证流程。

### Tasks

- [ ] **10.1** 创建 `H5/client/src/pages/LoginPage.tsx`（手机号 + 验证码）
- [ ] **10.2** Java 后端实现 `POST /api/auth/send-code` 和 `POST /api/auth/login`
- [ ] **10.3** H5 前端添加路由守卫：无 token 跳转 `/login`
- [ ] **10.4** 在 `H5/client/src/App.tsx` 注册 `/login` 路由

### 验收标准
```
✅ 登录页正常渲染
✅ 验证码发送和登录流程完整
✅ 未登录访问自动跳转
```

---

## Phase 11: 打磨与测试

**目标：** 添加动画、边界处理、自动化测试。

### Tasks

- [ ] **11.1** 使用 framer-motion 添加页面切换动画
- [ ] **11.2** 审批页添加下拉刷新
- [ ] **11.3** 添加边界状态 UI（审批拒绝、网络错误等）
- [ ] **11.4** 编写前端单元测试（vitest）
- [ ] **11.5** 编写后端单元测试（JUnit）
- [ ] **11.6** 验证 `cd H5 && pnpm build` 生产构建成功

### 验收标准
```
✅ 页面切换有过渡动画
✅ 边界状态有对应 UI
✅ 测试覆盖率 > 80%（业务逻辑）
✅ 前后端构建均成功
```

---

## 执行顺序总结

```
Phase 0  → 清理 (30 min)
Phase 1  → API 层 + 类型 (1 hour)
Phase 2  → 全局状态 (30 min)
Phase 3  → Loading/Error UI (30 min)
  ↓
Phase 4-8 → 前端页面改造 (8 hours)
  ↓ (可与 4-8 并行)
Phase 9  → Java 后端实现 (4-6 hours)
  ↓
Phase 10 → 认证 (2 hours)
Phase 11 → 打磨测试 (2-3 hours)
```

**总预估工时: 19-24 小时**
