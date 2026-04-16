# 2026-04-02 通联真实 `payProtocolNo` 接入整体设计方案（阶段性假设）

## 1. 文档目的

本文档用于基于 `2026-04-02` 当天齐为三条主动接口真实联调结果，输出一份可直接指导后续改造的整体设计方案。

本方案聚焦解决当时最核心的正式生产阻塞点：

> 基于 `2026-04-02` 当时齐为联调口径，`abs.push.order` 所需的 `payProtocolNo` 指向**通联真实签约协议号**，不能继续使用 mock 协议号、内部 `agreementNo` 或 `proto-` 占位值。

补充说明：

1. 截至 `2026-04-03`，支付渠道后续可能调整
2. 因此本文应视为“按通联假设形成的阶段性设计方案”
3. 其中关于“用户级协议号资产、回写入口、订单快照、override 边界”的设计仍然有效
4. 其中关于“最终渠道一定为通联”的表述，不应再直接视为最终定版结论

本文档覆盖：

1. 业务流程设计
2. 表设计与字段定义
3. 主流程取值规则
4. 代码改造点
5. 配置与上线切换策略
6. 开发任务拆分原则

本文档**不直接展开**以下内容：

1. 通联完整支付能力平台化设计
2. 通联其它支付接口的协议细节
3. 齐为回调我方公网接入细节

## 2. 当前背景与问题收敛

截至 `2026-04-02`，已确认如下事实：

1. 齐为主动接口三条线已经完成真实联调：
   - `abs.push.order`
   - `abs.token.get`
   - `abs.lending.notify`
2. `504 数据协议非法` 已经解决，不再是当前主问题
3. `515 会员卡未生效` 已定位为：
   - 齐为首扣失败
   - 首扣失败原因是 `payProtocolNo` 错误
4. 齐为已明确：
   - 真实 `payProtocolNo` 来自艾博生与通联签约后返回的协议号
   - 一个用户对应一个协议号
5. 当前项目内：
   - 尚未接通联真实签约流程
   - 因而没有真实可用的协议号来源
6. 当前测试环境为了继续验证齐为链路，临时引入了：
   - `QW_MEMBER_SYNC_PAY_PROTOCOL_NO_OVERRIDE`
   - 齐为提供的 mock 协议号

因此当前问题已经完全收敛为：

> 需要为主流程补上一条“真实支付协议号获取 -> 落库 -> 下单取值 -> 传给齐为”的闭环；当时按通联口径做了第一轮收敛。

## 3. 设计目标与范围

## 3.1 本次设计目标

本次采用 **C 方案：最小闭环落地，但结构上预留为可扩展签约能力模块**。

目标如下：

1. 在不推翻当前齐为主链接线的前提下，补足真实协议号来源
2. 让 `abs.push.order` 后续优先使用真实通联协议号
3. 明确区分：
   - 内部协议/合同标识
   - 真实支付协议号
4. 为后续扩展：
   - 重新签约
   - 协议失效
   - 多渠道协议管理
   预留清晰结构

## 3.2 本次不做的事

本次设计不建议顺手扩大到：

1. 通联全量支付系统能力中心
2. 解约、换绑、签约状态查询的完整产品化流程
3. 所有支付渠道统一抽象的大重构

原因：

1. 当前最紧迫的生产问题只与 `payProtocolNo` 真实来源有关
2. 齐为三接口已打通，当前最优先是补生产闭环，而不是扩边界

## 4. 设计原则

本次方案遵循以下原则：

### 4.1 用户资产与订单快照分层

真实支付协议号是**用户级资产**，不是订单临时字段。

因此：

1. 用户当前有效协议号应放在独立表中管理
2. 订单表只保留“本单实际使用了哪个协议号”的快照

### 4.2 不再混用内部 `agreementNo`

`benefit_order.agreement_no` 当前是我方内部协议/合同标识，后续必须继续保留其原职责，不能再被当成齐为扣款所需的真实支付协议号。

### 4.3 测试 override 与正式主流程隔离

`QW_MEMBER_SYNC_PAY_PROTOCOL_NO_OVERRIDE` 可以继续保留，但只能作为**测试环境联调能力**。

正式主流程必须明确：

1. 优先使用真实通联协议号
2. 生产环境禁止依赖 override

