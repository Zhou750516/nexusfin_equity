# 测试最佳实践与常见问题

**文档日期**: 2026-03-31
**目的**: 指导新开发者编写高质量测试用例

---

## 目录

1. [命名约定](#命名约定)
2. [测试结构](#测试结构)
3. [Mock 策略](#mock-策略)
4. [常见问题](#常见问题)
5. [反面模式](#反面模式)
6. [性能优化](#性能优化)

---

## 命名约定

### 测试类命名

#### 规则
```
[被测类名] + Test
```

#### 示例
```
类                          测试类
─────────────────────────────────────────
BenefitOrderService    →  BenefitOrderServiceTest
AuthController         →  AuthControllerTest
JwtUtil                →  JwtUtilTest
OrderStateMachine      →  OrderStateMachineTest
```

#### 特殊情况
```
特殊类型                    命名模式
────────────────────────────────────────
集成测试                 [Name]IntegrationTest
认证相关                 [Name]AuthTest
MySQL 特定              MySQL[Name]Test
端到端                   [Name]E2ETest
性能                     [Name]PerformanceTest
```

---

### 测试方法命名

#### 规则
```
should[期望结果]When[条件]
should[期望结果][On/For][对象]
```

#### 示例

✅ **好的命名**
```java
@Test
void shouldReturnProductPageWithMemberInfo() { }

@Test
void shouldCreateBenefitOrderAndPersistAgreementArtifacts() { }

@Test
void shouldReplayDuplicateCreateOrderRequest() { }

@Test
void shouldRejectOrderCreationWhenProductMissing() { }

@Test
void shouldPersistFirstDeductSuccessAndUpdateOrder() { }
```

❌ **不好的命名**
```java
@Test
void test1() { }                    // 无意义

@Test
void testOrderCreation() { }        // 不清楚期望

@Test
void createOrderTest() { }          // 顺序错误

@Test
void testOrderWithProductNotFound() { }  // 不用 when

@Test
void orderTest_success() { }        // 不一致的大小写
```

---

## 测试结构

### AAA 模式 (Arrange-Act-Assert)

#### 标准模板
```java
@Test
void shouldBehaviorWhenCondition() {
    // === Arrange: 准备测试数据 ===
    BenefitProduct product = createMockProduct();
    BenefitOrder order = createMockOrder();

    // === Act: 执行被测方法 ===
    CreateBenefitOrderResponse response = service.createOrder(
        "mem_001",
        new CreateBenefitOrderRequest(
            "req_001",
            "PROD_001",
            1000.00,
            true
        )
    );

    // === Assert: 验证结果 ===
    assertThat(response.benefitOrderNo()).isNotEmpty();
    assertThat(response.orderStatus()).isEqualTo("FIRST_DEDUCT_PENDING");
    verify(orderRepository).insert(any(BenefitOrder.class));
}
```

#### 分步骤注释
```java
@Test
void shouldCreateOrderWithEncryptedSensitiveData() {
    // [准备阶段] 1. 创建成员
    MemberInfo member = new MemberInfo();
    member.setMemberId("mem_001");
    member.setMobile(cipher.encrypt("13912345678"));
    memberRepository.insert(member);

    // [准备阶段] 2. 创建产品
    BenefitProduct product = new BenefitProduct();
    product.setProductCode("PROD_001");
    product.setStatus("ACTIVE");
    productRepository.insert(product);

    // [执行阶段] 3. 调用服务
    CreateBenefitOrderResponse response = service.createOrder(
        "mem_001",
        new CreateBenefitOrderRequest("req_001", "PROD_001", 1000.00, true)
    );

    // [验证阶段] 4. 检查订单创建
    BenefitOrder order = orderRepository.selectById(response.benefitOrderNo());
    assertThat(order).isNotNull();

    // [验证阶段] 5. 检查协议生成
    List<SignTask> tasks = signTaskRepository.selectList(
        new QueryWrapper<SignTask>()
            .eq("benefit_order_no", order.getBenefitOrderNo())
    );
    assertThat(tasks).hasSize(2);

    // [验证阶段] 6. 检查幂等性记录
    IdempotencyRecord record = idempotencyRepository.selectOne(
        new QueryWrapper<IdempotencyRecord>()
            .eq("request_id", "req_001")
    );
    assertThat(record.getBizKey()).isEqualTo(response.benefitOrderNo());
}
```

---

## Mock 策略

### 何时使用 Mock

| 场景 | 使用 Mock | 理由 |
|------|---------|------|
| 数据库操作 | ✅ 单元测试 | 隔离逻辑，加快速度 |
| HTTP 调用 | ✅ 单元测试 | 外部依赖，控制响应 |
| 文件 I/O | ✅ 单元测试 | 不依赖文件系统 |
| 工具类 | ❌ 集成测试 | 加密/签名需真实 |
| 状态机 | ❌ 单元测试 | 核心逻辑应真实测试 |

### Mock 依赖最佳实践

#### 方案 1: 最小化 Mock
```java
// ❌ 不好: Mock 过多，失去测试价值
@Mock
private BenefitProductRepository productRepo;

@Mock
private BenefitOrderRepository orderRepo;

@Mock
private MemberInfoRepository memberRepo;

@Mock
private MemberChannelRepository channelRepo;

// 太多 Mock 使测试变得脆弱
@Test
void shouldCreateOrder() {
    when(productRepo.selectById(anyString()))
        .thenReturn(mock(BenefitProduct.class));
    when(memberRepo.selectById(anyString()))
        .thenReturn(mock(MemberInfo.class));
    // ... 10 个 when-then
    // 实际上测试的不是逻辑，而是 Mock 的配置
}
```

#### 方案 2: 真实数据库 + Mock 外部
```java
// ✅ 好: 集成测试中使用真实数据库
@SpringBootTest
@AutoConfigureMockMvc
class BenefitOrderControllerIntegrationTest {

    @Autowired
    private BenefitOrderRepository orderRepo;

    @MockBean
    private QwBenefitClient qwClient;  // 仅 Mock 外部依赖

    @Test
    void shouldCreateOrder() {
        when(qwClient.syncMemberOrder(any()))
            .thenReturn(new QwMemberSyncResponse("OK"));

        // 调用真实服务，真实数据库
        CreateBenefitOrderResponse response = controller.createOrder(request);

        // 验证真实数据库中的数据
        BenefitOrder order = orderRepo.selectById(response.benefitOrderNo());
        assertThat(order).isNotNull();
    }
}
```

### Mock 验证

#### verify() 最佳实践
```java
// ✅ 好: 精确验证
@Test
void shouldSyncOrderToQwAfterCreation() {
    service.createOrder(memberId, request);

    // 验证: QW 客户端被调用了恰好 1 次
    verify(qwClient, times(1)).syncMemberOrder(any());

    // 验证: 使用了正确的参数
    verify(qwClient).syncMemberOrder(
        argThat(req ->
            req.getMemberId().equals("mem_001") &&
            req.getProductCode().equals("PROD_001")
        )
    );

    // 验证: 没有其他调用
    verifyNoMoreInteractions(qwClient);
}

// ❌ 不好: 过度验证
@Test
void test() {
    service.createOrder(memberId, request);

    verify(productRepo).selectById(anyString());     // 过于通用
    verify(orderRepo).insert(any());                  // 验证了实现细节
    verify(agreementService).ensureAgreementArtifacts(any());
    // ... 验证了所有内部调用，测试变得脆弱
}
```

---

## 常见问题

### Q1: 如何测试加密字段?

**答**:
```java
@Test
void shouldEncryptSensitiveDataOnCreation() {
    // 创建成员
    MemberInfo member = new MemberInfo();
    member.setMobile("13912345678");  // 明文
    member.setIdCard("110101199001011234");
    member.setRealName("张三");

    // 保存时应加密
    cipher.encrypt(member);

    // 验证: 加密字段不为明文
    assertThat(member.getMobile())
        .startsWith("__AES_GCM__")
        .doesNotContain("13912345678");

    // 验证: 可解密回原值
    String decrypted = cipher.decrypt(member.getMobile());
    assertThat(decrypted).isEqualTo("13912345678");
}
```

---

### Q2: 如何测试幂等性?

**答**:
```java
@Test
void shouldEnforceIdempotency() {
    // 准备请求
    CreateBenefitOrderRequest request = new CreateBenefitOrderRequest(
        "req_001",  // 相同的 requestId
        "PROD_001",
        1000.00,
        true
    );

    // 第一次调用
    CreateBenefitOrderResponse response1 = service.createOrder("mem_001", request);

    // 第二次调用相同请求
    CreateBenefitOrderResponse response2 = service.createOrder("mem_001", request);

    // 验证: 返回相同订单
    assertThat(response1.benefitOrderNo())
        .isEqualTo(response2.benefitOrderNo());

    // 验证: 数据库中仅有一条订单
    List<BenefitOrder> orders = orderRepository.selectList(
        new QueryWrapper<BenefitOrder>()
            .eq("request_id", "req_001")
    );
    assertThat(orders).hasSize(1);

    // 验证: 幂等性记录被创建
    IdempotencyRecord record = idempotencyRepository.selectOne(
        new QueryWrapper<IdempotencyRecord>()
            .eq("request_id", "req_001")
    );
    assertThat(record.getCreatedTs()).isNotNull();
}
```

---

### Q3: 如何测试状态转换?

**答**:
```java
@Test
void shouldValidateStateTransitions() {
    BenefitOrder order = new BenefitOrder();
    order.setOrderStatus(BenefitOrderStatusEnum.FIRST_DEDUCT_PENDING.name());

    // ✅ 有效转换: 首次成功
    OrderStateMachine.applyFirstDeductResult(order, true);
    assertThat(order.getOrderStatus())
        .isEqualTo(BenefitOrderStatusEnum.FIRST_DEDUCT_SUCCESS.name());

    // ❌ 无效转换: 再次尝试首次代扣
    BenefitException exception = assertThrows(BizException.class, () -> {
        OrderStateMachine.applyFirstDeductResult(order, false);
    });
    assertThat(exception.getErrorCode()).isEqualTo("INVALID_STATE_TRANSITION");
}

@Test
void shouldPreventInvalidTransitions() {
    BenefitOrder order = new BenefitOrder();
    order.setOrderStatus(BenefitOrderStatusEnum.FIRST_DEDUCT_SUCCESS.name());

    // ❌ 不允许: 已成功无法转换到失败
    BizException exception = assertThrows(BizException.class, () -> {
        OrderStateMachine.applyFirstDeductResult(order, false);
    });
    assertThat(exception.getMessage())
        .contains("Cannot transition from FIRST_DEDUCT_SUCCESS");
}
```

---

### Q4: 如何测试签名验证?

**答**:
```java
@Test
void shouldValidateCallbackSignature() {
    String appId = "test-app";
    String timestamp = "1704067200";
    String nonce = "nonce_001";
    String secret = "secret_key";

    // 生成正确的签名
    String correctSignature = SignatureService.sign(appId, timestamp, nonce, secret);

    // 验证: 正确签名应通过
    boolean valid = SignatureService.verify(
        appId, timestamp, nonce, correctSignature, secret
    );
    assertThat(valid).isTrue();

    // 验证: 错误的签名应拒绝
    String wrongSignature = "abc123";
    boolean invalid = SignatureService.verify(
        appId, timestamp, nonce, wrongSignature, secret
    );
    assertThat(invalid).isFalse();

    // 验证: 时间戳校验 (假设 maxSkew=300s)
    String expiredTimestamp = String.valueOf(
        Long.parseLong(timestamp) - 400  // 超过 300s
    );
    String expiredSignature = SignatureService.sign(
        appId, expiredTimestamp, nonce, secret
    );
    BizException exception = assertThrows(BizException.class, () -> {
        controller.handleCallback(appId, expiredTimestamp, nonce, expiredSignature, body);
    });
    assertThat(exception.getErrorCode()).isEqualTo("SIGNATURE_TIMESTAMP_SKEW");
}
```

---

### Q5: 如何测试异常情况?

**答**:
```java
@Test
void shouldThrowExceptionWithValidErrorCode() {
    // 测试: 产品不存在
    BizException exception = assertThrows(BizException.class, () -> {
        service.getProductPage("NONEXISTENT", "mem_001");
    });

    assertThat(exception.getErrorCode()).isEqualTo("PRODUCT_NOT_FOUND");
    assertThat(exception.getMessage()).contains("not found");
}

@Test
void shouldHandleMultipleErrorConditions() {
    // 多个错误条件应按优先级报告
    BenefitOrderRequest request = new BenefitOrderRequest(
        null,              // 缺少 requestId
        "NONEXISTENT",     // 缺少产品
        -1000.00,          // 无效金额
        false              // 协议未签署
    );

    BizException exception = assertThrows(BizException.class, () -> {
        service.createOrder("mem_001", request);
    });

    // 应该报告第一个错误
    assertThat(exception.getErrorCode()).isEqualTo("INVALID_REQUEST_ID");
}
```

---

## 反面模式

### ❌ 反面模式 1: 测试实现细节而非行为

```java
// ❌ 不好: 测试了如何创建，而非验证了什么被创建
@Test
void testOrderCreation() {
    // 直接验证数据库调用
    verify(orderRepository, times(1)).insert(any());
    verify(agreementService, times(1)).ensureAgreementArtifacts(any());
    verify(qwClient, times(1)).syncMemberOrder(any());
    // 这些验证都是实现细节，如果重构服务，测试就会失败
}

// ✅ 好: 测试业务行为
@Test
void shouldCreateOrderWithSignedAgreements() {
    CreateBenefitOrderResponse response = service.createOrder("mem_001", request);

    // 验证业务行为: 订单已创建
    BenefitOrder order = orderRepository.selectById(response.benefitOrderNo());
    assertThat(order).isNotNull();

    // 验证业务行为: 协议任务已生成
    List<SignTask> tasks = signTaskRepository.selectList(
        new QueryWrapper<SignTask>()
            .eq("benefit_order_no", response.benefitOrderNo())
    );
    assertThat(tasks).hasSize(2);

    // 验证业务行为: 订单就绪进入支付流程
    assertThat(order.getOrderStatus()).isEqualTo("FIRST_DEDUCT_PENDING");
}
```

---

### ❌ 反面模式 2: 过度 Mock

```java
// ❌ 不好: Mock 了太多，实际上没有测试任何东西
@Test
void testOrderService() {
    when(productRepo.selectById(any())).thenReturn(mock(BenefitProduct.class));
    when(memberRepo.selectById(any())).thenReturn(mock(MemberInfo.class));
    when(channelRepo.selectOne(any())).thenReturn(mock(MemberChannel.class));
    when(agreementService.ensureAgreementArtifacts(any())).thenReturn(null);
    when(qwClient.syncMemberOrder(any())).thenReturn(mock(QwMemberSyncResponse.class));

    // 测试实际上是在验证 Mock 的配置，不是真正的业务逻辑
    CreateBenefitOrderResponse response = service.createOrder("mem_001", request);
    assertThat(response).isNotNull();  // 这个断言几乎没有意义
}

// ✅ 好: 集成测试中使用真实数据
@Test
void testOrderService() {
    // 创建真实的产品
    BenefitProduct product = new BenefitProduct();
    product.setProductCode("PROD_001");
    product.setStatus("ACTIVE");
    productRepository.insert(product);

    // Mock 仅外部依赖
    when(qwClient.syncMemberOrder(any()))
        .thenReturn(new QwMemberSyncResponse("SUCCESS"));

    // 测试真实的业务逻辑
    CreateBenefitOrderResponse response = service.createOrder("mem_001", request);

    // 验证真实结果
    BenefitOrder order = orderRepository.selectById(response.benefitOrderNo());
    assertThat(order.getOrderStatus()).isEqualTo("FIRST_DEDUCT_PENDING");
}
```

---

### ❌ 反面模式 3: 单个测试验证过多场景

```java
// ❌ 不好: 一个测试做太多事
@Test
void testOrderFlow() {
    // 创建订单
    CreateBenefitOrderResponse response = service.createOrder("mem_001", request);

    // 支付回调
    service.handleFirstDeductCallback(new DeductionCallbackRequest(...));

    // 放款通知
    service.handleGrantCallback(new GrantNotificationRequest(...));

    // 行权
    service.handleExerciseCallback(new ExerciseCallbackRequest(...));

    // 单个失败无法定位问题，且难以维护
}

// ✅ 好: 按场景拆分
@Test
void shouldCreateOrderWithPendingPayment() {
    CreateBenefitOrderResponse response = service.createOrder("mem_001", request);
    assertThat(response.benefitOrderNo()).isNotEmpty();
}

@Test
void shouldApplyFirstDeductSuccessToOrder() {
    BenefitOrder order = createOrderInDatabase();

    service.handleFirstDeductCallback(
        new DeductionCallbackRequest(order.getBenefitOrderNo(), "SUCCESS")
    );

    BenefitOrder updated = orderRepository.selectById(order.getBenefitOrderNo());
    assertThat(updated.getFirstDeductStatus()).isEqualTo("SUCCESS");
}

@Test
void shouldTriggerFallbackOnGrantSuccess() {
    BenefitOrder order = createOrderWithFailedFirstDeduct();

    service.handleGrantCallback(
        new GrantNotificationRequest(order.getBenefitOrderNo(), "SUCCESS")
    );

    BenefitOrder updated = orderRepository.selectById(order.getBenefitOrderNo());
    assertThat(updated.getOrderStatus()).isEqualTo("FALLBACK_DEDUCT_PENDING");
}
```

---

## 性能优化

### 测试执行优化

#### 1. 并行执行
```java
// junit-platform.properties
junit.jupiter.execution.parallel.enabled=true
junit.jupiter.execution.parallel.mode.default=concurrent
junit.jupiter.execution.parallel.mode.classes.default=concurrent
```

#### 2. 共享测试数据
```java
// ❌ 不好: 每个测试重复创建
@Test
void test1() {
    BenefitProduct product = createProduct("PROD_001");
    // ...
}

@Test
void test2() {
    BenefitProduct product = createProduct("PROD_001");
    // ...
}

// ✅ 好: 使用 @TestInstance(Lifecycle.PER_CLASS)
@TestInstance(Lifecycle.PER_CLASS)
class BenefitOrderServiceTest {
    private BenefitProduct sharedProduct;

    @BeforeAll
    void setUpSharedData() {
        sharedProduct = createProduct("PROD_001");
    }

    @Test
    void test1() {
        // 使用 sharedProduct
    }

    @Test
    void test2() {
        // 使用 sharedProduct
    }
}
```

#### 3. 减少数据库往返
```java
// ❌ 不好: 多次查询
@Test
void test() {
    service.createOrder(memberId, request);

    BenefitOrder order = orderRepository.selectById(benefitOrderNo);
    SignTask task = signTaskRepository.selectById(taskNo);
    ContractArchive contract = contractRepository.selectById(contractNo);
    // 3 次数据库查询
}

// ✅ 好: 批量查询
@Test
void test() {
    service.createOrder(memberId, request);

    List<Object> results = orderRepository.selectList(
        new QueryWrapper<BenefitOrder>()
            .eq("benefit_order_no", benefitOrderNo)
            .join(SignTask.class) // Left join
            .join(ContractArchive.class)
    );
    // 1 次查询获得所有数据
}
```

---

### 依赖注入优化

```java
// ❌ 不好: 每个测试都重新初始化 Spring Context
@SpringBootTest
class TestA { }

@SpringBootTest
class TestB { }

// ✅ 好: 共享 Spring Context
@SpringBootTest
class BenefitOrderServiceTest { }

@SpringBootTest
class PaymentServiceTest { }
```

---

## 测试文档注释

### 为复杂测试添加文档

```java
/**
 * 验证幂等性保证: 重复的订单创建请求应返回相同的订单，
 * 不创建重复的支付记录或协议任务。
 *
 * 场景:
 * 1. 首次请求: requestId="req_001" → 创建订单 ord_001
 * 2. 重复请求: requestId="req_001" → 返回 ord_001 (无重复)
 *
 * 验证点:
 * - 数据库中仅有 1 条订单
 * - 幂等性记录正确保存
 * - 无重复的签约任务
 *
 * 关键代码: IdempotencyService.markProcessed()
 */
@Test
void shouldEnforceIdempotencyForDuplicateOrderRequests() {
    // ...
}
```

---

## 总结检查清单

创建新测试前:
- [ ] 命名遵循 `shouldXWhenY` 格式
- [ ] 使用 AAA 模式 (Arrange-Act-Assert)
- [ ] 仅 Mock 外部依赖，内部逻辑用真实对象
- [ ] 验证业务行为，而非实现细节
- [ ] 一个测试一个场景
- [ ] 添加有意义的注释
- [ ] 测试应该是独立的，可以任意顺序运行
- [ ] 避免测试间的数据依赖

---

**文档维护**: 每个 Sprint 更新
**最后更新**: 2026-03-31
