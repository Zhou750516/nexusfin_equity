# 齐为联调方案设计文档（通联证书直连版）

文档日期：2026-03-31

## 1. 背景与目标

本轮齐为联调不再以 `qweimobile` 聚合网关为主联调入口，而是改为使用通联测试环境直连：

- 测试地址：`https://tlt-test.allinpay.com/aipg/ProcessServlet`
- 目标：保留当前业务入口和业务编排不变，仅替换底层齐为 client 的协议实现方式

当前已在代码中落地的业务入口如下：

| 本地业务入口 | 当前依赖接口 | 业务作用 |
| --- | --- | --- |
| `BenefitOrderServiceImpl.createOrder()` | `QwBenefitClient.syncMemberOrder()` | 下单后同步会员与订单 |
| `BenefitOrderServiceImpl.getExerciseUrl()` | `QwBenefitClient.getExerciseUrl()` | 获取行权跳转链接 |
| `NotificationServiceImpl.handleGrant()` | `QwBenefitClient.notifyLending()` | 放款结果通知齐为 |

本设计的目标是：

1. 保持上层业务服务、控制器、数据库结构和调用顺序不变。
2. 在 `QwBenefitClient` 之下新增通联证书直连实现，替换当前仅面向 `qweimobile` 的 `HTTP JSON + MD5 + AES` 适配方式。
3. 将联调所需环境参数、证书材料、接口依赖、实施顺序和风险点明确成文，作为后续开发与联调的统一依据。

## 2. 现状判断

### 2.1 当前代码现状

当前代码中的 `QwBenefitClientImpl` 具备如下特点：

1. 协议面向 `https://t-api.test.qweimobile.com/api/abs/method`
2. 请求模型为：
   - `requestHead.partnerNo`
   - `requestHead.timestamp`
   - `requestHead.method`
   - `requestHead.version`
   - `requestHead.sign`
   - `requestBody`
3. 签名方式为 `MD5(method + partnerNo + timestamp + version + signKey)`
4. 报文体采用 `AES/GCM/NoPadding` 加解密
5. 当前 `QwProperties` 仅覆盖该套聚合网关协议所需参数

这意味着：

**当前实现可以支撑 mock 和 qweimobile 聚合网关联调，但不能直接支撑通联 `ProcessServlet` 的证书直连模式。**

### 2.2 本轮联调的设计边界

本轮设计不改变以下内容：

1. `BenefitOrderServiceImpl` 的业务职责与调用顺序
2. `NotificationServiceImpl.handleGrant()` 的放款通知触发时机
3. `QwBenefitClient` 作为上层统一业务接口的抽象边界
4. 现有订单、通知、幂等、审计、敏感信息处理主链

本轮设计需要新增或调整的内容：

1. 新增通联直连协议实现
2. 新增通联证书装载与回包验签能力
3. 新增通联联调配置项
4. 新增协议路由能力，使 `QwBenefitClient` 支持按配置切换不同下游协议实现

## 3. 方案选型与决策

本轮已确认采用的方案为：

**方案 2：保留 `QwBenefitClient` 业务接口不变，新增通联直连实现，并通过配置切换底层协议。**

### 3.1 采用该方案的原因

1. 上层业务入口不变，影响面可控。
2. 现有 mock 能力和新联调能力可以共存，便于回归和排障。
3. `qweimobile` 聚合协议与通联直连协议差异很大，不适合强行塞进同一个实现类里。
4. 若后续仍需保留聚合网关能力，可通过配置快速回切，而不是回滚代码。

### 3.2 不采用直接覆盖实现的原因

如果直接改写现有 `QwBenefitClientImpl`：

1. `MOCK`、聚合网关、通联直连三种能力会耦合在同一个类里。
2. 联调失败时很难快速判断是业务问题、协议问题还是配置问题。
3. 后续保留双协议能力时会出现大量条件分支，降低可维护性。

## 4. 目标架构

### 4.1 架构分层

建议将齐为适配层拆成以下结构：

