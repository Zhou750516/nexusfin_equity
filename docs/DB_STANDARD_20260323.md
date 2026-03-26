# NexusFin Equity 数据库表文档

- 生成日期: `2026-03-23`
- 代码基线: `main` 分支当前最新代码
- 数据库名称: `nexusfin_equity`
- 数据库类型: `MySQL 8.0`
- 建表脚本来源: `src/main/resources/db/schema.sql`

## 1. 文档范围

本文档根据当前代码中的建表脚本、实体类和服务实现整理，覆盖以下 9 张业务表：

1. `member_info`
2. `member_channel`
3. `benefit_product`
4. `benefit_order`
5. `payment_record`
6. `sign_task`
7. `contract_archive`
8. `notification_receive_log`
9. `idempotency_record`

## 2. 数据库概览

### 2.1 业务分层

| 分层 | 表名 | 说明 |
| --- | --- | --- |
| 会员域 | `member_info` | 会员基础信息，含敏感信息密文和 hash |
| 会员域 | `member_channel` | 渠道与会员绑定关系 |
| 产品域 | `benefit_product` | 权益产品主数据 |
| 订单域 | `benefit_order` | 权益订单主表，承载流程主状态 |
| 支付域 | `payment_record` | 首扣、兜底代扣等支付流水 |
| 协议域 | `sign_task` | 协议签署任务 |
| 协议域 | `contract_archive` | 合同归档信息 |
| 通知审计域 | `notification_receive_log` | 外部通知接收与处理日志 |
| 幂等控制域 | `idempotency_record` | 幂等请求处理记录 |

### 2.2 当前设计特征

- 使用 MyBatis-Plus，数据库字段下划线命名，Java 属性驼峰命名。
- 当前建表脚本未显式声明外键约束，表间关系由业务代码维护。
- 所有核心业务表都使用业务主键或显式主键，不依赖隐藏行号。
- 关键请求链路通过 `request_id` 和 `benefit_order_no` 串联。

### 2.3 金额与时间字段约定

- 金额字段均为 `bigint`，单位为分。
- 时间字段使用 `timestamp`。
- 代码侧实体时间类型为 `LocalDateTime`。

## 3. 关系总览

### 3.1 主要逻辑关系

| 主表 | 关联字段 | 从表 | 关系说明 |
| --- | --- | --- | --- |
| `member_info.member_id` | `member_id` | `member_channel` | 一个会员可关联多个渠道绑定记录 |
| `member_info.member_id` | `member_id` | `benefit_order` | 一个会员可创建多笔权益订单 |
| `benefit_product.product_code` | `product_code` | `benefit_order` | 一个产品可对应多笔订单 |
| `benefit_order.benefit_order_no` | `benefit_order_no` | `payment_record` | 一笔订单可有多条支付流水 |
| `benefit_order.benefit_order_no` | `benefit_order_no` | `sign_task` | 一笔订单对应多条签约任务 |
| `sign_task.task_no` | `task_no` | `contract_archive` | 一条签约任务对应一条合同归档 |
| `benefit_order.benefit_order_no` | `benefit_order_no` | `notification_receive_log` | 一笔订单可收到多条通知 |
| 任意业务请求 `request_id` | `request_id` | `idempotency_record` | 幂等记录保存请求是否已处理 |

### 3.2 典型数据链路

1. 注册用户时写入 `member_info`、`member_channel`、`idempotency_record`
2. 创建订单时写入 `benefit_order`、`sign_task`、`contract_archive`、`idempotency_record`
3. 支付回调时写入 `payment_record`，更新 `benefit_order`，再写入 `idempotency_record`
4. 通知回调时写入 `notification_receive_log`，按结果更新 `benefit_order`，再写入 `idempotency_record`

## 4. 表明细

---

## 4.1 `member_info`

- 表名: `member_info`
- 用途: 会员主档表，保存会员身份信息和敏感字段密文
- 主键: `member_id`

### 字段定义

| 字段名 | 类型 | 允许空 | 主键 | 说明 |
| --- | --- | --- | --- | --- |
| `member_id` | `varchar(64)` | 否 | 是 | 会员 ID |
| `external_user_id` | `varchar(64)` | 否 | 否 | 外部用户 ID |
| `mobile_encrypted` | `varchar(512)` | 否 | 否 | 手机号密文 |
| `mobile_hash` | `varchar(128)` | 否 | 否 | 手机号 hash，用于检索归并 |
| `id_card_encrypted` | `varchar(512)` | 否 | 否 | 身份证密文 |
| `id_card_hash` | `varchar(128)` | 否 | 否 | 身份证 hash，用于检索归并 |
| `real_name_encrypted` | `varchar(512)` | 否 | 否 | 姓名密文 |
| `member_status` | `varchar(32)` | 否 | 否 | 会员状态 |
| `created_ts` | `timestamp` | 否 | 否 | 创建时间 |
| `updated_ts` | `timestamp` | 否 | 否 | 更新时间 |

