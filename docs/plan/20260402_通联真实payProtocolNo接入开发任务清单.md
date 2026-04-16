# 2026-04-02 通联真实 `payProtocolNo` 接入开发任务清单

## 1. 任务目标

基于 `docs/third-part/20260402_通联真实payProtocolNo接入整体设计方案.md`，拆解后续开发任务，目标是把齐为下单链路切换到真实通联协议号来源。

## 2. 任务拆分

### T1：数据库与实体模型

1. 新增 `member_payment_protocol` 表
2. 扩展 `benefit_order`：
   - `pay_protocol_no_snapshot`
   - `pay_protocol_source`
3. 新增：
   - `MemberPaymentProtocol` 实体
   - `MemberPaymentProtocolRepository`

### T2：协议号服务层

1. 新增 `PaymentProtocolService`
2. 新增 `PaymentProtocolServiceImpl`
3. 实现：
   - 写入有效协议号
   - 查询当前有效协议号
   - 解析齐为下单使用的协议号

### T3：下单主流程改造

1. 改造 `BenefitOrderServiceImpl`
2. 下单前改为优先查询真实通联协议号
3. 订单写入：
   - `pay_protocol_no_snapshot`
   - `pay_protocol_source`
4. 无真实协议号时：
   - 测试环境按配置决定是否允许 override
   - 生产环境直接失败

### T4：配置与环境边界

1. 保留 `QW_MEMBER_SYNC_PAY_PROTOCOL_NO_OVERRIDE`
2. 增加“是否允许测试 override”的开关
3. 明确测试 / 预发 / 生产的环境约束

### T5：测试用例

至少覆盖：

1. 有真实协议号时优先使用真实协议号
2. 无真实协议号但测试 override 开启时使用 override
3. 无真实协议号且 override 不可用时明确失败
4. 订单快照字段正确写入

### T6：文档与上线切换

1. 更新齐为部署文档
2. 更新通联接入说明
3. 补一份上线切换 checklist：
   - 真实协议号来源已接通
   - override 已关闭
   - 订单快照来源均为 `ALLINPAY`

## 3. 推荐执行顺序

1. `T1 数据模型`
2. `T2 协议号服务`
3. `T3 下单主链改造`
4. `T5 测试用例`
5. `T4 配置边界`
6. `T6 文档与上线切换`

## 4. 当前建议

建议后续实现时，严格控制范围：

1. 本轮只解决真实协议号接入闭环
2. 不顺手扩大到通联完整支付系统
3. 不重构已验证通过的齐为三接口主链接线