| 层级 | 组件 | 职责 |
| --- | --- | --- |
| 业务层 | `BenefitOrderServiceImpl`、`NotificationServiceImpl` | 维持现有业务编排，不感知底层协议细节 |
| 统一业务接口层 | `QwBenefitClient` | 定义会员同步、联登链接、放款通知三个业务动作 |
| 协议路由层 | `RoutingQwBenefitClient` | 根据配置将请求路由到 `MOCK`、`QWEIMOBILE`、`ALLINPAY_DIRECT` 实现 |
| 聚合协议实现层 | 现有 `QwBenefitClientImpl` 可收敛为 `QweimobileQwBenefitClient` | 负责原有 `qweimobile` 网关协议 |
| 通联直连实现层 | 新增 `AllinpayDirectQwBenefitClient` | 负责通联证书加载、请求组包、签名、验签、报文解析 |
| 协议基础设施层 | 证书装载器、签名器、XML/报文编解码器、HTTP 客户端工厂 | 屏蔽通联直连细节 |

### 4.2 推荐配置模型

当前 `QwProperties.Mode` 只有：

- `MOCK`
- `HTTP`

建议扩展为：

- `MOCK`
- `QWEIMOBILE_HTTP`
- `ALLINPAY_DIRECT`

原因是：

1. `HTTP` 这个命名过于宽泛，无法区分聚合网关和证书直连。
2. 本轮需要稳定支持“按协议切换实现”，模式名必须表达协议差异，而不是只表达传输方式。

## 5. 业务流程设计

### 5.1 下单后会员同步流程

流程目标：在本地下单成功后，将会员、产品、支付协议等信息同步到齐为/通联下游，并获取下游订单信息。

建议流程如下：

1. `POST /api/equity/orders` 进入 `BenefitOrderServiceImpl.createOrder()`
2. 本地完成：
   - 幂等校验
   - 产品校验
   - 会员与渠道关联校验
   - 订单落库
   - 协议资料归档
3. `BenefitOrderServiceImpl` 构造 `QwMemberSyncRequest`
4. `RoutingQwBenefitClient` 根据 `QW_MODE=ALLINPAY_DIRECT` 路由到 `AllinpayDirectQwBenefitClient`
5. 通联直连实现完成：
   - 业务请求映射
   - 通联报文组装
   - 商户私钥签名
   - HTTPS 请求发送
   - 通联响应验签
   - 响应报文解析
6. 将下游返回的关键字段回填到本地订单上下文
7. 记录 `traceId + bizOrderNo + upstreamOrderNo`

成功标准：

1. 本地订单创建成功
2. 通联响应通过验签与解析
3. 本地可拿到对方订单标识或等价上下文字段

### 5.2 获取联登链接流程

流程目标：基于已有订单上下文，获取用户跳转所需的真实联登地址或等价 token。

建议流程如下：

1. `GET /api/equity/exercise-url/{benefitOrderNo}` 进入 `BenefitOrderServiceImpl.getExerciseUrl()`
2. 本地根据订单号读取订单与外部用户上下文
3. 构造 `QwExerciseUrlRequest`
4. 通过 `RoutingQwBenefitClient` 路由到通联直连实现
5. 通联直连实现完成：
   - 联登请求报文映射
   - 签名与发送
   - 响应验签
   - 提取 `redirectUrl` 或等价字段
6. 返回 `ExerciseUrlResponse`

成功标准：

1. 能拿到可消费的跳转地址
2. 地址关联到当前订单或当前用户
3. 过期时间、token 或其他联登关键字段有清晰映射

### 5.3 放款通知流程

流程目标：在内部放款结果处理成功后，将放款结果通知齐为/通联下游。

建议流程如下：

1. 放款结果回调进入 `NotificationServiceImpl.handleGrant()`
2. 本地完成：
   - 幂等校验
   - 通知日志落库
   - 订单状态推进
   - 必要时触发兜底代扣
3. 在本地状态落稳后，构造 `QwLendingNotifyRequest`
4. 通过 `RoutingQwBenefitClient` 路由到通联直连实现
5. 通联直连实现发送放款结果通知并处理响应
6. 本地记录外部通知结果

成功标准：

1. 本地状态先落库，再出站通知
2. 出站通知成功/失败都可审计
3. 失败时可通过日志快速回放或重试

## 6. 接口依赖设计

### 6.1 内部接口依赖

| 依赖方 | 被依赖方 | 说明 |
| --- | --- | --- |
| `BenefitOrderServiceImpl` | `QwBenefitClient.syncMemberOrder()` | 下单后同步会员与订单 |
| `BenefitOrderServiceImpl` | `QwBenefitClient.getExerciseUrl()` | 获取联登链接 |
| `NotificationServiceImpl` | `QwBenefitClient.notifyLending()` | 放款结果通知 |

### 6.2 外部协议依赖

本轮通联证书直连联调，至少依赖以下外部信息：

