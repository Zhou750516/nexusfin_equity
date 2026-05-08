# 2026-05-01 工作 Checklist

> 今日实际主线：收紧联调边界、统一联调文档口径、闭合 repayment 线 3 个真实问题，并把后续测试重新收口到剩余 `pending case`。

## 状态总览

### 今日已完成

- [x] 联调边界已正式收紧为只看艾博生直接边界
- [x] 对外问题默认已收紧为只追云卡、齐为
- [x] `docs/test/20260506_联调case设计方案.md` 已改成当前可执行版本
- [x] `docs/test/20260506_联调结果.md` 已建立并持续更新
- [x] `docs/test/20260506_联调case覆盖矩阵.md` 已建立并持续更新
- [x] `benefiturl` 主链接线与运行态口径已完成拆分管理
- [x] `/api/repayment/submit` timeout 通用 500 问题已修复
- [x] 超额还款未拦截问题已修复
- [x] 重复还款未防重问题已修复
- [x] `EX-E.1`、`EX-E.3`、`EX-E.4` 已运行态回归通过
- [x] 日报、decision-review、次日 plan/checklist 已回填

### 今日未闭合

- [ ] 签约主链和签约异常剩余 `pending case`
- [ ] 借款主链和审批结果剩余 `pending case`
- [ ] `M-B.11` 还款完整主链样本仍待补证据
- [ ] `EX-A.3 token 篡改` 是否可稳定构造仍待确认
- [ ] `EX-F.3` 当前是否仍因 stub 能力不足而 `BLOCKED`
- [ ] `benefiturl` 字段语义风险仍待外部继续确认

## P0：联调边界与文档基线

- [x] 明确“艾博生不直接感知科技平台服务”
- [x] 明确科技平台细节由云卡统一收口
- [x] 除非存在直接 `token / 回调 / 跳转` 边界，否则不向科技平台索要接口级细节
- [x] 联调 case 文档已切到当前可执行版本
- [x] 覆盖矩阵已开始用于差量推进

## P0：repayment 关键问题

- [x] `EX-E.3` timeout 不再回归成 HTTP 500
- [x] `EX-E.1` 超额还款会在本地拦截
- [x] `EX-E.1` 超额时不会继续打 `/repay/apply`
- [x] `EX-E.4` 同窗口重复还款会受控失败
- [x] `EX-E.4` 第二次请求不会重复打 `/repay/apply`

## P1：剩余风险管理

- [x] `benefiturl` 已主链接上，但语义风险继续单独保留
- [x] repayment 重复防重方案已明确为 `120` 秒短窗口最小可用方案
- [ ] `benefiturl` 外部语义是否接受仍待确认
- [ ] `EX-A.3`、`EX-F.3` 是否受 stub 限制仍待确认

## 收尾检查

- [x] 今日真实完成项已写入日报
- [x] 今日关键判断已写入 decision-review
- [x] 明日 `plan / checklist` 已生成
- [x] 后续测试会话任务已重新收口到剩余 `pending case`
