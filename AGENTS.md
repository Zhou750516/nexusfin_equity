# NexusFin - nexusfin-equity (艾博生)

## 项目概述
惠聚项目（NexusFin），本仓库为艾博生权益分发服务。
Java 17 + Spring Boot 3.2 + MyBatis-Plus + MySQL 8.0

## 目录结构
src/main/java/com/nexusfin/equity/
├── controller/    # REST API 入口
├── service/       # 业务逻辑层
├── service/impl/  # 业务实现
├── repository/    # 数据访问层
├── entity/        # 数据库实体
├── dto/
│   ├── request/   # 请求 DTO
│   └── response/  # 响应 DTO
├── config/        # 配置类
├── exception/     # 异常定义与统一处理
├── enums/         # 枚举类
└── util/          # 公共工具

## 构建与运行
- 构建: mvn clean package -DskipTests
- 运行: java -jar target/nexusfin-equity.jar
- 测试: mvn test
- MySQL回归: MYSQL_IT_ENABLED=true MYSQL_IT_DATABASE=nexusfin_equity mvn -Dtest=MySqlRoundTripIntegrationTest,MySqlCallbackFlowIntegrationTest test
- 代码检查: mvn checkstyle:check

## 本地数据库
- 复用现有数据库 `nexusfin_equity`
- 连接方式: `mysql -uroot`
- 默认不重建数据库和数据表

## 编码规范
- 所有 REST 接口统一返回 Result<T> 包装类
- 接口路径不包含版本号，如 /api/equity/benefit/create
- 数据库字段使用下划线命名，Java 属性使用驼峰命名
- 所有金额字段使用 Long 类型，单位为分
- 敏感字段（身份证、手机号）必须加密存储，查询使用 hash 索引
- 异常统一通过 GlobalExceptionHandler 处理
- 日志使用 SLF4J，关键业务节点必须打印 traceId + bizOrderNo

## 禁止事项
- 禁止在 Controller 层写业务逻辑
- 禁止硬编码配置值，必须使用 @Value 或 @ConfigurationProperties
- 禁止使用 System.out.println
- 禁止在循环中进行数据库查询
- 禁止捕获 Exception 后不做任何处理

## 完成标准
- 代码编译通过，无 checkstyle 告警
- 单元测试覆盖核心业务方法
- 接口参数有 @Valid 校验注解

## Active Technologies
- Java 17 + Spring Boot 3.2.x, Spring Validation, MyBatis-Plus, (001-equity-service-baseline)
- MySQL 8.0 (`nexusfin_equity`), optional Redis/Redisson for (001-equity-service-baseline)

## Recent Changes
- 001-equity-service-baseline: Added Java 17 + Spring Boot 3.2.x, Spring Validation, MyBatis-Plus,
- 001-equity-service-baseline: Added H2/MySQL integration coverage, quickstart smoke flow, downstream sync idempotency, and reconciliation query support

---

# H5 前端模块（Loan App）

> 以下内容描述 `H5/` 目录下的移动端金融贷款前端应用。该模块与 Java 后端通过 REST API 通信，后端接口规范见 `H5/API_SCHEMA.md`。

## H5 模块概述

- **目录位置：** `H5/`（项目根目录下）
- **类型：** 移动端金融贷款 H5 应用（中国市场）
- **当前状态：** 前端 UI 已完成，所有业务数据为硬编码 Mock，需对接 Java 后端接口
- **UI 语言：** 简体中文；代码注释和变量名使用英文

## H5 技术栈

| 层级 | 技术 | 版本 |
|---|---|---|
| 运行时 | Node.js | >= 22.x |
| 包管理器 | pnpm | >= 10.x |
| 框架 | React | 19.x |
| 语言 | TypeScript | 5.6.3 |
| 样式 | Tailwind CSS | 4.x |
| 路由 | wouter | 3.x |
| UI 组件 | shadcn/ui (Radix) | latest |
| HTTP 客户端 | axios | 1.x |
| 动画 | framer-motion | 12.x |
| 构建工具 | Vite | 7.x |
| 测试 | vitest | 2.x |