### 当前代码中的典型值

| 字段 | 典型值 |
| --- | --- |
| `member_status` | `ACTIVE` / `INACTIVE` |

### 写入与使用说明

- 用户静默注册时写入。
- 代码通过 `mobile_hash + id_card_hash` 判断是否已存在同一会员。
- 敏感字段不明文保存，查询归并依赖 hash 字段。

### 关系说明

- 通过 `member_id` 关联 `member_channel`
- 通过 `member_id` 关联 `benefit_order`

---

## 4.2 `member_channel`

- 表名: `member_channel`
- 用途: 渠道用户与平台会员绑定关系表
- 主键: `id`
- 唯一键: `uk_channel_external_user(channel_code, external_user_id)`

### 字段定义

| 字段名 | 类型 | 允许空 | 主键 | 说明 |
| --- | --- | --- | --- | --- |
| `id` | `bigint` | 否 | 是 | 自增主键 |
| `member_id` | `varchar(64)` | 否 | 否 | 会员 ID |
| `source_channel_code` | `varchar(64)` | 否 | 否 | 来源渠道编码 |
| `external_user_id` | `varchar(64)` | 否 | 否 | 渠道侧用户 ID |
| `bind_status` | `varchar(32)` | 否 | 否 | 绑定状态 |
| `created_ts` | `timestamp` | 否 | 否 | 创建时间 |
| `updated_ts` | `timestamp` | 否 | 否 | 更新时间 |

### 当前代码中的典型值

| 字段 | 典型值 |
| --- | --- |
| `bind_status` | `BOUND` |

### 写入与使用说明

- 用户静默注册成功时写入。
- 同一 `channel_code + external_user_id` 只能绑定一次。
- 创建权益订单时，会取该会员最近一条渠道绑定记录回填 `source_channel_code` 和 `external_user_id`。

---

## 4.3 `benefit_product`

- 表名: `benefit_product`
- 用途: 权益产品主数据表
- 主键: `product_code`

### 字段定义

| 字段名 | 类型 | 允许空 | 主键 | 说明 |
| --- | --- | --- | --- | --- |
| `product_code` | `varchar(64)` | 否 | 是 | 产品编码 |
| `product_name` | `varchar(128)` | 否 | 否 | 产品名称 |
| `fee_rate` | `int` | 否 | 否 | 费率 |
| `status` | `varchar(32)` | 否 | 否 | 产品状态 |
| `created_ts` | `timestamp` | 否 | 否 | 创建时间 |
| `updated_ts` | `timestamp` | 否 | 否 | 更新时间 |

### 当前代码中的典型值

| 字段 | 典型值 |
| --- | --- |
| `status` | `ACTIVE` |

### 写入与使用说明

- 当前主要用于产品页查询和订单创建前校验。
- 代码要求产品存在且 `status='ACTIVE'`，否则拒绝下单。

---

## 4.4 `benefit_order`

- 表名: `benefit_order`
- 用途: 权益订单主表，承载订单主状态与关键链路标识
- 主键: `benefit_order_no`

### 字段定义

| 字段名 | 类型 | 允许空 | 主键 | 说明 |
| --- | --- | --- | --- | --- |
| `benefit_order_no` | `varchar(64)` | 否 | 是 | 权益订单号 |
| `member_id` | `varchar(64)` | 否 | 否 | 会员 ID |
| `source_channel_code` | `varchar(64)` | 否 | 否 | 来源渠道编码 |
| `external_user_id` | `varchar(64)` | 否 | 否 | 外部用户 ID |
| `product_code` | `varchar(64)` | 否 | 否 | 产品编码 |
| `agreement_no` | `varchar(64)` | 是 | 否 | 协议号 |
| `loan_amount` | `bigint` | 否 | 否 | 借款金额，单位分 |
| `order_status` | `varchar(32)` | 否 | 否 | 订单主状态 |
| `first_deduct_status` | `varchar(32)` | 否 | 否 | 首扣状态 |
| `fallback_deduct_status` | `varchar(32)` | 否 | 否 | 兜底代扣状态 |
| `exercise_status` | `varchar(32)` | 否 | 否 | 行权状态 |
| `refund_status` | `varchar(32)` | 否 | 否 | 退款状态 |
| `grant_status` | `varchar(32)` | 否 | 否 | 放款状态 |
| `loan_order_no` | `varchar(64)` | 是 | 否 | 下游借据号 |
| `sync_status` | `varchar(32)` | 否 | 否 | 下游同步状态 |
| `request_id` | `varchar(64)` | 否 | 否 | 创建订单请求号 |
| `created_ts` | `timestamp` | 否 | 否 | 创建时间 |
| `updated_ts` | `timestamp` | 否 | 否 | 更新时间 |

