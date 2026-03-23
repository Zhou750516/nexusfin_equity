# Feature Specification: NexusFin Equity Service Baseline

**Feature Branch**: `001-equity-service-baseline`  
**Created**: 2026-03-23  
**Status**: Draft  
**Input**: User description: "根据《惠聚项目-技术概要设计方案》为艾博生
nexusfin-equity 创建权益分发服务基线规格，覆盖静默注册、权益订单、电子签章、首次代扣、兜底代扣、退款、云卡通知消费，以及安全、幂等、审计和可用性要求"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - 拒件用户承接与权益下单 (Priority: P1)

作为被科技平台导流到艾博生的用户，我希望在不重复填写基础信息的前提下完成静默注册、查看可购
买权益并创建权益订单，这样我可以顺畅进入后续签约和借款流程，而不会因为跨系统切换而中断。

**Why this priority**: 这是整条业务闭环的入口。如果无法稳定承接拒件用户并创建有效权益订单，后续
签约、支付、试算、放款都无法发生。

**Independent Test**: 向服务推送一名符合导流条件的用户后，系统可以幂等完成会员落库、渠道关系建
立、权益产品展示和权益订单创建，并返回可追踪的订单标识。

**Acceptance Scenarios**:

1. **Given** 科技平台已将拒件用户转发到艾博生，**When** 艾博生接收该用户，**Then** 系统应完成
   静默注册或幂等复用已有会员，并建立正确的渠道归属关系。
2. **Given** 用户已进入权益购买页面，**When** 用户选择有效权益产品并提交购买，**Then** 系统应
   创建一笔可追踪的权益订单并关联会员、渠道和产品信息。

---

### User Story 2 - 权益签约与首次代扣 (Priority: P2)

作为准备购买权益的用户，我希望在权益订单创建后能完成所需协议签署，并立即得到首次代扣结果，
这样我可以明确知道自己是进入直接撮合路径还是先享后付路径。

**Why this priority**: 权益签约和首次代扣决定订单是否具备继续向云卡流转的前置条件，也是路径 A
和路径 B 的分流节点。

**Independent Test**: 对一笔已创建的权益订单执行协议签署和首次代扣后，系统能够正确记录签约结
果、支付结果和订单状态，并区分首次代扣成功与失败两条路径。

**Acceptance Scenarios**:

1. **Given** 用户已创建权益订单，**When** 用户完成权益服务协议和先享后付协议签署，**Then** 系
   统应保存签约任务结果并允许订单进入首次代扣流程。
2. **Given** 已触发首次代扣，**When** 支付渠道返回成功结果，**Then** 系统应把订单标记为已首扣
   成功并准备进入后续借款撮合流程。
3. **Given** 已触发首次代扣，**When** 支付渠道返回失败结果，**Then** 系统应把订单标记为待兜底
   路径，并保留继续进入借款撮合流程所需的信息。

---

### User Story 3 - 放款结果消费、兜底代扣与状态闭环 (Priority: P3)

作为艾博生运营和结算方，我希望在订单进入云卡和科技平台后，仍能持续接收放款与还款结果，并在
首次代扣失败时自动发起兜底代扣，这样每一笔权益订单都能形成可结算、可对账、可追溯的闭环。

**Why this priority**: 这是权益服务真正完成商业闭环的关键。如果艾博生无法消费放款和还款结果，或
无法在放款成功后处理兜底代扣，业务会停留在半完成状态。

**Independent Test**: 对一笔已流转到云卡的订单，模拟放款结果通知、兜底代扣结果和还款状态通知，
系统应能正确更新订单与支付状态，并保留审计记录。

**Acceptance Scenarios**:

1. **Given** 首次代扣失败且借款已放款成功，**When** 艾博生收到放款成功通知，**Then** 系统应发
   起一次兜底代扣并记录处理结果。
2. **Given** 科技平台后续发生还款状态变化，**When** 艾博生收到云卡转发的还款通知，**Then**
   系统应将该状态关联到正确的权益订单并可用于后续对账和客服查询。
3. **Given** 行权或退款结果由合作方返回，**When** 艾博生接收结果通知，**Then** 系统应更新相
   关状态并保留完整的通知处理记录。

### Edge Cases

- 同一拒件用户被重复推送时，系统必须避免生成重复会员和重复渠道绑定关系。
- 用户已完成签约但首次代扣回调重复、延迟或乱序到达时，系统必须维持一致的订单状态。
- 首次代扣失败后，如果放款结果迟迟未到或多次通知，系统必须保证兜底代扣不会被重复触发。
- 放款成功后，如果兜底代扣再次失败，系统必须保留后续人工处理和对账所需的失败原因。
- 外部通知签名校验失败、时间戳异常或来源不在白名单时，系统必须拒绝处理并保留审计记录。
- 还款或退款通知到达时，如果关联订单不存在或状态不匹配，系统必须记录异常并支持后续追查。

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST accept diverted rejected-user information from the
  upstream transit flow and complete silent registration through an idempotent
  member onboarding process.
