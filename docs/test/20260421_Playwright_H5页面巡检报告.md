# 2026-04-21 Playwright H5 页面巡检报告

## 1. 巡检目标

- 使用 Playwright 对本地 H5 页面做自动化巡检；
- 验证 H5 页面是否存在明显异常；
- 验证前端与后端接口交互是否正确；
- 记录本轮发现的问题与注意事项。

## 2. 巡检环境

- H5 地址：`http://127.0.0.1:3000/`
- 后端地址：`http://127.0.0.1:8080`
- 巡检时间：`2026-04-21`
- 巡检方式：Playwright Headless 自动化访问
- 原始日志：`test/artifacts/20260421_playwright_h5_check.json:1`

## 3. 巡检结论

### 3.1 总体结论

本轮按**正确登录方式**执行后，H5 主流程页面表现正常，前后端接口交互正常，未发现：

- 页面白屏；
- 前端 JS 异常；
- 控制台报错；
- 网络请求失败；
- 核心接口 4xx/5xx 异常。

主流程已跑通：

1. 登录回调写入 Cookie；
2. 首页试算页加载成功；
3. 借款申请提交成功；
4. 审批结果页展示成功；
5. 权益详情页加载成功；
6. 还款确认页加载成功；
7. 还款提交成功；
8. 还款成功页展示成功。

### 3.2 结论判断

- **H5 页面是否存在明显问题：** 未发现明显页面问题；
- **前后端接口交互是否正确：** 本轮验证结果为正确；
- **是否存在需要记录的注意事项：** 有，见第 5 节。

## 4. 本轮验证通过的页面与接口

### 4.1 页面验证结果

| 页面 | 访问结果 | 说明 |
| --- | --- | --- |
| `/` | 通过 | 首页正常展示借款金额、期数、收款账户、利率、协议勾选区 |
| `/approval-pending?applicationId=...` | 通过 | 页面自动跳转到审批结果页，符合当前后端返回已审批通过场景 |
| `/benefits-card?applicationId=...` | 通过 | 权益详情页正常展示价格、特色、权益内容、提示文案 |
| `/approval-result?applicationId=...` | 通过 | 正常展示审批通过、金额、步骤、到账提示 |
| `/confirm-repayment?loanId=...` | 通过 | 正常展示还款金额、银行卡、还款提示 |
| `/repayment-success?repaymentId=...` | 通过 | 正常展示还款成功、还款金额、时间、银行卡、提示信息 |

### 4.2 核心接口验证结果

| 接口 | 方法 | 结果 | 关键说明 |
| --- | --- | --- | --- |
| `/api/auth/sso-callback?token=mock-tech-token` | GET | 通过 | 正常写入 `NEXUSFIN_AUTH` Cookie，并跳转 `/equity/index` |
| `/api/loan/calculator-config` | GET | 通过 | 返回试算配置、收款账户、利率等信息 |
| `/api/loan/calculate` | POST | 通过 | 返回总息费与 3 期还款计划 |
| `/api/loan/apply` | POST | 通过 | 返回 `applicationId`、`benefitOrderNo`、`pending` 状态 |
| `/api/loan/approval-status/{applicationId}` | GET | 通过 | 返回 `approved`，页面因此自动进入审批结果页 |
| `/api/benefits/card-detail` | GET | 通过 | 返回惠选卡详情信息 |
| `/api/loan/approval-result/{applicationId}` | GET | 通过 | 返回审批通过结果与 `loanId` |
| `/api/repayment/info/{loanId}` | GET | 通过 | 返回还款金额、银行卡、还款提示 |
| `/api/repayment/submit` | POST | 通过 | 返回 `repaymentId`，状态为 `processing` |
| `/api/repayment/result/{repaymentId}` | GET | 通过 | 返回 `success`，页面进入还款成功页 |

## 5. 发现的问题与注意事项

### 5.1 登录回跳地址限制

本轮 Playwright 首次执行时，曾使用以下方式登录：

`/api/auth/sso-callback?token=mock-tech-token&redirect_url=http://127.0.0.1:3000/`

后端返回：

- `REDIRECT_URL_INVALID`
- `Redirect url is not allowed`

原因：

- 当前后端只允许白名单中的回跳地址；
- 绝对地址 `http://127.0.0.1:3000/` 不在当前白名单内；
- 后端配置当前允许前缀为：
  - `/equity/`
  - `https://equity.nexusfin.com/`

影响：

- 使用绝对 `redirect_url` 时，浏览器拿不到登录态；
- H5 首页会继续请求 `/api/loan/calculator-config`；
- 因缺失 Cookie，接口统一返回 `401 Missing auth cookie`。

当前规避方式：

- 先访问 `http://127.0.0.1:8080/api/auth/sso-callback?token=mock-tech-token`
- 确认 Cookie 写入后，再打开 `http://127.0.0.1:3000/`

结论：

- 这不是本轮 H5 页面本身的功能缺陷；
- 属于**本地联调登录方式**需要注意的约束；
- 建议后续统一沉淀为固定联调指引。

## 6. 本轮关键返回值

- `applicationId`：`APP-0035f85668fe4b4e86c2a35746643635`
- `loanId`：`LN-f5b89e1650b94d239ca26a2b7cbf0fbb`
- `repaymentId`：`RP-20260421-0001`

## 7. 自动化巡检观察

- 控制台错误数：`0`
- 页面运行时错误数：`0`
- 请求失败数：`0`
- 采集到的核心 API 请求数：`10`

## 8. 结论建议

### 8.1 当前可确认

- 当前本地 H5 主流程可以正常跑通；
- 当前前端到后端接口链路可以正常联通；
- 当前返回数据结构可以支撑页面展示与主流程跳转。

### 8.2 建议后续补充

- 将“本地联调登录方式”统一写入测试入口文档；
- 如后续要支持外部绝对地址回跳，需要评估是否调整 SSO 回跳白名单；
- 后续可继续补做：
  - 多语言切换自动巡检；
  - 异常链路巡检；
  - 数据落库校验与页面动作的自动对账。