### 当前代码中的典型状态值

| 字段 | 典型值 |
| --- | --- |
| `order_status` | `FIRST_DEDUCT_PENDING`、`FIRST_DEDUCT_SUCCESS`、`FIRST_DEDUCT_FAIL`、`FALLBACK_DEDUCT_PENDING`、`FALLBACK_DEDUCT_SUCCESS`、`FALLBACK_DEDUCT_FAIL`、`EXERCISE_PENDING`、`EXERCISE_SUCCESS`、`EXERCISE_FAIL`、`REFUND_SUCCESS`、`REFUND_FAIL`、`SYNC_PENDING`、`SYNC_SUCCESS`、`SYNC_FAIL` |
| `first_deduct_status` | `NONE` / `PENDING` / `SUCCESS` / `FAIL` |
| `fallback_deduct_status` | `NONE` / `PENDING` / `SUCCESS` / `FAIL` |
| `exercise_status` | `NONE` / `SUCCESS` / `FAIL` |
| `refund_status` | `NONE` / `SUCCESS` / `FAIL` |
| `grant_status` | `PENDING` / `SUCCESS` / `FAIL` |
| `sync_status` | `SYNC_PENDING` / `SYNC_SUCCESS` / `SYNC_FAIL` |

### 写入与使用说明

- 创建订单时插入一条记录。
- 首扣、兜底代扣、放款、行权、退款回调都会更新该表状态。
- `benefit_order_no` 是外部主业务单号，也是支付、签约、通知日志的核心关联键。
- `request_id` 保存订单创建请求号，用于幂等回放时反查业务结果。

### 当前默认初始化值

| 字段 | 默认写入值 |
| --- | --- |
| `order_status` | `FIRST_DEDUCT_PENDING` |
| `first_deduct_status` | `PENDING` |
| `fallback_deduct_status` | `NONE` |
| `exercise_status` | `NONE` |
| `refund_status` | `NONE` |
| `grant_status` | `PENDING` |
| `sync_status` | `SYNC_PENDING` |

### 关系说明

- 关联 `member_info.member_id`
- 关联 `benefit_product.product_code`
- 关联 `payment_record.benefit_order_no`
- 关联 `sign_task.benefit_order_no`
- 关联 `notification_receive_log.benefit_order_no`

---

## 4.5 `payment_record`

- 表名: `payment_record`
- 用途: 支付流水表，记录首扣和兜底代扣回调结果
- 主键: `payment_no`

### 字段定义

| 字段名 | 类型 | 允许空 | 主键 | 说明 |
| --- | --- | --- | --- | --- |
| `payment_no` | `varchar(64)` | 否 | 是 | 支付流水号 |
| `benefit_order_no` | `varchar(64)` | 否 | 否 | 权益订单号 |
| `payment_type` | `varchar(32)` | 否 | 否 | 支付类型 |
| `provider_code` | `varchar(64)` | 否 | 否 | 执行方编码 |
| `channel_trade_no` | `varchar(64)` | 是 | 否 | 渠道交易流水号 |
| `amount` | `bigint` | 否 | 否 | 支付金额，单位分 |
| `payment_status` | `varchar(32)` | 否 | 否 | 支付状态 |
| `fail_reason` | `varchar(256)` | 是 | 否 | 失败原因 |
| `request_id` | `varchar(64)` | 是 | 否 | 支付回调请求号 |
| `created_ts` | `timestamp` | 否 | 否 | 创建时间 |
| `updated_ts` | `timestamp` | 否 | 否 | 更新时间 |

### 当前代码中的典型值

| 字段 | 典型值 |
| --- | --- |
| `payment_type` | `FIRST_DEDUCT` / `FALLBACK_DEDUCT` |
| `provider_code` | `QW` |
| `payment_status` | `PENDING` / `SUCCESS` / `FAIL` |

### 写入与使用说明

- 首扣回调成功或失败时插入。
- 放款成功且满足兜底条件时，会先创建一笔 `FALLBACK_DEDUCT`、`PENDING` 的待处理记录。
- 兜底代扣回调到达后，会再根据回调状态更新订单并新增/回放支付记录。
- 代码通过 `request_id` 做支付回调幂等。