### 4.4 改造点收口到主链最短路径

这次不重做齐为 client，不重做通知链路，而是聚焦于：

1. 协议号来源
2. 落库
3. 取值
4. 订单快照

## 5. 整体架构设计

## 5.1 推荐架构

推荐增加一个独立的“支付协议号能力层”，位于业务主链和具体协议来源之间。

建议结构如下：

1. 主业务链：
   - 用户签约成功后，获取真实协议号
   - 协议号入库
   - 下单时解析当前有效协议号
   - 将协议号传给齐为 `abs.push.order`
2. 协议号管理层：
   - `PaymentProtocolService`
3. 协议号来源层：
   - `PaymentProtocolProvider`
   - 初期可对接支付侧/中台
   - 后续可替换为通联直连实现

## 5.2 为什么不直接把协议号写死在订单表

不推荐只在 `benefit_order` 上追加一个“当前协议号字段”来当唯一来源，原因如下：

1. 协议号本质是用户级绑定关系，不是订单级临时状态
2. 一个用户可能会在后续重新签约、协议失效或换绑
3. 如果只放订单表，后续很难快速得到“当前有效协议号”

因此推荐：

1. 用户维度维护当前有效协议号
2. 订单维度保留调用快照

## 6. 表设计

## 6.1 新增表：`member_payment_protocol`

### 6.1.1 表定位

用于维护用户当前有效的支付协议号关系。

### 6.1.2 建议字段

| 字段 | 类型 | 是否必填 | 含义 |
| --- | --- | --- | --- |
| `id` | `bigint` | 是 | 主键 |
| `member_id` | `varchar(64)` | 是 | 会员 ID |
| `external_user_id` | `varchar(64)` | 是 | 外部用户标识 |
| `provider_code` | `varchar(64)` | 是 | 支付渠道编码，当前建议 `ALLINPAY` |
| `protocol_no` | `varchar(128)` | 是 | 真实支付协议号 |
| `protocol_status` | `varchar(32)` | 是 | 协议状态 |
| `sign_request_no` | `varchar(64)` | 否 | 签约请求号 / 外部流水 |
| `channel_code` | `varchar(64)` | 否 | 来源渠道 |
| `signed_ts` | `timestamp` | 否 | 签约成功时间 |
| `expired_ts` | `timestamp` | 否 | 协议失效时间 |
| `last_verified_ts` | `timestamp` | 否 | 最近一次核验时间 |
| `created_ts` | `timestamp` | 是 | 创建时间 |
| `updated_ts` | `timestamp` | 是 | 更新时间 |

### 6.1.3 建议状态值

建议先统一以下枚举：

1. `ACTIVE`
2. `INACTIVE`
3. `EXPIRED`
4. `REVOKED`

其中当前齐为主链只依赖：

1. `ACTIVE`：可用于 `abs.push.order`
2. 非 `ACTIVE`：不可用于扣款

### 6.1.4 建议索引

1. `unique key uk_provider_protocol_no (provider_code, protocol_no)`
2. `key idx_member_provider_status (member_id, provider_code, protocol_status)`
3. `key idx_external_user_provider_status (external_user_id, provider_code, protocol_status)`

## 6.2 扩展表：`benefit_order`

### 6.2.1 扩展字段

建议新增两个字段：

| 字段 | 类型 | 是否必填 | 含义 |
| --- | --- | --- | --- |
| `pay_protocol_no_snapshot` | `varchar(128)` | 否 | 本单实际使用的协议号快照 |
| `pay_protocol_source` | `varchar(32)` | 否 | 本单协议号来源 |

### 6.2.2 来源建议值

`pay_protocol_source` 建议值：

1. `ALLINPAY`
2. `TEST_OVERRIDE`
3. `FALLBACK`

当前正式推荐仅允许：

1. 测试环境出现 `TEST_OVERRIDE`
2. 生产环境最终应仅出现 `ALLINPAY`

### 6.2.3 为什么要保留快照

因为后续人工排障和审计会关注：

1. 这个订单当时到底用了哪个协议号
2. 这个协议号来自真实通联还是测试 override

如果不留快照，只保留用户当前协议号，后续很难还原历史现场。

## 7. 字段职责边界

## 7.1 继续保留的内部字段