## H5 目录结构

```
H5/
├── client/
│   ├── index.html                        # HTML 入口
│   └── src/
│       ├── main.tsx                      # React 入口
│       ├── App.tsx                       # 路由 + 全局 Provider
│       ├── index.css                     # Tailwind 主题 + 全局样式
│       ├── pages/
│       │   ├── CalculatorPage.tsx        # 路由: /         试算页
│       │   ├── ApprovalPendingPage.tsx   # 路由: /approval-pending  审批中
│       │   ├── BenefitsCardPage.tsx      # 路由: /benefits-card     惠选卡
│       │   ├── ApprovalResultPage.tsx    # 路由: /approval-result   审批结果
│       │   ├── ConfirmRepaymentPage.tsx  # 路由: /confirm-repayment 确认还款
│       │   └── RepaymentSuccessPage.tsx  # 路由: /repayment-success 还款成功
│       ├── components/
│       │   ├── MobileLayout.tsx          # 移动端布局壳 (max-w-440px)
│       │   ├── Icons.tsx                 # 内联 SVG 图标库
│       │   └── ui/                       # shadcn/ui 组件 (40+)
│       ├── contexts/
│       │   └── ThemeContext.tsx           # 主题上下文
│       ├── hooks/                        # 自定义 Hooks
│       └── lib/
│           └── utils.ts                  # 工具函数
├── server/
│   └── index.ts                          # Express 静态服务（开发用，生产环境由 Java 后端代理）
├── package.json
├── tsconfig.json
└── vite.config.ts
```

## H5 路由表

| 路由 | 页面组件 | 说明 |
|---|---|---|
| `/` | `CalculatorPage` | 试算页：金额、期数选择、息费明细 |
| `/approval-pending` | `ApprovalPendingPage` | 审批中：3步进度条 + 惠选卡广告 |
| `/benefits-card` | `BenefitsCardPage` | 惠选卡详情：特色、Tab权益、提示 |
| `/approval-result` | `ApprovalResultPage` | 审批结果：3步完成、金额确认 |
| `/confirm-repayment` | `ConfirmRepaymentPage` | 确认还款：金额、银行卡、支付 |
| `/repayment-success` | `RepaymentSuccessPage` | 还款成功：详情、节省利息、提示 |

## 业务流程

```
CalculatorPage (/)
  → [确认借款] → ApprovalPendingPage (/approval-pending)
    → [开通服务] → BenefitsCardPage (/benefits-card)
      → [立即开通] → ApprovalResultPage (/approval-result)
    → [放弃优惠] → ApprovalResultPage (/approval-result)
  → ApprovalResultPage (/approval-result)
    → [立即生效权益] → ConfirmRepaymentPage (/confirm-repayment)
      → [确认支付] → RepaymentSuccessPage (/repayment-success)
        → [返回首页] → CalculatorPage (/)
```

## 前后端对接说明

H5 前端通过 axios 调用 Java 后端的 REST API。在开发环境中，Vite 的 `server.proxy` 将 `/api/*` 请求代理到 Java 后端服务（默认 `http://localhost:8080`）。生产环境中，H5 构建产物（`H5/dist/`）由 Java 后端或 Nginx 作为静态资源托管，API 请求直接走同域。

**Vite 代理配置（需在 `H5/vite.config.ts` 中添加）：**

```typescript
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:8080',  // Java 后端地址
      changeOrigin: true,
    }
  }
}
```

**API 路径映射（H5 前端 → Java 后端）：**