### 关系说明

- 通过 `benefit_order_no` 关联 `benefit_order`

---

## 4.6 `sign_task`

- 表名: `sign_task`
- 用途: 协议签署任务表
- 主键: `task_no`

### 字段定义

| 字段名 | 类型 | 允许空 | 主键 | 说明 |
| --- | --- | --- | --- | --- |
| `task_no` | `varchar(64)` | 否 | 是 | 签署任务号 |
| `benefit_order_no` | `varchar(64)` | 否 | 否 | 权益订单号 |
| `contract_type` | `varchar(64)` | 否 | 否 | 合同类型 |
| `sign_url` | `varchar(512)` | 否 | 否 | 签署链接 |
| `sign_status` | `varchar(32)` | 否 | 否 | 签署状态 |
| `created_ts` | `timestamp` | 否 | 否 | 创建时间 |
| `updated_ts` | `timestamp` | 否 | 否 | 更新时间 |

### 当前代码中的典型值

| 字段 | 典型值 |
| --- | --- |
| `contract_type` | `EQUITY_AGREEMENT` / `DEFERRED_AGREEMENT` |
| `sign_status` | `PENDING` / `SIGNED` |

### 写入与使用说明

- 创建订单后，系统会自动为每笔订单补齐两类协议任务。
- 当前基线实现里，任务生成时即写为 `SIGNED`。
- `sign_url` 为示例签约地址，当前未对接真实电子签平台。

### 关系说明

- 通过 `benefit_order_no` 关联 `benefit_order`
- 通过 `task_no` 关联 `contract_archive`

---

## 4.7 `contract_archive`

- 表名: `contract_archive`
- 用途: 合同归档表，保存归档文件访问地址和摘要
- 主键: `contract_no`

### 字段定义

| 字段名 | 类型 | 允许空 | 主键 | 说明 |
| --- | --- | --- | --- | --- |
| `contract_no` | `varchar(64)` | 否 | 是 | 合同归档号 |
| `task_no` | `varchar(64)` | 否 | 否 | 签署任务号 |
| `contract_type` | `varchar(64)` | 否 | 否 | 合同类型 |
| `file_url` | `varchar(512)` | 否 | 否 | 合同文件地址 |
| `file_hash` | `varchar(128)` | 否 | 否 | 文件摘要 |
| `created_ts` | `timestamp` | 否 | 否 | 创建时间 |

### 写入与使用说明

- 创建订单后，由协议服务自动生成。
- 每条签约任务当前对应一条归档记录。
- `file_hash` 目前使用任务号计算的 sha256 作为示例摘要。

### 关系说明

- 通过 `task_no` 关联 `sign_task`

---

## 4.8 `notification_receive_log`

- 表名: `notification_receive_log`
- 用途: 外部通知接收与处理审计日志表
- 主键: `notify_no`

### 字段定义

| 字段名 | 类型 | 允许空 | 主键 | 说明 |
| --- | --- | --- | --- | --- |
| `notify_no` | `varchar(64)` | 否 | 是 | 通知日志号 |
| `benefit_order_no` | `varchar(64)` | 否 | 否 | 权益订单号 |
| `notify_type` | `varchar(64)` | 否 | 否 | 通知类型 |
| `request_id` | `varchar(64)` | 否 | 否 | 外部通知请求号 |
| `process_status` | `varchar(32)` | 否 | 否 | 处理状态 |
| `payload` | `text` | 是 | 否 | 原始通知报文 |
| `retry_count` | `int` | 否 | 否 | 重试次数 |
| `received_ts` | `timestamp` | 否 | 否 | 接收时间 |
| `processed_ts` | `timestamp` | 是 | 否 | 处理完成时间 |

### 当前代码中的典型值

| 字段 | 典型值 |
| --- | --- |
| `notify_type` | `GRANT_RESULT` / `REPAYMENT_STATUS` / `EXERCISE_RESULT` / `REFUND_RESULT` |
| `process_status` | `RECEIVED` / `PROCESSED` / `FAILED` |
| `retry_count` | `0` |

### 写入与使用说明

- 放款、还款、行权、退款回调到达时先插入一条 `RECEIVED` 记录。
- 后续根据处理结果更新为 `PROCESSED` 或 `FAILED`。
- 若订单不存在，接口仍可能返回成功响应，但通知日志会记为 `FAILED`。
- `payload` 保存原始请求体字符串，便于审计与排障。

### 关系说明

- 通过 `benefit_order_no` 关联 `benefit_order`

---

## 4.9 `idempotency_record`