`benefit_order.agreement_no` 继续作为：

1. 我方内部协议/合同编号
2. 协议归档、签章、审计链路的内部标识

后续不能再把它当成：

1. 通联真实支付协议号
2. 齐为扣款协议号

## 7.2 新增的真实支付字段

`member_payment_protocol.protocol_no` 承担：

1. 用户当前真实支付协议号
2. 下单时齐为所需 `payProtocolNo` 的主要来源

## 7.3 订单快照字段

`benefit_order.pay_protocol_no_snapshot` 承担：

1. 本单实际对外发给齐为的协议号留痕

`benefit_order.pay_protocol_source` 承担：

1. 说明该协议号来自真实通联还是测试 override

## 8. 主流程取值规则

## 8.1 协议号写入时机

当通联签约完成后，应将真实协议号写入 `member_payment_protocol`。

建议规则：

1. 同一用户当前仅保留一个 `ACTIVE` 协议号
2. 若用户重新签约：
   - 旧协议号置为非 `ACTIVE`
   - 新协议号置为 `ACTIVE`

## 8.2 下单时的协议号解析规则

创建权益订单并准备调用齐为 `abs.push.order` 时，协议号取值规则建议固定为：

### 一级优先级：真实通联协议号

按 `memberId` 查询：

1. `provider_code=ALLINPAY`
2. `protocol_status=ACTIVE`
3. 最近更新的一条记录

若命中，则：

1. 用该值填充 `QwMemberSyncRequest.payProtocolNo`
2. 同时写入：
   - `benefit_order.pay_protocol_no_snapshot`
   - `benefit_order.pay_protocol_source=ALLINPAY`

### 二级优先级：测试 override

若未命中真实协议号，则：

1. 若当前环境明确允许测试 override
2. 且配置了 `QW_MEMBER_SYNC_PAY_PROTOCOL_NO_OVERRIDE`

则：

1. 使用 override 值继续联调
2. 订单快照写：
   - `pay_protocol_no_snapshot=<override>`
   - `pay_protocol_source=TEST_OVERRIDE`

### 三级策略：正式环境禁止 fallback

若既没有真实协议号，又没有测试 override，则：

1. 测试环境可按需要继续保留老 fallback 观察期
2. 生产环境应直接阻断，并返回明确错误码，例如：
   - `PAY_PROTOCOL_NOT_FOUND`

推荐最终目标是：

1. 测试环境允许 `TEST_OVERRIDE`
2. 生产环境仅允许 `ALLINPAY`
3. 废弃 `proto-` 兜底策略

## 8.3 对后续两个齐为接口的影响

### `abs.token.get`

无需新增协议号逻辑，继续使用：

1. `uniqueId`
2. `partnerOrderNo`

### `abs.lending.notify`

无需新增协议号逻辑，继续使用：

1. `uniqueId`
2. `partnerOrderNo`
3. `status`

因此本次主流程改造真正影响的只有：

1. 协议号来源
2. 下单同步链路
3. 订单快照

## 9. 代码改造点

## 9.1 新增实体与仓储

建议新增：

1. `src/main/java/com/nexusfin/equity/entity/MemberPaymentProtocol.java`
2. `src/main/java/com/nexusfin/equity/repository/MemberPaymentProtocolRepository.java`

实体职责：

1. 表达用户与支付协议号的绑定关系
2. 提供当前有效协议号查询能力

## 9.2 新增协议号服务层

建议新增：

1. `src/main/java/com/nexusfin/equity/service/PaymentProtocolService.java`
2. `src/main/java/com/nexusfin/equity/service/impl/PaymentProtocolServiceImpl.java`

建议方法：

1. `upsertActiveProtocol(...)`
2. `findActiveProtocol(memberId, externalUserId, providerCode)`
3. `resolveProtocolForQw(...)`

职责边界：

1. 屏蔽主流程对具体表结构的感知
2. 屏蔽测试 override 与真实协议号的优先级判断

## 9.3 新增协议号来源适配层

为了后续既能支持“支付中台回写协议号”，也能支持“通联直连获取协议号”，建议引入一个轻量 Provider 接口。

建议新增：

1. `src/main/java/com/nexusfin/equity/service/paymentprotocol/PaymentProtocolProvider.java`

后续可有两类实现：

