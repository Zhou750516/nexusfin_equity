# Data Model: NexusFin Equity Service Baseline

## Overview

The ABS-side `nexusfin_equity` database centers on two aggregates:

- `member_info` as the member aggregate root
- `benefit_order` as the order aggregate root

Supporting tables capture channel mapping, products, payment attempts, signing
artifacts, contract archives, and received downstream notifications.

## Entity: Member

**Purpose**: Represents the internal ABS-side identity for a diverted user.

**Core Fields**:

- `member_id`: internal unique member identifier
- `external_user_id`: upstream platform user identifier
- `mobile_encrypted`: encrypted mobile number
- `id_card_encrypted`: encrypted identity number
- `real_name_encrypted`: encrypted real name
- `member_status`: active or inactive state
- `created_ts`
- `updated_ts`

**Validation Rules**:

- `member_id` MUST be unique
- personal information MUST be stored encrypted at rest
- repeated onboarding for the same identity MUST resolve idempotently to the
  same `member_id`

**Relationships**:

- one Member to many Benefit Orders
- one Member to many Member Channel Links

## Entity: Member Channel Link

**Purpose**: Captures the relationship between an ABS member and an upstream
channel/platform identity.

**Physical Table**: `member_channel`

**Core Fields**:

- `member_id`
- `channel_code`
- `external_user_id`
- `bind_status`
- `created_ts`
- `updated_ts`

**Validation Rules**:

- the combination of channel and external user identity MUST be unique for the
  same logical binding
- channel mapping MUST allow idempotent repeat registration without creating a
  second member

**Relationships**:

- many Member Channel Links to one Member

## Entity: Benefit Product

**Purpose**: Defines the equity product the user can purchase.

**Core Fields**:

- `product_code`
- `product_name`
- `fee_rate`
- `status`

**Validation Rules**:

- `product_code` MUST be unique
- only active products can be purchased
- fee configuration MUST support deriving the payable equity fee for a given
  loan amount

**Relationships**:

- one Benefit Product to many Benefit Orders

## Entity: Benefit Order

**Purpose**: Represents the full lifecycle of an ABS-side equity order.

**Core Fields**:

- `benefit_order_no`
- `member_id`
- `channel_code`
- `external_user_id`
- `product_code`
- `agreement_no`
- `loan_amount`
- `qw_status`
- `grant_status`
- `loan_order_no`
- `created_ts`
- `updated_ts`

**Lifecycle States**:

- `SYNC_PENDING`
- `SYNC_SUCCESS`
- `SYNC_FAIL`
- `FIRST_DEDUCT_PENDING`
- `FIRST_DEDUCT_SUCCESS`
- `FIRST_DEDUCT_FAIL`
- `FALLBACK_DEDUCT_PENDING`
- `FALLBACK_DEDUCT_SUCCESS`
- `FALLBACK_DEDUCT_FAIL`
- `EXERCISE_PENDING`
- `EXERCISE_SUCCESS`
- `EXERCISE_FAIL`
- `REFUND_PENDING`
- `REFUND_SUCCESS`
- `REFUND_FAIL`
- `REFUND_DONE`

**Validation Rules**:

- `benefit_order_no` MUST be unique
- order transitions MUST follow legal status progression
- first deduct success and fallback deduct success MUST not coexist as duplicate
  payment outcomes for the same business event
- only fallback-eligible orders may trigger fallback deduct after grant success

**Relationships**:

- many Benefit Orders to one Member
- many Benefit Orders to one Benefit Product
- one Benefit Order to many Payment Records
- one Benefit Order to many Sign Tasks
- one Benefit Order to many Notification Receive Logs

## Entity: Payment Record

**Purpose**: Stores each first-deduct or fallback-deduct attempt.

**Core Fields**:

- `payment_no`
- `benefit_order_no`
- `payment_type` (`FIRST_DEDUCT` or `FALLBACK_DEDUCT`)
- `channel_name`
- `channel_trade_no`
- `amount`
- `payment_status`
- `fail_reason`

**Validation Rules**:

- `payment_no` MUST be unique
- `payment_type` MUST distinguish first and fallback attempts
- duplicate callback processing MUST not create duplicate successful payment
  outcomes for the same attempt

**Relationships**:

- many Payment Records to one Benefit Order

## Entity: Sign Task

**Purpose**: Stores signing tasks for equity and deferred-payment agreements.

**Core Fields**:

- `task_no`
- `benefit_order_no`
- `contract_type` (`EQUITY_AGREEMENT` or `DEFERRED_AGREEMENT`)
- `sign_url`
- `sign_status`

**Validation Rules**:

- `task_no` MUST be unique
- each contract type MUST be independently traceable per order
- expired tasks may be recreated, but current active signing state must remain
  unambiguous

**Relationships**:

- many Sign Tasks to one Benefit Order
- one Sign Task to zero or one Contract Archive

## Entity: Contract Archive

**Purpose**: Stores the finalized contract artifact after signing completes.

**Core Fields**:

- `contract_no`
- `task_no`
- `contract_type`
- `file_url`
- `file_hash`

**Validation Rules**:

- `contract_no` MUST be unique
- archived contract files MUST support integrity verification using file hash

**Relationships**:

- one Contract Archive to one Sign Task

## Entity: Notification Receive Log

**Purpose**: Records grant and repayment notifications received from YunKa and
their processing status.

**Core Fields**:

- `notify_no`
- `benefit_order_no`
- `notify_type` (`GRANT_RESULT` or `REPAYMENT_STATUS`)
- `request_id`
- `process_status`
- `retry_count`
- `received_ts`
- `processed_ts`

**Validation Rules**:

- `notify_no` MUST be unique
- callback retries MUST be safely deduplicated using stable request identity
- failed processing attempts MUST remain visible for replay or manual repair

**Relationships**:

- many Notification Receive Logs to one Benefit Order

## Cross-System Linkage

## ABS to YunKa

- `benefit_order_no` is the primary logical key joining ABS `benefit_order` and
  YunKa `order_transit`
- `member_id` and `external_user_id` support business reconciliation but do not
  replace `benefit_order_no` as the primary order-level key

## State Transition Rules

### Onboarding

- new user push -> create or reuse Member
- member resolved -> create or reuse Member Channel Link

### Order and Payment

- order created -> agreements pending
- agreements signed -> first deduct pending
- first deduct success -> path A continuation
- first deduct fail -> path B continuation eligible
- path B + grant success -> fallback deduct pending

### Post-Grant

- grant success may unlock fallback deduct and later exercise
- repayment notifications enrich servicing state but do not replace payment
  lifecycle data

### Refund

- refund may only be entered from eligible business states
- refund completion must remain linked to both order and payment history