- 表名: `idempotency_record`
- 用途: 幂等处理记录表
- 主键: `request_id`

### 字段定义

| 字段名 | 类型 | 允许空 | 主键 | 说明 |
| --- | --- | --- | --- | --- |
| `request_id` | `varchar(64)` | 否 | 是 | 请求唯一标识 |
| `biz_type` | `varchar(64)` | 否 | 否 | 业务类型 |
| `biz_key` | `varchar(128)` | 否 | 否 | 业务主键或关联键 |
| `response_body` | `text` | 是 | 否 | 当前代码中用于存储结果摘要或状态 |
| `processed_ts` | `timestamp` | 否 | 否 | 处理完成时间 |

### 当前代码中的典型值

| 字段 | 典型值 |
| --- | --- |
| `biz_type` | `REGISTER` / `CREATE_ORDER` / `FIRST_DEDUCT` / `FALLBACK_DEDUCT` / `GRANT` / `REPAYMENT` / `EXERCISE` / `REFUND` / `DOWNSTREAM_SYNC` |
| `biz_key` | `member_id` / `benefit_order_no` / `payment_no` |
| `response_body` | 会员号、订单状态、支付状态或下游同步 payload |

### 写入与使用说明

- 注册、建单、支付回调、通知回调、下游同步等流程处理完成后写入。
- 所有使用 `request_id` 做幂等的入口都会先查该表。
- 同一个 `request_id` 已存在时，后续请求不会重复执行业务。

## 5. 当前索引与约束

### 5.1 已在建表脚本中声明的主键/唯一键

| 表名 | 约束 |
| --- | --- |
| `member_info` | `PRIMARY KEY (member_id)` |
| `member_channel` | `PRIMARY KEY (id)` |
| `member_channel` | `UNIQUE KEY uk_channel_external_user(channel_code, external_user_id)` |
| `benefit_product` | `PRIMARY KEY (product_code)` |
| `benefit_order` | `PRIMARY KEY (benefit_order_no)` |
| `payment_record` | `PRIMARY KEY (payment_no)` |
| `sign_task` | `PRIMARY KEY (task_no)` |
| `contract_archive` | `PRIMARY KEY (contract_no)` |
| `notification_receive_log` | `PRIMARY KEY (notify_no)` |
| `idempotency_record` | `PRIMARY KEY (request_id)` |

### 5.2 当前未在建表脚本中声明但业务上存在的逻辑关联

| 逻辑关联 | 说明 |
| --- | --- |
| `member_channel.member_id -> member_info.member_id` | 会员绑定关系 |
| `benefit_order.member_id -> member_info.member_id` | 订单所属会员 |
| `benefit_order.product_code -> benefit_product.product_code` | 订单所属产品 |
| `payment_record.benefit_order_no -> benefit_order.benefit_order_no` | 支付流水归属订单 |
| `sign_task.benefit_order_no -> benefit_order.benefit_order_no` | 协议任务归属订单 |
| `contract_archive.task_no -> sign_task.task_no` | 合同归档归属签署任务 |
| `notification_receive_log.benefit_order_no -> benefit_order.benefit_order_no` | 通知日志归属订单 |

说明：

- 当前数据库没有物理外键约束。
- 数据完整性主要依赖应用服务层控制。

## 6. 数据流说明

### 6.1 注册链路

1. `member_info` 新增会员主档
2. `member_channel` 新增渠道绑定
3. `idempotency_record` 写入 `REGISTER`

### 6.2 创建订单链路

1. `benefit_order` 新增订单主记录
2. `sign_task` 为订单生成两条协议任务
3. `contract_archive` 为协议任务生成归档
4. `idempotency_record` 写入 `CREATE_ORDER`

### 6.3 支付链路

1. `payment_record` 写首扣或兜底代扣流水
2. `benefit_order` 更新支付相关状态
3. `idempotency_record` 写入对应支付业务类型

### 6.4 通知回调链路

1. `notification_receive_log` 先落 `RECEIVED`
2. `benefit_order` 按放款/行权/退款结果更新状态
3. `notification_receive_log` 更新为 `PROCESSED` 或 `FAILED`
4. 成功时 `idempotency_record` 写入对应业务类型

## 7. 当前实现说明

- 文档以当前代码真实表结构为准，不额外推导未落地字段。
- 当前 `schema.sql` 使用 `create table if not exists`，适合复用已有库表。
- 由于没有物理外键和额外索引，若后续进入生产高并发阶段，通常还需要补充：
  - 关键关联字段索引
  - 审计查询索引
  - 外键或等价的数据一致性治理方案
- 本文档仅描述当前实现，不代表最终生产版数据建模已经封板。