1. `AllinpayPaymentProtocolProvider`
2. `MockPaymentProtocolProvider`

说明：

1. 本次不要求立即把 Provider 做成很重的抽象体系
2. 只需要确保未来真实来源切换时，不用重改 `BenefitOrderServiceImpl`

## 9.4 改造下单主链

重点改造文件：

- `src/main/java/com/nexusfin/equity/service/impl/BenefitOrderServiceImpl.java`

当前逻辑问题：

1. 优先使用 `agreementNo`
2. 否则回退 `proto-` + `benefitOrderNo`

建议改为：

1. 优先取 `PaymentProtocolService.resolveProtocolForQw(...)`
2. 若解析到真实协议号，则正常下发
3. 若未解析到，但测试环境允许 override，则使用 override
4. 若生产环境无真实协议号，则直接失败

## 9.5 配置层改造

当前已存在：

1. `QW_MEMBER_SYNC_PAY_PROTOCOL_NO_OVERRIDE`

建议补充一个限制开关，例如：

1. `nexusfin.third-party.qw.allow-test-pay-protocol-override`

目标：

1. 测试环境：可显式开启
2. 生产环境：默认关闭

从而避免误把 override 带入生产。

## 9.6 数据库脚本改造

需要修改：

1. `src/main/resources/db/schema.sql`
2. `src/test/resources/db/schema-h2.sql`

改造内容：

1. 新增 `member_payment_protocol`
2. 扩展 `benefit_order` 两个快照字段

## 10. 业务流程设计

## 10.1 推荐正式流程

推荐正式主流程如下：

1. 用户在支付侧完成通联签约
2. 系统拿到真实 `protocolNo`
3. 写入 `member_payment_protocol`
4. 用户创建权益订单
5. 下单链路从协议号服务解析真实 `protocolNo`
6. 调用齐为 `abs.push.order`
7. 后续继续：
   - `abs.token.get`
   - `abs.lending.notify`

## 10.2 失败分支

### 情况 A：未拿到真实协议号

1. 测试环境：
   - 可使用 override
2. 生产环境：
   - 阻断下单同步
   - 返回明确错误码

### 情况 B：协议号状态无效

若协议状态不是 `ACTIVE`：

1. 不允许透传给齐为
2. 应提示重新签约或等待支付侧处理

## 11. 上线切换策略

## 11.1 测试阶段

允许：

1. 真实协议号尚未接通时保留 override
2. 继续跑齐为链路联调

## 11.2 预发阶段

建议至少验证：

1. 真实协议号写库
2. 下单时优先读取真实协议号
3. 订单快照正确留痕
4. 未配置 override 时仍可闭环

## 11.3 生产阶段

必须满足：

1. 通联真实协议号来源已接通
2. `QW_MEMBER_SYNC_PAY_PROTOCOL_NO_OVERRIDE` 未启用
3. `pay_protocol_source` 实际均为 `ALLINPAY`

## 12. 风险与控制

## 12.1 风险一：真实协议号仍未能稳定回写

控制方式：

1. 协议号能力层与主流程解耦
2. 生产环境无协议号时直接失败，避免静默错误扣款

## 12.2 风险二：内部字段再次被误用

控制方式：

1. 明确 `agreement_no` 与 `protocol_no` 的职责边界
2. 代码中统一通过 `PaymentProtocolService` 解析

## 12.3 风险三：测试配置误带生产

控制方式：

1. 增加 override 显式开关
2. 部署清单要求生产环境清理 override
3. 订单快照保留来源字段，便于审计

## 13. 推荐结论

本次推荐采用如下最终方案：

1. 新增用户级支付协议号表 `member_payment_protocol`
2. 扩展订单表，保留协议号快照与来源
3. 新增 `PaymentProtocolService`
4. 下单主链统一通过协议号服务解析 `payProtocolNo`
5. 保留测试 override，但仅允许测试环境使用
6. 生产环境切换到真实通联协议号来源，废弃旧 fallback

## 14. 一句话总结

> 这次最合理的方案，不是把 mock 协议号硬塞进主流程，而是补一个“用户级真实通联协议号管理 + 订单级调用快照”的最小正式闭环；这样既能尽快解决齐为生产阻塞，又不会把后续通联签约能力做死。