| H5 前端调用 | Java 后端 Controller |
|---|---|
| `GET /api/loan/calculator-config` | `LoanController.getCalculatorConfig()` |
| `POST /api/loan/calculate` | `LoanController.calculate()` |
| `POST /api/loan/apply` | `LoanController.apply()` |
| `GET /api/loan/approval-status/:id` | `LoanController.getApprovalStatus()` |
| `GET /api/benefits/card-detail` | `BenefitsController.getCardDetail()` |
| `POST /api/benefits/activate` | `BenefitsController.activate()` |
| `GET /api/loan/approval-result/:id` | `LoanController.getApprovalResult()` |
| `GET /api/repayment/info/:loanId` | `RepaymentController.getInfo()` |
| `POST /api/repayment/submit` | `RepaymentController.submit()` |
| `GET /api/repayment/result/:id` | `RepaymentController.getResult()` |

## H5 硬编码数据映射（需替换为 API 调用）

| 文件（相对 H5/client/src/） | 硬编码变量 | 替换为 |
|---|---|---|
| `pages/CalculatorPage.tsx` | `TERM_OPTIONS`, `REPAYMENT_DATA` | `GET /api/loan/calculator-config` + `POST /api/loan/calculate` |
| `pages/CalculatorPage.tsx` | 金额 `3000`, 利率 `18.0%`, 银行 `招商银行 8648` | `GET /api/loan/calculator-config` |
| `pages/ApprovalPendingPage.tsx` | 步骤状态、惠选卡特色列表 | `GET /api/loan/approval-status/:id` |
| `pages/BenefitsCardPage.tsx` | `FEATURES`, `BENEFITS_DATA`, `TIPS`, 价格 `300`, 节省 `448` | `GET /api/benefits/card-detail` |
| `pages/ApprovalResultPage.tsx` | 金额 `¥3,000`, 步骤状态 | `GET /api/loan/approval-result/:id` |
| `pages/ConfirmRepaymentPage.tsx` | 金额 `¥1018.50`, 银行卡信息 | `GET /api/repayment/info/:loanId` |
| `pages/RepaymentSuccessPage.tsx` | 金额、时间、节省利息、Tips | `GET /api/repayment/result/:id` |

## H5 设计 Token（新增/修改页面必须遵守）

```
主色: #165dff          渐变终止: #4d8fff
强调色: #ff6b00        警告色: #fbaf19 → #ff9500
成功色: #22c55e
标题文字: #1d2129      正文: #4e5969      辅助: #86909c      禁用: #c9cdd4
页面背景: #f7f8fa      卡片: #ffffff      分割线: #f2f3f5
卡片圆角: 16px (rounded-2xl)    按钮圆角: 9999px (rounded-full)
按钮高度: 56px (h-14)           最大宽度: 440px
按钮阴影: 0px 8px 24px rgba(22,93,255,0.35)
卡片阴影: 0px 4px 20px rgba(0,0,0,0.08)
```

## H5 编码规范

1. 所有页面组件放在 `H5/client/src/pages/`，必须在 `H5/client/src/App.tsx` 注册路由
2. 所有 SVG 图标必须是内联 React 组件，放在 `H5/client/src/components/Icons.tsx`
3. 每个页面必须用 `<MobileLayout>` 包裹
4. 使用 `wouter` 的 `useLocation` 做导航
5. 只用 Tailwind 工具类，不写单独的 CSS 文件
6. 禁止使用 `any` 类型
7. API 请求统一通过 `H5/client/src/lib/api.ts` 的 axios 实例

## H5 命令

```bash
cd H5
pnpm install     # 安装依赖
pnpm dev         # 启动开发服务器 http://localhost:3000
pnpm build       # 生产构建 → H5/dist/
pnpm check       # TypeScript 类型检查
```

## H5 需清理的平台专属代码

| 文件 | 操作 |
|---|---|
| `H5/vite.config.ts` 中 `vite-plugin-manus-runtime` | 移除 import 和 plugins 引用 |
| `H5/vite.config.ts` 中 `debugCollectorPlugin` | 移除整个函数和引用 |
| `H5/client/public/__manus__/` | 删除整个目录 |
| `H5/client/index.html` 中 analytics script | 移除 |
