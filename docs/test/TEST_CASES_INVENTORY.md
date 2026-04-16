# NexusFin Equity Service - 测试用例全量清单

**文档日期**: 2026-03-31
**范围**: 所有 25 个测试类，包括 60+ 个测试方法
**覆盖**: 单元测试、集成测试、端到端测试

---

## 目录

1. [测试概览](#测试概览)
2. [认证与授权测试](#认证与授权测试)
3. [订单管理测试](#订单管理测试)
4. [支付处理测试](#支付处理测试)
5. [通知处理测试](#通知处理测试)
6. [工具类测试](#工具类测试)
7. [集成测试](#集成测试)
8. [支撑服务测试](#支撑服务测试)
9. [第三方集成测试](#第三方集成测试)
10. [测试数据与 Fixtures](#测试数据与-fixtures)
11. [Mock 策略](#mock-策略)

---

## 测试概览

### 测试金字塔

```
                         端到端 (4)
                    ↗ MySQL 集成
                   ↙  Round-trip
              集成测试 (10)
            ↗  Controller
           ↙    Service
    单元测试 (50+)
        Mock-based
```

### 测试统计

| 类别 | 数量 | 位置 |
|------|------|------|
| **单元测试** | 35+ | `service/`, `util/`, `config/` |
| **集成测试** | 10 | `controller/` |
| **端到端测试** | 2 | `controller/` (MySQL) |
| **测试文件总数** | 25 | `src/test/java/` |
| **测试方法总数** | 60+ | 遍布所有文件 |

### 技术栈

- **测试框架**: JUnit 5 (Jupiter)
- **Mock 框架**: Mockito (MockitoExtension)
- **集成测试**: Spring Boot Test + @SpringBootTest
- **HTTP Mock**: MockRestServiceServer
- **数据库**: H2 (默认) 和 MySQL (条件化)

---

## 认证与授权测试

### 1. JwtAuthenticationFilterTest
**文件**: `config/JwtAuthenticationFilterTest.java`
**职责**: JWT 认证过滤器的单元测试

#### 测试方法

| 方法名 | 场景 | 预期结果 |
|--------|------|---------|
| `shouldRejectProtectedRequestWithoutCookie()` | 不带认证 cookie 的受保护请求 | 返回 401 Unauthorized |
| `shouldSkipExcludedRequestPath()` | 排除列表中的请求路径 | 绕过认证过滤器 |

#### Mock 依赖
- `AuthProperties` - 包含受保护路径列表和排除路径列表

#### 测试用例
```java
@Test
void shouldRejectProtectedRequestWithoutCookie() {
    // 模拟 GET /api/equity/orders (受保护)
    // 无 NEXUSFIN_AUTH cookie
    // 预期: MockHttpServletResponse.getStatus() == 401
}

@Test
void shouldSkipExcludedRequestPath() {
    // 模拟 POST /api/callbacks/first-deduction (排除)
    // 预期: 过滤器链继续
}
```

---

### 2. AuthControllerIntegrationTest
**文件**: `controller/AuthControllerIntegrationTest.java`
**职责**: 认证端点集成测试 (SSO 回调、当前用户)

#### 测试方法

| 方法名 | 场景 | 预期结果 |
|--------|------|---------|
| `shouldCreateCookieAndRedirectForSsoCallback()` | SSO 回调，有效 token | 创建认证 cookie，302 重定向 |
| `shouldReturnCurrentUserWhenJwtCookieIsPresent()` | GET /api/users/me，有效 JWT | 返回 200 + 用户信息 |
| `shouldRejectCurrentUserWhenJwtCookieMissing()` | GET /api/users/me，无 cookie | 返回 401 Unauthorized |
| `shouldRejectInvalidRedirectUrl()` | SSO 回调，外部重定向 URL | 返回 400 Bad Request |

#### 测试数据
- 使用 `@Sql(scripts = "classpath:db/test-data.sql")` 预加载数据
- Mock 的 `TechPlatformUserClient.verifyToken()` 返回用户资料

#### 断言示例
```java
@Test
void shouldCreateCookieAndRedirectForSsoCallback() {
    // 1. 调用 GET /api/auth/sso-callback?token=mock-token&redirect_url=/equity
    // 2. 断言: response.getCookie("NEXUSFIN_AUTH") 不为空
    // 3. 断言: response.getStatus() == 302
    // 4. 断言: response.getRedirectUrl() == "/equity"
}
```

---

### 3. AuthServiceTest
**文件**: `service/AuthServiceTest.java`
**职责**: 认证服务逻辑单元测试 (成员预配、JWT 生成)

#### 测试方法

| 方法名 | 场景 | 预期结果 |
|--------|------|---------|
| `shouldProvisionMemberAndIssueJwtForSsoLogin()` | 新用户 SSO 登录 | 创建成员 + 加密敏感数据 + 生成 JWT |
| `shouldReuseExistingMemberForRepeatSsoLogin()` | 已存在用户 SSO 登录 | 复用成员，不重复创建 |
| `shouldReturnCurrentUserFromAuthContext()` | 检索 AuthContext | 返回认证用户对象 |
| `shouldRejectInvalidPhoneForNewSsoMember()` | 无效手机号格式 | 抛出校验异常 |

#### 加密测试
```java
@Test
void shouldEncryptSensitiveDataDuringProvisioning() {
    // 新建成员时加密敏感字段
    // mobile: AES/GCM encrypted
    // idCard: AES/GCM encrypted
    // realName: AES/GCM encrypted
    // 断言: 加密字段不为明文
}
```

---

## 订单管理测试

### 4. BenefitOrderControllerIntegrationTest
**文件**: `controller/BenefitOrderControllerIntegrationTest.java`
**职责**: 权益订单 HTTP 端点集成测试

#### 测试方法

| 方法名 | 端点 | 场景 | 预期结果 |
|--------|------|------|---------|
| `shouldLoadProductPageFromDatabase()` | GET `/api/equity/products/{productCode}` | 查询产品 | 返回产品信息 + 成员上下文 |
| `shouldCreateBenefitOrderAndPersistAgreementArtifacts()` | POST `/api/equity/orders` | 创建订单 | 订单 + 2 个签约任务 + 2 个合同存档 |
| `shouldReplayDuplicateCreateOrderRequest()` | POST `/api/equity/orders` (幂等) | 重复请求 | 返回相同订单 (无重复) |
| `shouldReadOrderStatusFromDatabase()` | GET `/api/equity/orders/{benefitOrderNo}` | 查询状态 | 返回所有状态字段 |
| `shouldGetExerciseUrlFromQwMockClient()` | GET `/api/equity/exercise-url/{benefitOrderNo}` | 获取行权 URL | 返回 URL + 过期时间 |
| `shouldLogAuthenticatedAccessForProductOrderStatusAndExerciseUrl()` | 所有端点 | 审计日志 | 记录 traceId + businessOrderNo |

#### 测试数据准备
```java
@BeforeEach
void setUp() {
    // 清理所有相关数据
    - 合同存档
    - 签约任务
    - 权益订单
    - 幂等性记录
    - 成员渠道关系
    - 成员信息
    - 产品

    // 创建测试数据
    - 产品 (productCode="PROD_001", status="ACTIVE")
    - 成员 (encryptedMobile, encryptedIdCard, encryptedRealName)
    - 渠道关系 (channelCode, externalUserId)
}
```

#### 签约任务验证
```java
@Test
void shouldCreateSignTasksForBothAgreements() {
    // 创建订单后应有 2 个 SignTask
    // 1. contractType = "EQUITY_AGREEMENT"
    // 2. contractType = "DEFERRED_AGREEMENT"

    // 每个 SignTask 应有
    // - taskNo (唯一)
    // - signUrl (可访问的签约链接)
    // - signStatus = "PENDING"
}
```

---

### 5. BenefitOrderServiceTest
**文件**: `service/BenefitOrderServiceTest.java`
**职责**: 订单服务逻辑单元测试 (Mock 所有依赖)

#### 测试方法

| 方法名 | 场景 | 预期结果 |
|--------|------|---------|
| `shouldReturnProductPageWithMemberInfo()` | 查询产品页 | 返回产品 + 成员信息 DTO |
| `shouldCreateOrderAndEnsureAgreementArtifacts()` | 创建订单 | 持久化订单 + 触发协议生成 + 标记幂等 |
| `shouldReplayExistingOrderForDuplicateCreateRequest()` | 重复创建请求 | 幂等记录缓存命中，返回已存订单 |
| `shouldRejectOrderCreationWhenProductMissing()` | 产品不存在 | 抛出 BizException("PRODUCT_NOT_FOUND") |
| `shouldGetExerciseUrlFromQwClient()` | 获取行权 URL | 调用 QwBenefitClient.getExerciseUrl() |

#### Mock 配置
```java
@Mock
private BenefitProductRepository productRepo;

@Mock
private BenefitOrderRepository orderRepo;

@Mock
private AgreementService agreementService;

@Mock
private IdempotencyService idempotencyService;

@Mock
private QwBenefitClient qwClient;

@InjectMocks
private BenefitOrderServiceImpl service;
```

---

### 6. BenefitOrderServiceAuthTest
**文件**: `service/BenefitOrderServiceAuthTest.java`
**职责**: 订单创建的身份认证相关错误测试

#### 测试方法

| 方法名 | 场景 | 预期结果 |
|--------|------|---------|
| `shouldRejectAuthenticatedOrderFlowWhenMemberMissing()` | 成员不存在 | BizException("MEMBER_NOT_FOUND") |

---

### 7. OrderStateMachineTest
**文件**: `util/OrderStateMachineTest.java`
**职责**: 订单状态转换机验证

#### 订单生命周期流程图
```
初始状态
  ↓
FIRST_DEDUCT_PENDING
  ├─ 成功 → FIRST_DEDUCT_SUCCESS → 后续借款
  └─ 失败 → FIRST_DEDUCT_FAIL
          ↓
      [等待放款成功]
          ↓
      兜底代扣触发 → FALLBACK_DEDUCT_PENDING
          ├─ 成功 → FALLBACK_DEDUCT_SUCCESS
          └─ 失败 → FALLBACK_DEDUCT_FAIL

[独立路径]
GRANT_SUCCESS → 行权成功
REFUND_SUCCESS → 退款成功
```

#### 测试方法

| 方法名 | 验证 | 预期结果 |
|--------|------|---------|
| `shouldRejectOrderCreationWhenAgreementNotReady()` | 未签协议 | BizException |
| `shouldApplyFirstDeductSuccessState()` | 首次代扣成功 | orderStatus=FIRST_DEDUCT_SUCCESS, syncStatus=SYNC_SUCCESS |
| `shouldRejectFallbackWhenOrderNotInFirstDeductFail()` | 无效兜底触发 | BizException("FALLBACK_NOT_ELIGIBLE") |
| `shouldApplyGrantExerciseAndRefundStates()` | 完整路径 | 状态链式转换正确 |

#### 状态转换验证代码
```java
@Test
void shouldApplyFirstDeductSuccessState() {
    BenefitOrder order = new BenefitOrder();
    order.setOrderStatus(BenefitOrderStatusEnum.FIRST_DEDUCT_PENDING.name());
    order.setFirstDeductStatus(PaymentStatusEnum.PENDING.name());

    OrderStateMachine.applyFirstDeductResult(order, true); // success=true

    // 断言
    assertThat(order.getOrderStatus())
        .isEqualTo(BenefitOrderStatusEnum.FIRST_DEDUCT_SUCCESS.name());
    assertThat(order.getSyncStatus())
        .isEqualTo(BenefitOrderStatusEnum.SYNC_SUCCESS.name());
}
```

---

## 支付处理测试

### 8. PaymentCallbackControllerIntegrationTest
**文件**: `controller/PaymentCallbackControllerIntegrationTest.java`
**职责**: 支付回调端点集成测试 (首次代扣、兜底代扣)

#### 测试方法

| 方法名 | 端点 | 场景 | 预期结果 |
|--------|------|------|---------|
| `shouldPersistFirstDeductSuccessAndUpdateOrder()` | POST `/callbacks/first-deduction` | 首次成功 | 支付记录 + 订单状态转换 |
| `shouldReplayDuplicateFirstDeductFailureWithoutCreatingSecondPayment()` | POST `/callbacks/first-deduction` (重复) | 首次失败重复 | 幂等性保证，无重复记录 |
| `shouldPersistFallbackDeductSuccess()` | POST `/callbacks/fallback-deduction` | 兜底成功 | 兜底支付记录持久化 |

#### 签名与回调格式
```java
// 回调请求示例
DeductionCallbackRequest {
    requestId: "cb_20260401_001",
    benefitOrderNo: "ord_20260401_001",
    paymentStatus: "SUCCESS",
    amount: 1000.00,
    failReason: null,
    channelTradeNo: "ch_20260401_001",
    timestamp: 1704067200
}

// HTTP 签名头
X-App-Id: test-app
X-Timestamp: 1704067200
X-Nonce: nonce_001
X-Signature: HMAC-SHA256(appId+timestamp+nonce, secret)
```

#### 幂等性验证
```java
@Test
void shouldReplayDuplicateWithoutDuplicatePaymentRecord() {
    // 第一次回调
    controller.handleFirstDeduction(callbackRequest);

    // 验证: 1 条支付记录
    PaymentRecord record1 = paymentRepo.findByRequestId("cb_001");
    assertThat(record1).isNotNull();

    // 第二次回调 (相同 requestId)
    controller.handleFirstDeduction(callbackRequest);

    // 验证: 仍然只有 1 条记录
    List<PaymentRecord> records = paymentRepo.findAllByRequestId("cb_001");
    assertThat(records).hasSize(1);
}
```

---

### 9. PaymentServiceTest
**文件**: `service/PaymentServiceTest.java`
**职责**: 支付服务逻辑单元测试

#### 测试方法

| 方法名 | 场景 | 预期结果 |
|--------|------|---------|
| `shouldReturnExistingPaymentForDuplicateCallback()` | 重复回调 | 返回已存支付记录 |
| `shouldPersistSuccessfulFirstDeductAndSyncOrder()` | 首次成功 | 持久化 + 同步下游 |
| `shouldRejectCallbackWhenOrderMissing()` | 订单不存在 | BizException("ORDER_NOT_FOUND") |

---

### 10. FallbackDeductServiceTest
**文件**: `service/FallbackDeductServiceTest.java`
**职责**: 兜底代扣触发逻辑

#### 测试方法

| 方法名 | 场景 | 预期结果 |
|--------|------|---------|
| `shouldReturnExistingFallbackRecordWhenAlreadyTriggered()` | 兜底已触发 | 返回已存记录（幂等） |
| `shouldCreatePendingFallbackPaymentWhenEligible()` | 首次创建 | 创建 PENDING 状态支付记录 |

---

## 通知处理测试

### 11. NotificationCallbackControllerIntegrationTest
**文件**: `controller/NotificationCallbackControllerIntegrationTest.java`
**职责**: 通知回调端点集成测试 (放款、还款、行权、退款)

#### 测试方法

| 方法名 | 端点 | 场景 | 预期结果 |
|--------|------|------|---------|
| `shouldTriggerSingleFallbackAfterGrantSuccess()` | POST `/callbacks/grant/forward` | 放款成功 + 首次失败 | 触发兜底代扣 |
| `shouldPersistRepaymentNotification()` | POST `/callbacks/repayment/forward` | 还款通知 | 审计日志 + 幂等记录 |
| `shouldMarkMissingRepaymentOrderAsFailed()` | 订单不存在 | 标记失败状态 |
| `shouldUpdateOrderForExerciseAndRefundCallbacks()` | 行权/退款回调 | 更新对应状态 |

#### 放款触发兜底场景
```
Order Status Transitions:
─────────────────────
初始: FIRST_DEDUCT_FAIL
      ↓
[等待下游通知]
      ↓
GrantCallback (SUCCESS) 到达
      ├─ OrderStateMachine.applyGrantResult(order)
      ├─ FallbackDeductService.triggerFallbackDeduct()
      └─ 订单状态 → FALLBACK_DEDUCT_PENDING
```

---

### 12. NotificationServiceTest
**文件**: `service/NotificationServiceTest.java`
**职责**: 通知服务逻辑单元测试

#### 测试方法

| 方法名 | 场景 | 预期结果 |
|--------|------|---------|
| `shouldTriggerFallbackOnGrantSuccessForFirstDeductFailOrder()` | 放款成功 + 首次失败 | 触发兜底 + 幂等标记 |
| `shouldIgnoreDuplicateRepaymentNotification()` | 重复还款通知 | 幂等跳过 |
| `shouldUpdateOrderForExerciseAndRefund()` | 行权/退款 | 状态更新 |
| `shouldWriteNotificationLogWhenRequestFirstSeen()` | 首次通知 | 审计日志创建 |
| `shouldMarkNotificationFailedWhenRepaymentOrderMissing()` | 订单不存在 | 标记失败 + 幂等跳过 |

---

## 工具类测试

### 13. JwtUtilTest
**文件**: `util/JwtUtilTest.java`
**职责**: JWT 生成与解析

#### 测试方法

| 方法名 | 场景 | 预期结果 |
|--------|------|---------|
| `shouldGenerateAndParseJwtToken()` | 生成并解析 | 往返一致 |
| `shouldRejectInvalidToken()` | 畸形 token | IllegalArgumentException |

#### JWT 载荷示例
```json
{
  "iss": "nexusfin-equity",
  "memberId": "mem_001",
  "techPlatformUserId": "tp_001",
  "iat": 1704067200,
  "exp": 1704153600
}
```

---

### 14. CookieUtilTest
**文件**: `util/CookieUtilTest.java`
**职责**: HTTP Cookie 安全属性构建

#### 测试方法

| 方法名 | 验证 | 预期值 |
|--------|------|--------|
| `shouldBuildSecureHttpOnlyCookie()` | Cookie 属性 | HttpOnly=true, Secure=true, SameSite=Lax |

#### Cookie 属性验证
```java
@Test
void shouldBuildSecureHttpOnlyCookie() {
    ResponseCookie cookie = CookieUtil.buildCookie("jwt_token_value", authProperties);

    assertThat(cookie.getName()).isEqualTo("NEXUSFIN_AUTH");
    assertThat(cookie.getValue()).isEqualTo("jwt_token_value");
    assertThat(cookie.isHttpOnly()).isTrue();
    assertThat(cookie.getSecure()).isTrue();
    assertThat(cookie.getSameSite()).isEqualTo("Lax");
    assertThat(cookie.getMaxAge()).isPositive(); // 长期有效
}
```

---

### 15. SensitiveDataCipherTest
**文件**: `util/SensitiveDataCipherTest.java`
**职责**: 敏感数据加密/解密和入站负载编码

#### 测试方法

| 方法名 | 场景 | 预期结果 |
|--------|------|---------|
| `shouldEncryptAndDecryptWithConfiguredDek()` | 加密往返 | 明文 → 密文 → 明文 |
| `shouldDecodeInboundBase64PayloadByChannelMode()` | Base64 解码 | 入站 Base64 数据正确解码 |

#### 加密格式验证
```java
@Test
void shouldEncryptWithFormatPrefix() {
    String plaintext = "13912345678"; // 敏感手机号
    String encrypted = cipher.encrypt(plaintext);

    // 格式: __AES_GCM__{nonce}{ciphertext}{tag}
    assertThat(encrypted).startsWith("__AES_GCM__");
    assertThat(encrypted.length()).isGreaterThan(plaintext.length());

    String decrypted = cipher.decrypt(encrypted);
    assertThat(decrypted).isEqualTo(plaintext);
}
```

---

### 16. SignatureServiceTest
**文件**: `config/SignatureServiceTest.java`
**职责**: HMAC-SHA256 签名生成与验证

#### 测试方法

| 方法名 | 场景 | 预期结果 |
|--------|------|---------|
| `shouldGenerateDeterministicSignatureForSameInput()` | 相同输入 | 签名一致 (64 字符 hex) |
| `shouldChangeSignatureWhenNonceChanges()` | 不同 nonce | 签名变化 |

#### 签名验证代码
```java
@Test
void shouldGenerateDeterministicSignature() {
    String appId = "test-app";
    String timestamp = "1704067200";
    String nonce = "nonce_001";
    String secret = "secret_key";

    String sig1 = SignatureService.sign(appId, timestamp, nonce, secret);
    String sig2 = SignatureService.sign(appId, timestamp, nonce, secret);

    assertThat(sig1).isEqualTo(sig2);
    assertThat(sig1).hasSize(64); // SHA256 hex = 64 chars
}
```

---

## 集成测试

### 17. BenefitOrderControllerIntegrationTest (详见 [订单管理测试](#订单管理测试))

### 18. MySqlCallbackFlowIntegrationTest
**文件**: `controller/MySqlCallbackFlowIntegrationTest.java`
**职责**: 端到端回调流程持久化测试 (MySQL)

#### 条件化执行
```java
@EnabledIfEnvironmentVariable(named = "MYSQL_IT_ENABLED", matches = "true")
@ActiveProfiles("mysql-it")
```

#### 测试方法

| 方法名 | 流程 | 验证点 |
|--------|------|--------|
| `shouldPersistPaymentAndGrantFlowInMySql()` | 首次失败 → 放款成功 → 兜底 | 订单转换 + 支付记录 + 通知日志 + 幂等 |

#### 完整业务流程
```
1. 创建订单
   ├─ 订单状态: FIRST_DEDUCT_PENDING
   └─ 首次支付: PENDING

2. 首次代扣失败回调
   ├─ 订单状态: FIRST_DEDUCT_FAIL
   ├─ 支付状态: FAILED
   └─ 失败原因: 余额不足

3. 放款成功通知
   ├─ 订单状态: FALLBACK_DEDUCT_PENDING
   └─ 触发兜底代扣

4. 兜底成功回调
   ├─ 订单状态: FALLBACK_DEDUCT_SUCCESS
   └─ 支付状态: SUCCESS

5. 审计记录
   ├─ NotificationReceiveLog (2 条)
   ├─ PaymentRecord (2 条)
   └─ IdempotencyRecord (3 条)
```

---

### 19. MySqlRoundTripIntegrationTest
**文件**: `controller/MySqlRoundTripIntegrationTest.java`
**职责**: 订单创建与检索的往返流程 (MySQL)

#### 测试方法

| 方法名 | 操作 | 验证 |
|--------|------|------|
| `shouldWriteToAndReadFromMySql()` | 创建订单 → 检索 | 数据完整性 + 加密字段安全 |

#### 数据一致性验证
```java
@Test
void shouldWriteToAndReadFromMySql() {
    // 1. 创建订单
    CreateBenefitOrderResponse createResp = controller.createOrder(request);
    String benefitOrderNo = createResp.benefitOrderNo();

    // 2. 创建签约任务和合同存档
    List<SignTask> signTasks = signTaskRepo
        .selectList(new QueryWrapper<SignTask>()
            .eq("benefit_order_no", benefitOrderNo));
    assertThat(signTasks).hasSize(2);

    // 3. 检索订单状态
    BenefitOrderStatusResponse statusResp = controller.getOrderStatus(benefitOrderNo);

    // 4. 验证加密字段
    assertThat(statusResp.memberInfo().mobile()).startsWith("__AES_GCM__");

    // 5. 验证状态转换
    assertThat(statusResp.orderStatus()).isEqualTo("FIRST_DEDUCT_PENDING");
}
```

---

## 支撑服务测试

### 20. AgreementServiceTest
**文件**: `service/AgreementServiceTest.java`
**职责**: 协议生成 (签约任务 + 合同存档)

#### 测试方法

| 方法名 | 场景 | 预期结果 |
|--------|------|---------|
| `shouldGenerateSignTasksAndArchivesForMissingArtifacts()` | 新订单 | 2 个 SignTask + 2 个 ContractArchive |
| `shouldReuseExistingSignTaskAndArchive()` | 已存在 | 无新建 |

#### 生成的签约任务
```
SignTask 1:
- taskNo: agr_20260401_001
- contractType: EQUITY_AGREEMENT
- signUrl: https://signing-service/task/...
- signStatus: PENDING

SignTask 2:
- taskNo: agr_20260401_002
- contractType: DEFERRED_AGREEMENT
- signUrl: https://signing-service/task/...
- signStatus: PENDING
```

---

### 21. IdempotencyServiceTest
**文件**: `service/IdempotencyServiceTest.java`
**职责**: 幂等性记录管理

#### 测试方法

| 方法名 | 场景 | 预期结果 |
|--------|------|---------|
| `shouldInsertRecordWhenRequestNotProcessed()` | 首次请求 | 新增记录 |
| `shouldSkipInsertWhenRequestAlreadyProcessed()` | 重复请求 | 无操作 |
| `shouldReturnStoredRecordByRequestId()` | 查询记录 | 返回已存 |

#### 幂等记录结构
```java
IdempotencyRecord {
    id: 1,
    requestId: "cb_20260401_001",
    bizType: "PAYMENT",
    bizKey: "pmt_20260401_001",
    bizStatus: "SUCCESS",
    createdTs: 1704067200000
}
```

---

### 22. ReconciliationServiceTest
**文件**: `service/ReconciliationServiceTest.java`
**职责**: 订单/支付/通知查询

#### 测试方法

| 方法名 | 查询 | 预期 |
|--------|------|------|
| `shouldQueryAcrossCoreIdentifiers()` | 按订单号/支付号/通知查询 | 返回匹配记录 |

---

### 23. DownstreamSyncServiceTest
**文件**: `service/DownstreamSyncServiceTest.java`
**职责**: 下游同步路由决策

#### 测试方法

| 方法名 | 场景 | 路由决策 |
|--------|------|---------|
| `shouldSyncDirectContinuationOrder()` | 首次成功 | DIRECT_CONTINUE |
| `shouldSyncFallbackEligibleOrder()` | 首次失败 | FALLBACK_ELIGIBLE |
| `shouldRejectUnsupportedOrderStatus()` | 无效状态 | BizException |

#### 路由决策树
```
Order.firstDeductStatus
├─ SUCCESS → Route: DIRECT_CONTINUE
├─ FAILED → Route: FALLBACK_ELIGIBLE
└─ NONE → Error: UNSUPPORTED
```

---

## 第三方集成测试

### 24. TechPlatformClientTest
**文件**: `thirdparty/techplatform/TechPlatformClientTest.java`
**职责**: 科技平台 API 客户端 (加密、签名、解密)

#### 测试方法

| 方法名 | 操作 | 验证 |
|--------|------|------|
| `shouldSendLoanInfoNoticeWithSignedEncryptedPayload()` | 发送借贷通知 | 加密 + 签名 + 头部 |
| `shouldParseEncryptedNotifyResponse()` | 解析响应 | 解密 + 解析 |
| `shouldRejectNonSuccessNotifyResponse()` | 失败响应 | BizException |

#### 请求流程
```
LoanInfoNoticeRequest {
    loanOrderNo: "loan_20260401_001",
    status: "DISBURSED",
    amount: 100000.00,
    timestamp: 1704067200
}
        ↓ [加密]
Encrypted payload (AES/GCM)
        ↓ [签名]
HMAC-SHA256 signature
        ↓ [HTTP POST]
Tech-Platform 服务
        ↓ [响应]
EncryptedResponse {
    encryptedData: "...",
    signature: "..."
}
        ↓ [解密]
NotifyResponse {
    code: "SUCCESS",
    message: "OK"
}
```

---

### 25. TechPlatformUserClientImplTest
**文件**: `service/impl/TechPlatformUserClientImplTest.java`
**职责**: 用户资料查询 (重试逻辑)

#### 测试方法

| 方法名 | 场景 | 预期结果 |
|--------|------|---------|
| `shouldRetryTechPlatformUserLookupAfterTransientFailure()` | 瞬时失败 | 重试成功 |
| `shouldLogAttemptsAndThrowAfterRetryExhausted()` | 重试耗尽 | 日志 + BizException |

#### 重试配置
```
maxAttempts: 2
backoffDelay: 100ms
backoffMultiplier: 1.0
retryableExceptions: [SocketException, HttpServerErrorException]
```

---

## 测试数据与 Fixtures

### 测试数据初始化

#### 方式 1: SQL 脚本
```sql
-- db/test-data.sql
INSERT INTO benefit_product (product_code, product_name, fee_rate, status)
VALUES ('PROD_001', '权益产品 1', 0.05, 'ACTIVE');

INSERT INTO member_info (member_id, tech_platform_user_id, external_user_id, ...)
VALUES ('mem_001', 'tp_001', 'ext_001', ...);
```

#### 方式 2: 编程创建
```java
@BeforeEach
void setUp() {
    BenefitProduct product = new BenefitProduct();
    product.setProductCode("PROD_001");
    product.setProductName("权益产品");
    product.setStatus("ACTIVE");
    productRepository.insert(product);
}
```

#### 敏感数据加密
```java
String encrypted_mobile = cipher.encrypt("13912345678");
String encrypted_idCard = cipher.encrypt("110101199001011234");
String encrypted_realName = cipher.encrypt("张三");

MemberInfo member = new MemberInfo();
member.setMobile(encrypted_mobile);
member.setIdCard(encrypted_idCard);
member.setRealName(encrypted_realName);
memberRepository.insert(member);
```

---

## Mock 策略

### Mock 工具

| 工具 | 用途 | 配置 |
|------|------|------|
| **Mockito** | 依赖 Mock | `@ExtendWith(MockitoExtension.class)` |
| **MockRestServiceServer** | HTTP Mock | `mockServer = MockRestServiceServer.createServer(restTemplate)` |
| **@MockBean** | Spring Bean Mock | `@MockBean TechPlatformUserClient client` |
| **@Spy** | 部分 Mock | `@Spy BenefitOrderRepository repo` |

### 常见 Mock 场景

#### 1. 服务依赖 Mock
```java
@ExtendWith(MockitoExtension.class)
class BenefitOrderServiceTest {

    @Mock
    BenefitProductRepository productRepo;

    @Mock
    QwBenefitClient qwClient;

    @InjectMocks
    BenefitOrderServiceImpl service;

    @Test
    void test() {
        when(productRepo.selectById("PROD_001"))
            .thenReturn(createMockProduct());

        when(qwClient.syncMemberOrder(any()))
            .thenReturn(new QwMemberSyncResponse("OK"));
    }
}
```

#### 2. HTTP 客户端 Mock
```java
@Test
void testTechPlatformClient() {
    mockServer.expect(post("/api/loans/notify"))
        .andRespond(withSuccess(
            "{\"code\": \"SUCCESS\"}",
            MediaType.APPLICATION_JSON
        ));

    TechPlatformResponse response = client.notifyLoanInfo(request);

    mockServer.verify();
    assertThat(response.code()).isEqualTo("SUCCESS");
}
```

#### 3. 异常 Mock
```java
@Test
void testRetryOnFailure() {
    when(userClient.getProfile(userId))
        .thenThrow(new SocketException("Connection timeout"))
        .thenReturn(new UserProfile(...));

    UserProfile profile = userClient.getProfile(userId);
    // 验证重试行为
    verify(userClient, times(2)).getProfile(userId);
}
```

---

## 测试执行指南

### 运行所有测试
```bash
mvn test
```

### 运行特定测试类
```bash
mvn test -Dtest=BenefitOrderServiceTest
```

### 运行 MySQL 集成测试
```bash
export MYSQL_IT_ENABLED=true
mvn test -Dtest=MySql*
```

### 测试覆盖率
```bash
mvn clean test jacoco:report
# 报告: target/site/jacoco/index.html
```

### 快速反馈
```bash
mvn test -Dtest=BenefitOrderServiceTest#shouldCreateBenefitOrderAndPersistAgreementArtifacts
```

---

## 测试维护清单

### 新增功能测试
- [ ] 单元测试覆盖所有代码路径
- [ ] 集成测试覆盖 Controller 端点
- [ ] 添加幂等性测试
- [ ] 更新此清单

### 回归测试
- [ ] 所有既有测试通过
- [ ] 代码覆盖率 >= 80%
- [ ] MySQL 集成测试通过

### 性能测试 (未来)
- [ ] 单个订单创建 < 500ms
- [ ] QW 同步 < 1000ms
- [ ] 回调处理 < 200ms

---

## 已知限制与改进机会

### 当前限制
1. **异步测试**: 缺乏 @Async 方法的测试
2. **并发测试**: 无多线程场景测试
3. **性能测试**: 无负载/压力测试
4. **容错测试**: @Retry 和熔断器需补充测试

### 改进建议
1. ✅ 添加异步订单同步测试
2. ✅ 添加并发幂等性测试
3. ✅ 添加 @Retry 重试行为验证
4. ✅ 添加熔断器降级路径测试

---

**文档生成时间**: 2026-03-31
**测试框架版本**: JUnit 5, Spring Boot Test, Mockito 4+
**下次更新**: 新增功能或重大重构后
