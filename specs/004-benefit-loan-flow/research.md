# Research: 惠选卡权益与借款流程 V1 页面交互理解

## Decision 1: 先实现科技平台 outbound client，不先铺开页面 API

**Rationale**:

- 用户已经明确要求“先在 thirdpart 完成科技平台接口的调用实现”。
- 科技平台评审文档指出，很多关键接口方向此前被理解反了，因此先把 outbound 接口边界做对，比先写页面 controller 更稳妥。
- 当前 ABS 仓库已有 `thirdparty/qw` 作为参考模式，延续这一结构最自然。

**Alternatives considered**:

- 先写页面 controller 和 service，再反向补 thirdparty client：会导致上层 API 先依赖未稳定的外部契约。
- 直接把科技平台调用逻辑写进 `service/impl`：会把外部协议细节扩散到业务层，后续难维护。

## Decision 2: 当前第一批只覆盖 3 个通知类接口

**Decision**:

- `creditStatusNotice`
- `loanInfoNotice`
- `repayInfoNotice`

**Rationale**:

- 这三类接口都在标准文档第 5 章，属于典型的“机构主动通知平台”能力。
- 它们与 ABS 当前已有的订单、放款、还款通知链路最接近，落地后能直接为后续业务编排提供基础。
- 其余如 `applyInfoNotice`、`loanAmountByFund` 是否由 ABS 负责，当前还不明确，不适合一并实现。

**Alternatives considered**:

- 一次性实现第 5 章全部接口：范围过大，且部分接口当前业务归属未清楚。
- 只实现一个 `loanInfoNotice`：过窄，不能形成统一 client 设计。

## Decision 3: 签名算法与 AES 算法做成可配置

**Rationale**:

- 标准接口文档明确了请求头结构和 `param` 加密，但没有在当前材料里给出完全确定的签名实现细节。
- 若直接把某个算法写死，联调确认后需要再改代码。
- 可配置实现能先完成工程接入和测试，后续把最终值下沉到配置即可。

**Alternatives considered**:

- 固定 HMAC-SHA256 + AES/ECB/PKCS5Padding：实现简单，但联调风险集中在代码变更。
- 固定 MD5 + AES/GCM：同样缺少足够依据。

## Decision 4: 客户端响应同时兼容 plain `code/msg` 与加密 `param`

**Rationale**:

- 标准接口总则与具体 5.x 接口章节对响应体形式的表述并不完全一致。
- 为了避免文档口径差异导致 client 过于脆弱，解析层同时支持两种响应。

**Alternatives considered**:

- 只支持 plain `code/msg`：简单，但遇到统一加密响应时会直接失败。
- 只支持 encrypted `param`：会让本地测试和 mock 响应变复杂，且不利于快速验证。