| 类别 | 已确认信息 | 说明 |
| --- | --- | --- |
| 测试地址 | `https://tlt-test.allinpay.com/aipg/ProcessServlet` | 通联直连测试环境地址 |
| 商户号 | `200000000007804` | 来自 `证书信息.txt` |
| 用户名 | `20000000000780404` | 来自 `证书信息.txt` |
| 用户密码 | `111111` | 来自 `证书信息.txt` |
| P12 密码 | `111111` | 来自 `证书信息.txt` |
| 支付账户 | `200000000007804` | 来自 `艾博生-齐为测试环境.txt` |
| 渠道编码 | `allinpay-1` | 来自 `艾博生-齐为测试环境.txt` |

### 6.3 仍需补齐的直连协议资料

本轮设计必须明确一个现实边界：

**当前仓库中没有通联 `ProcessServlet` 对应的完整接口规范、serviceCode 定义或 XML 报文模板。**

因此在实施前，还需要齐为/通联提供以下协议资料：

| 依赖项 | 原因 |
| --- | --- |
| 三个业务动作分别对应的通联接口代码或 serviceCode | 需要把 `syncMemberOrder`、`getExerciseUrl`、`notifyLending` 映射到真实直连接口 |
| 请求报文结构定义 | 决定 XML 或表单报文如何组装 |
| 响应报文结构定义 | 决定如何解析成功码、业务码、跳转链接等字段 |
| 签名字段规则 | 决定签名串拼接顺序和验签口径 |
| 是否需要双向 TLS | 决定 `user-rsa.p12` 是用于报文签名、客户端证书认证，还是两者同时使用 |
| 响应验签口径 | 决定使用 `public-rsa.cer` 还是其他平台公钥证书 |

没有这些协议资料，无法直接进入编码阶段，但不影响先完成架构设计和环境准备设计。

## 7. 环境配置设计

### 7.1 新增配置项建议

建议在 `nexusfin.third-party.qw` 下增加直连配置分组：

| 配置项 | 示例值 | 用途 |
| --- | --- | --- |
| `mode` | `ALLINPAY_DIRECT` | 切换底层协议实现 |
| `direct.base-url` | `https://tlt-test.allinpay.com` | 通联基础地址 |
| `direct.process-path` | `/aipg/ProcessServlet` | 通联处理路径 |
| `direct.merchant-id` | `200000000007804` | 商户号 |
| `direct.user-name` | `20000000000780404` | 联调用户名 |
| `direct.user-password` | `111111` | 联调密码 |
| `direct.channel-code` | `allinpay-1` | 渠道编码 |
| `direct.account-no` | `200000000007804` | 支付账户或业务账号 |
| `direct.pkcs12-path` | 证书文件路径 | 商户私钥证书位置 |
| `direct.pkcs12-password` | `111111` | P12 密码 |
| `direct.verify-cert-path` | 证书文件路径 | 响应验签证书位置 |
| `direct.connect-timeout-ms` | `3000` | 连接超时 |
| `direct.read-timeout-ms` | `5000` | 读超时 |

### 7.2 证书文件管理建议

联调期建议：

1. 保留仓库中的第三方材料目录作为原始材料存档目录，不直接在代码里硬编码相对路径。
2. 应通过环境变量注入证书位置，例如：
   - `QW_DIRECT_PKCS12_PATH`
   - `QW_DIRECT_VERIFY_CERT_PATH`
3. 密码应通过环境变量或密钥管理注入，不应写入默认配置文件。

### 7.3 当前证书材料分析

#### `user-rsa.p12`

解析结果：

1. 是带密码保护的 `PKCS#12` 证书包
2. 密码与文档一致，为 `111111`
3. 使用了旧算法，需要 `openssl pkcs12 -legacy` 才能正确读取
4. 包内证书主题为：
   - `CN=allinpay`
   - 有效期：`2024-05-06` 至 `2034-05-04`

工程判断：

该文件可以作为通联直连模式下的候选商户私钥载体。

#### `public-rsa.cer`

解析结果：

1. 是 `PEM` 证书，不是 `DER`
2. 可正常解析
3. 有效期：`2024-05-06` 至 `2034-05-04`
4. 与 `user-rsa.p12` 证书主题一致

工程判断：

该文件更接近当前有效的通联平台公钥或商户公钥配套材料，适合作为本轮联调的首选验签候选证书。

#### `allinpay-pds.cer`

解析结果：

1. 是 `DER` 证书
2. 可正常解析
3. 证书有效期：`2012-07-24` 至 `2014-07-24`

工程判断：

