# 2026-04-14 工作 Checklist

> 明日主线：启动真实联调，完成 H5 主流程页面走查，并收口登录态 / Cookie / 页面映射问题。

## 状态总览

### 明日重点

- [x] 启动本地后端并打通 H5 `/api` 代理
- [x] 完成 H5 六个主页面联调走查
- [x] 完成四语真实数据展示核对
- [x] 沉淀联调问题与待确认项

### 明日边界

- [x] 不新增与联调无关的功能扩展
- [x] 不提前展开额外重构
- [x] 优先处理阻塞联调的问题，再处理体验优化

## P0：打通联调基础条件

- [x] 启动 Java 后端并确认监听 `http://localhost:8080`
- [x] 确认 `GET /api/loan/calculator-config` 经 H5 代理可访问
- [x] 确认 `NEXUSFIN_AUTH` Cookie 可用
- [x] 确认 `GET /api/users/me` 可返回当前用户
- [x] 确认 H5 首页不再出现代理 `ECONNREFUSED`

## P0：首页 `/`

- [x] 配置接口加载正常
- [ ] 金额修改抽屉可用
- [x] 期数切换触发试算
- [x] 借款试算数据展示正确
- [x] 勾选协议后可提交借款申请

## P0：审批中页 `/approval-pending`

- [x] `applicationId` 可恢复
- [x] 审批状态轮询正常
- [x] 步骤状态展示正确
- [x] 权益广告价格与文案正确
- [x] 审批完成后可自动跳转结果页

## P0：权益详情页 `/benefits-card`

- [x] 权益详情接口加载正常
- [ ] 分类 Tab 展示正常
- [ ] 协议链接展示正常
- [x] “立即开通”动作可提交

## P0：审批结果页 `/approval-result`

- [x] 审批通过场景正确
- [x] 审批拒绝场景正确
- [x] `loan_failed` 场景正确
- [x] “权益成功、借款失败”口径正确展示
- [x] `loanId` 可恢复并继续进入还款页

## P0：还款流程

- [x] `/confirm-repayment` 可加载还款信息
- [x] 提交还款可返回 `repaymentId`
- [x] `/repayment-success` 可加载还款结果
- [ ] 返回首页后流程状态可清空

## P0：多语言真实数据走查

- [x] `zh-CN` 页面展示正确
- [x] `zh-TW` 页面展示正确
- [x] `en-US` 页面展示正确
- [x] `vi-VN` 页面展示正确
- [x] 手动切换后刷新仍保留语言
- [x] 接口展示字段随 `Accept-Language` 变化

## P1：问题收口

- [x] 记录 H5 页面映射问题
- [x] 记录后端响应问题
- [x] 记录登录态 / Cookie 问题
- [x] 记录云卡 / 外部依赖问题
- [x] 输出待确认项与建议处理顺序

## 收尾检查

- [x] 如有代码修改，重新运行 `cd H5 && ./node_modules/.bin/tsc --noEmit`
- [x] 如有前端改动，重新运行 `cd H5 && npm exec --yes pnpm@10.4.1 -- build`
- [x] 更新日报 / checklist / 问题清单
