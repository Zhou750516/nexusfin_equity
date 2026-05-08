# 2026-04-30 工作 Checklist

> 今日实际主线：关闭 callback P0 线、对齐 ABS -> Yunka 真实请求协议、收口 joint-login / benefit redirect，并确认 benefiturl 主链接入缺口。

## 状态总览

### 今日已完成

- [x] callback `B-P0-3.4` 坏签名拒绝且无副作用已验证
- [x] callback P0 线 `B-P0-3.1 ~ B-P0-3.4` 已形成运行时证据闭环
- [x] `querySignStatus timeout` 已修复为受控业务错误
- [x] `applySign timeout` 已修复为受控业务错误
- [x] ABS -> Yunka 协议已按 `2026-04-10` 文档对齐
- [x] ABS backend 已可打印 Yunka request/response JSON，且同一 `traceId` 可追踪
- [x] 已真实发起 `/api/loan/calculate` 到 Yunka，并拿到上游 `KJ_NOT_READY` 明确拒绝
- [x] 已明确 QW 签约链和 Yunka 链的接口归属，避免后续混测
- [x] `push / exercise / refund` 联登入口已拆开，运行态回归通过
- [x] `POST /api/auth/redrect_benefit_url` 运行态通过，timeout / reject 不落通用 500
- [x] `CURRENT_STATE.md`、日报、decision-review、次日 plan/checklist 已回填

### 今日未闭合

- [ ] `confirmSign timeout` 仍未继续推进到最终收口
- [ ] invalid token 语义运行态闭环
- [ ] `benefiturl` 主链运行态实发证据与字段语义风险仍待后续收口

## P0：callback / 签约 / 联登

- [x] callback `B-P0-3.4` 已验证坏签名被拒绝且无副作用
- [x] callback P0 线已可判通过
- [x] `querySignStatus timeout` 运行态回归通过
- [x] `applySign timeout` 运行态回归通过
- [ ] `confirmSign timeout` 继续修复并回归
- [x] `push` 无 `benefitOrderNo` 可成功联登并进入 `/landing`
- [x] `exercise / refund` 缺少 `benefitOrderNo` 会受控失败
- [x] `exercise / refund` 带 `benefitOrderNo` 运行态联登通过

## P0：Yunka 协议与真实请求

- [x] 明确当前真实 Yunka baseUrl 与运行态模式
- [x] 对齐 `loan/trial`、header、`repay/query.loanId` 等协议字段
- [x] 真实打通一条 `/api/loan/calculate` -> Yunka 请求
- [x] 拿到完整 requestBodyJson / responseBodyJson
- [x] 将真实外部阻塞收敛为 `KJ_NOT_READY / kj.private-key 配置无效`

## P1：benefit redirect

- [x] 新增 `POST /api/auth/redrect_benefit_url`
- [x] 正常链路返回非空 `redirectUrl`
- [x] timeout 收口为 `REDRECT_BENEFIT_URL_UPSTREAM_TIMEOUT`
- [x] 上游 reject 收口为 `REDRECT_BENEFIT_URL_UPSTREAM_FAILED`
- [x] `benefiturl` 已接入 `BenefitsServiceImpl.activate -> XiaohuaGatewayService.syncBenefitOrder` 主链
- [x] `/api/benefits/activate` 已要求 token，H5 已从 joint-login session 透传
- [x] `benefiturl` 生成失败时按强依赖中断同步，不静默降级
- [ ] invalid token 语义 fault 仍缺 stub 注入入口
- [ ] `benefiturl` 主链运行态实发证据仍待补齐
- [ ] `benefiturl` 字段语义风险仍待继续确认

## 收尾检查

- [x] 今日真实完成项已写入日报
- [x] 今日关键判断已写入 decision-review
- [x] 明日 `plan / checklist` 已生成
- [x] docs 入口已切换到 `20260501`