- **FR-002**: System MUST maintain both member master information and channel
  association information so that repeated user pushes can be reconciled to the
  same internal member identity.
- **FR-003**: System MUST allow eligible users to view active benefit products
  and create a benefit order linked to the correct member, channel, and product.
- **FR-004**: System MUST require completion of the benefit-service agreement
  and the first-enjoy-then-pay agreement before an order can enter payment
  processing.
- **FR-005**: System MUST create and track sign tasks and contract artifacts so
  that signed agreements can be proven after the fact.
- **FR-006**: System MUST initiate first deduct processing for eligible orders
  and persist the full result, including success, failure, external trade
  reference, and failure reason when available.
- **FR-007**: System MUST distinguish between direct continuation orders and
  fallback-eligible orders based on the first deduct result and preserve the
  downstream routing state needed for both paths.
- **FR-008**: System MUST push paid or fallback-pending benefit-order
  information downstream to YunKa so the user can continue trial, signing, and
  grant processing.
- **FR-009**: System MUST consume grant-result notifications and, when an order
  previously failed first deduct, trigger exactly one fallback deduct attempt
  per eligible grant success event.
- **FR-010**: System MUST consume repayment-status notifications and associate
  them with the correct benefit order for reconciliation, servicing, and
  dispute handling.
- **FR-011**: System MUST consume exercise-result and refund-result
  notifications and update the corresponding order or payment state without
  losing historical traceability.
- **FR-012**: System MUST expose and consume interfaces through the unified
  business response contract and MUST reject unauthorized or malformed external
  requests.
- **FR-013**: System MUST support request-level idempotency for all state
  changing inbound and outbound interactions so retries do not create duplicate
  orders, duplicate payments, or duplicate notifications.
- **FR-014**: System MUST preserve an auditable processing trail for member
  registration, order creation, signing, payment, callback handling, and
  notification forwarding using stable business identifiers.
- **FR-015**: System MUST protect sensitive user data at rest and ensure that
  only the minimum required party data is stored in the ABS service.
- **FR-016**: System MUST support operational reconciliation by keeping order,
  payment, contract, and callback records queryable by core identifiers such as
  benefit order number, member ID, payment number, and request ID.

### Key Entities *(include if feature involves data)*

- **Member**: Represents the internal ABS-side user identity created through
  silent registration, including encrypted personal information and current
  member status.
- **Member Channel Link**: Represents the mapping between an internal member and
  an upstream platform/channel identity so duplicate pushes can be reconciled
  idempotently.
- **Benefit Product**: Represents the equity product being sold, including
  product availability and configured fee rule.
- **Benefit Order**: Represents the full lifecycle of a purchased equity order,
  including signing, first deduct, fallback deduct, exercise, refund, and
  downstream loan-related state.
- **Sign Task**: Represents the agreements the user must sign and the resulting
  sign status and contract proof.
- **Payment Record**: Represents each first deduct or fallback deduct attempt,
  including external trade reference, amount, status, and failure reason.
- **Notification Log**: Represents inbound callback and downstream state
  processing records used for traceability, retry control, and reconciliation.

## Assumptions

- This baseline specification treats the ABS-side service as the authoritative
  owner of member registration, benefit order state, agreement state, and ABS
  payment state.
- The YunKa service remains the only transit hub between ABS and the KJ core
  system.
- The business baseline includes both the first-deduct-success path and the
  first-deduct-fail-then-fallback path described in the technical overview.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of valid diverted-user pushes result in exactly one
  internally traceable member identity and one correct channel association for
  the same upstream identity.
- **SC-002**: 99% of core transaction interactions for order creation, payment
  result handling, and grant-result consumption complete within 500ms under the
  expected production load profile.
- **SC-003**: 100% of successful grant notifications for fallback-eligible
  orders result in at most one fallback deduct attempt for the corresponding
  business event.
- **SC-004**: Operations staff can reconstruct the end-to-end status of any
  benefit order, including signing and payment outcomes, using stored business
  identifiers without depending on manual log digging.
- **SC-005**: Daily reconciliation can compare ABS order and payment records
  against downstream notifications without missing the originating benefit order
  or external request identifier.
- **SC-006**: Sensitive member fields remain protected in storage while still
  allowing the business to locate the correct member and order records for
  servicing and audit scenarios.