该文件明显是历史证书材料，不应默认作为本轮联调的有效验签证书。应在联调前向齐为/通联确认其是否仍有用途；若无，则不纳入当前有效配置。

## 8. 组件设计

### 8.1 推荐新增组件

| 组件 | 职责 |
| --- | --- |
| `RoutingQwBenefitClient` | 读取 `QwProperties.mode` 并分发到底层实现 |
| `QweimobileQwBenefitClient` | 承接当前 `qweimobile` 聚合协议实现 |
| `AllinpayDirectQwBenefitClient` | 通联证书直连实现 |
| `AllinpayDirectProperties` | 承载通联专属配置 |
| `AllinpayCertificateLoader` | 装载 `PKCS12` 和验签证书 |
| `AllinpayRequestSigner` | 商户请求签名 |
| `AllinpayResponseVerifier` | 响应验签 |
| `AllinpayPayloadCodec` | 报文编解码 |
| `AllinpayHttpClientFactory` | 构建带证书和超时配置的 HTTP 客户端 |

### 8.2 关键设计约束

1. 上层服务不依赖任何通联特有字段。
2. 通联特有的 serviceCode、证书、签名细节全部下沉到直连实现层。
3. 错误分类要区分：
   - 配置错误
   - 证书加载错误
   - 连接错误
   - 验签错误
   - 报文解析错误
   - 业务失败码
4. 日志必须带：
   - `traceId`
   - `bizOrderNo`
   - 下游接口名
   - 下游返回码或错误类型

## 9. 联调顺序设计

联调建议沿用现有业务顺序，但底层替换为通联直连：

| 顺序 | 业务动作 | 本地入口 | 目的 |
| --- | --- | --- | --- |
| 1 | 会员同步 / 建单 | `BenefitOrderServiceImpl.createOrder()` | 确认直连建单主链打通 |
| 2 | 获取联登链接 | `BenefitOrderServiceImpl.getExerciseUrl()` | 确认联登跳转链路打通 |
| 3 | 放款通知 | `NotificationServiceImpl.handleGrant()` | 确认放款后出站通知链路打通 |

原因：

1. 建单是后续链路的前置条件。
2. 联登依赖前置订单和用户上下文。
3. 放款通知应在订单与联登链路稳定后再验证。

## 10. 风险与确认项

### 10.1 当前主要风险

| 风险 | 影响 |
| --- | --- |
| 通联直连协议文档缺失 | 无法直接编码具体报文 |
| `allinpay-pds.cer` 为过期历史证书 | 若误用，会导致验签失败或联调误判 |
| `user-rsa.p12` 使用旧加密算法 | 需要在本地和运行环境确认 `PKCS12` 兼容性 |
| 当前 `QwProperties` 仅适配聚合协议 | 若不拆分配置，容易出现配置污染和实现耦合 |

### 10.2 联调前必须确认的事项

| 确认项 | 当前结论 |
| --- | --- |
| 通联直连三类业务动作分别对应哪个接口定义 | 必须向齐为/通联确认 |
| 直连接口的请求 / 响应报文规范 | 必须向齐为/通联确认 |
| `public-rsa.cer` 是否为当前有效回包验签证书 | 必须向齐为/通联确认 |
| `user-rsa.p12` 是否同时承担客户端证书认证与报文签名 | 必须向齐为/通联确认 |
| 是否需要双向 TLS | 必须向齐为/通联确认 |

## 11. 实施建议

建议实施顺序如下：

1. 先完成配置模型和路由层改造，不改上层业务入口。
2. 再新增通联证书装载、签名、验签和 HTTP 客户端基础设施。
3. 待齐为/通联提供直连协议文档后，实现三类业务动作的具体报文映射。
4. 先完成单接口冒烟，再串业务主链联调。
5. 保留 `QWEIMOBILE_HTTP` 作为回退模式，避免联调失败时只能回滚代码。

## 12. 设计结论

本轮齐为联调的正确落地方式不是继续沿用当前 `qweimobile` 聚合网关实现，而是：

**在不改变现有业务入口和业务编排的前提下，保留 `QwBenefitClient` 抽象，新增通联证书直连实现与配置路由能力，使系统可以在 `MOCK`、聚合协议、通联直连协议之间按模式切换。**

这样做的结果是：

1. 能满足本轮以通联测试地址为准的真实联调要求。
2. 不破坏现有业务主链和测试主链。
3. 后续即使继续保留聚合网关模式，也不会和通联直连实现混在一起。
