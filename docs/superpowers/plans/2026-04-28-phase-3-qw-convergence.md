# Phase 3 QW Convergence Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Collapse the QW / Allinpay direct integration into fewer request-side building blocks, remove `Skeleton*` placeholder fan-out, and split oversized `QwProperties` into smaller subdomain config classes without changing external business behavior.

**Architecture:** Keep `QwBenefitClient`, `RoutingQwBenefitClient`, and the existing service-facing APIs unchanged. Move request-side orchestration into a single `AllinpayDirectRequestBuilder`, replace the default `Skeleton*` placeholders with a smaller unsupported-protocol handler set, and shrink `QwProperties` into a thin root object that delegates to focused nested config classes. Preserve all current unsupported-operation behavior, mock-mode behavior, and existing direct-mode wiring semantics.

**Tech Stack:** Java 17, Spring Boot 3.2, JUnit 5, Mockito, ApplicationContextRunner, Maven

---

## Baseline

- Baseline capture date: `2026-04-28`
- Current `Allinpay*` file count under `src/main/java/com/nexusfin/equity/thirdparty/qw`: `37`
- Current `QwProperties.java` size: `359` lines
- Current runtime constraints to preserve:
  1. `QwBenefitClient` public methods and `RoutingQwBenefitClient` mode switching stay unchanged.
  2. `AllinpayDirectQwBenefitClient` keeps current unsupported behavior for sign query/apply/confirm.
  3. Default direct-mode protocol path remains intentionally unimplemented and still throws the same `ALLINPAY_DIRECT_PROTOCOL_UNIMPLEMENTED` family of `BizException`s.
  4. Yunka / Xiaohua / repayment codepaths are out of scope.

## File Structure

### Files to Create

- `docs/superpowers/plans/2026-04-28-phase-3-qw-convergence.md`
- `src/main/java/com/nexusfin/equity/thirdparty/qw/AllinpayDirectRequestBuilder.java`
- `src/main/java/com/nexusfin/equity/thirdparty/qw/AllinpayDirectUnsupportedProtocolHandler.java`
- `src/main/java/com/nexusfin/equity/config/QwHttpProperties.java`
- `src/main/java/com/nexusfin/equity/config/QwSecurityProperties.java`
- `src/main/java/com/nexusfin/equity/config/QwPaymentProperties.java`
- `src/main/java/com/nexusfin/equity/config/QwDirectProperties.java`
- `src/test/java/com/nexusfin/equity/thirdparty/qw/AllinpayDirectRequestBuilderTest.java`
- `src/test/java/com/nexusfin/equity/thirdparty/qw/AllinpayDirectUnsupportedProtocolHandlerTest.java`

### Files to Modify

- `src/main/java/com/nexusfin/equity/config/QwProperties.java`
- `src/main/java/com/nexusfin/equity/config/AllinpayDirectConfiguration.java`
- `src/main/java/com/nexusfin/equity/config/QwPayProtocolOverrideGuard.java`
- `src/main/java/com/nexusfin/equity/service/impl/PaymentProtocolServiceImpl.java`
- `src/main/java/com/nexusfin/equity/thirdparty/qw/AllinpayDirectQwBenefitClient.java`
- `src/main/java/com/nexusfin/equity/thirdparty/qw/QwBenefitClientImpl.java`
- `src/test/java/com/nexusfin/equity/thirdparty/qw/AllinpayDirectConfigurationTest.java`
- `src/test/java/com/nexusfin/equity/thirdparty/qw/AllinpayDirectQwBenefitClientTest.java`
- `src/test/java/com/nexusfin/equity/thirdparty/qw/QwPropertiesTest.java`
- `src/test/java/com/nexusfin/equity/thirdparty/qw/QwBenefitClientImplTest.java`
- `src/test/java/com/nexusfin/equity/service/PaymentProtocolServiceTest.java`
- `src/test/java/com/nexusfin/equity/config/QwPayProtocolOverrideGuardTest.java`

### Files to Delete

- `src/main/java/com/nexusfin/equity/thirdparty/qw/AllinpayDirectRequestFactory.java`
- `src/main/java/com/nexusfin/equity/thirdparty/qw/AllinpayDirectPayloadMapperRegistry.java`
- `src/main/java/com/nexusfin/equity/thirdparty/qw/AllinpayDirectEnvelopeFactory.java`
- `src/main/java/com/nexusfin/equity/thirdparty/qw/AllinpayDirectRequestPreparer.java`
- `src/main/java/com/nexusfin/equity/thirdparty/qw/AllinpayDirectSkeletonHttpExecutor.java`
- `src/main/java/com/nexusfin/equity/thirdparty/qw/AllinpayDirectSkeletonResponseVerificationStage.java`
- `src/main/java/com/nexusfin/equity/thirdparty/qw/AllinpayDirectSkeletonResponseParser.java`

### Files to Read During Implementation

- `docs/plan/20260427_重构降摩擦.md`
- `src/main/java/com/nexusfin/equity/thirdparty/qw/AllinpayDirectInvocation.java`
- `src/main/java/com/nexusfin/equity/thirdparty/qw/AllinpayDirectPreparedRequest.java`
- `src/main/java/com/nexusfin/equity/thirdparty/qw/AllinpayDirectTransportRequest.java`
- `src/main/java/com/nexusfin/equity/thirdparty/qw/AllinpayDirectResponseVerificationAdapter.java`
- `src/main/java/com/nexusfin/equity/thirdparty/qw/AllinpayDirectRestHttpExecutor.java`

### Explicitly In Scope

- merge request-side `Factory / PayloadMapperRegistry / EnvelopeFactory / RequestPreparer` into fewer components
- remove unused `Skeleton*` placeholder fan-out where there is no runtime branch justification
- split `QwProperties` into smaller config files while preserving the same root prefix
- update focused unit tests and Spring config tests to the new wiring

### Explicitly Out of Scope

- changing `QwBenefitClient` external API
- changing `BenefitOrderServiceImpl`, `RepaymentServiceImpl`, `YunkaLoanApplyCompensationExecutor`, or async compensation payload behavior
- changing `.gitignore`
- reworking QW business semantics or unsupported-operation policies
- touching `thirdparty/qw/demo/` unless it still exists

### Validation Commands

- `mvn -Dtest=AllinpayDirectRequestBuilderTest,AllinpayDirectUnsupportedProtocolHandlerTest,AllinpayDirectConfigurationTest test`
- `mvn -Dtest=QwBenefitClientImplTest,AllinpayDirectQwBenefitClientTest,QwPropertiesTest,QwPayProtocolOverrideGuardTest,PaymentProtocolServiceTest test`
- `mvn -q -DskipTests compile`
- `find src/main/java/com/nexusfin/equity/thirdparty/qw -maxdepth 1 -name 'Allinpay*' | wc -l`
- `wc -l src/main/java/com/nexusfin/equity/config/QwProperties.java`

---

### Task 1: Freeze the New QW Request-Building Boundary in Tests

**Files:**
- Create: `src/test/java/com/nexusfin/equity/thirdparty/qw/AllinpayDirectRequestBuilderTest.java`
- Create: `src/test/java/com/nexusfin/equity/thirdparty/qw/AllinpayDirectUnsupportedProtocolHandlerTest.java`
- Modify: `src/test/java/com/nexusfin/equity/thirdparty/qw/AllinpayDirectConfigurationTest.java`
- Test: `mvn -Dtest=AllinpayDirectRequestBuilderTest,AllinpayDirectUnsupportedProtocolHandlerTest,AllinpayDirectConfigurationTest test`

- [ ] **Step 1: Add a focused request-builder test first**

Create `AllinpayDirectRequestBuilderTest` with coverage for the merged request-side behavior:

```java
class AllinpayDirectRequestBuilderTest {

    @Test
    void shouldBuildSignedMemberSyncPreparedRequest() {
        QwProperties properties = directProperties();
        AllinpayDirectRequestBuilder builder = new AllinpayDirectRequestBuilder(
                properties,
                new ObjectMapper(),
                signerThatReturns("signed-payload")
        );

        AllinpayDirectPreparedRequest prepared = builder.prepareMemberSync(new QwMemberSyncRequest(
                "ord-001",
                "uid-001",
                "Alice",
                "13800000000",
                "IDCARD",
                "3301",
                "6222"
        ));

        assertThat(prepared.targetUri().toString()).isEqualTo("https://tlt-test.allinpay.com/aipg/ProcessServlet");
        assertThat(prepared.signature()).isEqualTo("signed-payload");
        assertThat(prepared.requestBody()).contains("\"serviceCode\":\"SYNC001\"");
    }

    @Test
    void shouldRejectMissingDirectMerchantId() {
        QwProperties properties = directProperties();
        properties.getDirect().setMerchantId("");
        AllinpayDirectRequestBuilder builder = new AllinpayDirectRequestBuilder(
                properties,
                new ObjectMapper(),
                signerThatReturns("ignored")
        );

        assertThatThrownBy(() -> builder.prepareMemberSync(new QwMemberSyncRequest(
                "ord-001", "uid-001", "Alice", "13800000000", "IDCARD", "3301", "6222"
        )))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("direct.merchantId");
    }
}
```

- [ ] **Step 2: Add a focused unsupported-protocol handler test**

Create `AllinpayDirectUnsupportedProtocolHandlerTest` to freeze the placeholder behavior after deleting `Skeleton*` classes:

```java
class AllinpayDirectUnsupportedProtocolHandlerTest {

    private final AllinpayDirectUnsupportedProtocolHandler handler =
            new AllinpayDirectUnsupportedProtocolHandler();

    @Test
    void shouldThrowForTransportExecution() {
        assertThatThrownBy(() -> handler.execute(new AllinpayDirectTransportRequest(
                URI.create("https://tlt-test.allinpay.com/aipg/ProcessServlet"),
                HttpMethod.POST,
                MediaType.APPLICATION_JSON,
                "{}",
                Map.of(),
                Map.of()
        )))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("transport is not implemented");
    }

    @Test
    void shouldThrowForResponseParsing() {
        assertThatThrownBy(() -> handler.parse(
                AllinpayDirectOperation.MEMBER_SYNC,
                "SYNC001",
                new AllinpayDirectVerifiedResponse(200, "{\"ok\":true}", "sig"),
                QwMemberSyncResponse.class
        ))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("response parsing is not implemented");
    }
}
```

- [ ] **Step 3: Update the Spring config test to expect the new beans and watch it fail**

Adjust `AllinpayDirectConfigurationTest` so the default beans are `AllinpayDirectRequestBuilder` plus `AllinpayDirectUnsupportedProtocolHandler` instead of the deleted request factory / skeleton trio, then run:

```bash
mvn -Dtest=AllinpayDirectRequestBuilderTest,AllinpayDirectUnsupportedProtocolHandlerTest,AllinpayDirectConfigurationTest test
```

Expected:

- compilation or test failure because `AllinpayDirectRequestBuilder` and `AllinpayDirectUnsupportedProtocolHandler` do not exist yet

- [ ] **Step 4: Commit the red tests**

```bash
git add src/test/java/com/nexusfin/equity/thirdparty/qw/AllinpayDirectRequestBuilderTest.java \
        src/test/java/com/nexusfin/equity/thirdparty/qw/AllinpayDirectUnsupportedProtocolHandlerTest.java \
        src/test/java/com/nexusfin/equity/thirdparty/qw/AllinpayDirectConfigurationTest.java
git commit -m "test: freeze qw convergence request and protocol seams"
```

---

### Task 2: Merge Allinpay Direct Request-Side Builders

**Files:**
- Create: `src/main/java/com/nexusfin/equity/thirdparty/qw/AllinpayDirectRequestBuilder.java`
- Modify: `src/main/java/com/nexusfin/equity/thirdparty/qw/AllinpayDirectQwBenefitClient.java`
- Modify: `src/main/java/com/nexusfin/equity/config/AllinpayDirectConfiguration.java`
- Delete: `src/main/java/com/nexusfin/equity/thirdparty/qw/AllinpayDirectRequestFactory.java`
- Delete: `src/main/java/com/nexusfin/equity/thirdparty/qw/AllinpayDirectPayloadMapperRegistry.java`
- Delete: `src/main/java/com/nexusfin/equity/thirdparty/qw/AllinpayDirectEnvelopeFactory.java`
- Delete: `src/main/java/com/nexusfin/equity/thirdparty/qw/AllinpayDirectRequestPreparer.java`
- Test: `mvn -Dtest=AllinpayDirectRequestBuilderTest,AllinpayDirectQwBenefitClientTest,AllinpayDirectConfigurationTest test`

- [ ] **Step 1: Implement the merged request builder with the old four responsibilities**

Create `AllinpayDirectRequestBuilder` with:

```java
public class AllinpayDirectRequestBuilder {

    private final QwProperties properties;
    private final ObjectMapper objectMapper;
    private final AllinpayRequestSigner requestSigner;
    private final List<AllinpayDirectPayloadMapper<?>> mappers;

    public AllinpayDirectRequestBuilder(
            QwProperties properties,
            ObjectMapper objectMapper,
            AllinpayRequestSigner requestSigner,
            AllinpayDirectPayloadMapper<?>... mappers
    ) { ... }

    public AllinpayDirectPreparedRequest prepareMemberSync(QwMemberSyncRequest request) { ... }
    public AllinpayDirectPreparedRequest prepareExerciseUrl(QwExerciseUrlRequest request) { ... }
    public AllinpayDirectPreparedRequest prepareLendingNotify(QwLendingNotifyRequest request) { ... }
}
```

Inside it, move:

- invocation building from `AllinpayDirectRequestFactory`
- payload resolution from `AllinpayDirectPayloadMapperRegistry`
- envelope creation from `AllinpayDirectEnvelopeFactory`
- signed prepared request creation from `AllinpayDirectRequestPreparer`

- [ ] **Step 2: Switch `AllinpayDirectQwBenefitClient` to the new builder**

Update constructor dependencies from:

```java
AllinpayDirectRequestFactory requestFactory,
AllinpayDirectPayloadMapperRegistry payloadMapperRegistry,
AllinpayDirectEnvelopeFactory envelopeFactory,
```

to:

```java
AllinpayDirectRequestBuilder requestBuilder,
```

Then simplify each request path:

```java
AllinpayDirectPreparedRequest preparedRequest = requestBuilder.prepareMemberSync(request);
return execute(AllinpayDirectOperation.MEMBER_SYNC, "memberSync", preparedRequest, QwMemberSyncResponse.class);
```

Preserve:

- `ensureReady(...)`
- current direct certificate loading
- current unsupported sign operations
- current protocol-boundary enrichment for unimplemented direct flow

- [ ] **Step 3: Replace configuration wiring and delete the four old classes**

`AllinpayDirectConfiguration` should now provide:

```java
@Bean
@ConditionalOnMissingBean(AllinpayDirectRequestBuilder.class)
public AllinpayDirectRequestBuilder allinpayDirectRequestBuilder(
        QwProperties properties,
        ObjectMapper objectMapper
) {
    return new AllinpayDirectRequestBuilder(
            properties,
            objectMapper,
            new AllinpayRequestSigner(KeyStore.getInstance(KeyStore.getDefaultType()), "unused"),
            new AllinpayMemberSyncPayloadMapper(),
            new AllinpayExerciseUrlPayloadMapper(),
            new AllinpayLendingNotifyPayloadMapper()
    );
}
```

Implementation note:

- production `AllinpayDirectQwBenefitClient` still creates the real signer after certificates load; the Spring default builder bean only needs to exist for wiring/tests and can expose a constructor variant that defers signer injection until `prepare(...)`
- if a deferred signer supplier is cleaner than a placeholder signer, prefer `Supplier<AllinpayRequestSigner>` in `AllinpayDirectRequestBuilder`

- [ ] **Step 4: Run focused tests until green**

```bash
mvn -Dtest=AllinpayDirectRequestBuilderTest,AllinpayDirectQwBenefitClientTest,AllinpayDirectConfigurationTest test
```

Expected:

- `BUILD SUCCESS`

- [ ] **Step 5: Commit the request-side merge**

```bash
git add src/main/java/com/nexusfin/equity/thirdparty/qw/AllinpayDirectRequestBuilder.java \
        src/main/java/com/nexusfin/equity/thirdparty/qw/AllinpayDirectQwBenefitClient.java \
        src/main/java/com/nexusfin/equity/config/AllinpayDirectConfiguration.java \
        src/test/java/com/nexusfin/equity/thirdparty/qw/AllinpayDirectRequestBuilderTest.java \
        src/test/java/com/nexusfin/equity/thirdparty/qw/AllinpayDirectQwBenefitClientTest.java \
        src/test/java/com/nexusfin/equity/thirdparty/qw/AllinpayDirectConfigurationTest.java
git add -u src/main/java/com/nexusfin/equity/thirdparty/qw
git commit -m "refactor: merge allinpay direct request builders"
```

---

### Task 3: Remove `Skeleton*` Placeholder Fan-Out

**Files:**
- Create: `src/main/java/com/nexusfin/equity/thirdparty/qw/AllinpayDirectUnsupportedProtocolHandler.java`
- Modify: `src/main/java/com/nexusfin/equity/config/AllinpayDirectConfiguration.java`
- Modify: `src/test/java/com/nexusfin/equity/thirdparty/qw/AllinpayDirectConfigurationTest.java`
- Delete: `src/main/java/com/nexusfin/equity/thirdparty/qw/AllinpayDirectSkeletonHttpExecutor.java`
- Delete: `src/main/java/com/nexusfin/equity/thirdparty/qw/AllinpayDirectSkeletonResponseVerificationStage.java`
- Delete: `src/main/java/com/nexusfin/equity/thirdparty/qw/AllinpayDirectSkeletonResponseParser.java`
- Test: `mvn -Dtest=AllinpayDirectUnsupportedProtocolHandlerTest,AllinpayDirectConfigurationTest,AllinpayDirectQwBenefitClientTest test`

- [ ] **Step 1: Implement one unsupported handler for the three default placeholder roles**

```java
public class AllinpayDirectUnsupportedProtocolHandler
        implements AllinpayDirectHttpExecutor, AllinpayDirectResponseVerificationStage, AllinpayDirectResponseParser {

    @Override
    public AllinpayDirectRawResponse execute(AllinpayDirectTransportRequest transportRequest) {
        throw new BizException(
                "ALLINPAY_DIRECT_PROTOCOL_UNIMPLEMENTED",
                "Allinpay direct transport is not implemented for targetUri=" + transportRequest.targetUri()
        );
    }

    @Override
    public AllinpayDirectVerifiedResponse verify(AllinpayDirectRawResponse rawResponse) {
        throw new BizException(
                "ALLINPAY_DIRECT_PROTOCOL_UNIMPLEMENTED",
                "Allinpay direct response verification is not implemented"
        );
    }

    @Override
    public <T> T parse(...) {
        throw new BizException(
                "ALLINPAY_DIRECT_PROTOCOL_UNIMPLEMENTED",
                "Allinpay direct response parsing is not implemented for " + operation + " with serviceCode=" + serviceCode
        );
    }
}
```

- [ ] **Step 2: Wire the same instance into all three default beans**

In `AllinpayDirectConfiguration`, register:

```java
@Bean
@ConditionalOnMissingBean({
        AllinpayDirectHttpExecutor.class,
        AllinpayDirectResponseVerificationStage.class,
        AllinpayDirectResponseParser.class
})
public AllinpayDirectUnsupportedProtocolHandler allinpayDirectUnsupportedProtocolHandler() {
    return new AllinpayDirectUnsupportedProtocolHandler();
}

@Bean
@ConditionalOnMissingBean(AllinpayDirectHttpExecutor.class)
public AllinpayDirectHttpExecutor allinpayDirectHttpExecutor(AllinpayDirectUnsupportedProtocolHandler handler) {
    return handler;
}
```

Repeat the same pattern for `AllinpayDirectResponseVerificationStage` and `AllinpayDirectResponseParser`.

- [ ] **Step 3: Run the focused tests and keep behavior green**

```bash
mvn -Dtest=AllinpayDirectUnsupportedProtocolHandlerTest,AllinpayDirectConfigurationTest,AllinpayDirectQwBenefitClientTest test
```

Expected:

- `BUILD SUCCESS`

- [ ] **Step 4: Commit the skeleton collapse**

```bash
git add src/main/java/com/nexusfin/equity/thirdparty/qw/AllinpayDirectUnsupportedProtocolHandler.java \
        src/main/java/com/nexusfin/equity/config/AllinpayDirectConfiguration.java \
        src/test/java/com/nexusfin/equity/thirdparty/qw/AllinpayDirectUnsupportedProtocolHandlerTest.java \
        src/test/java/com/nexusfin/equity/thirdparty/qw/AllinpayDirectConfigurationTest.java
git add -u src/main/java/com/nexusfin/equity/thirdparty/qw
git commit -m "refactor: collapse allinpay direct unsupported handlers"
```

---

### Task 4: Split `QwProperties` into Smaller Subdomain Config Classes

**Files:**
- Create: `src/main/java/com/nexusfin/equity/config/QwHttpProperties.java`
- Create: `src/main/java/com/nexusfin/equity/config/QwSecurityProperties.java`
- Create: `src/main/java/com/nexusfin/equity/config/QwPaymentProperties.java`
- Create: `src/main/java/com/nexusfin/equity/config/QwDirectProperties.java`
- Modify: `src/main/java/com/nexusfin/equity/config/QwProperties.java`
- Modify: `src/main/java/com/nexusfin/equity/thirdparty/qw/QwBenefitClientImpl.java`
- Modify: `src/main/java/com/nexusfin/equity/service/impl/PaymentProtocolServiceImpl.java`
- Modify: `src/main/java/com/nexusfin/equity/config/QwPayProtocolOverrideGuard.java`
- Modify: `src/test/java/com/nexusfin/equity/thirdparty/qw/QwPropertiesTest.java`
- Modify: `src/test/java/com/nexusfin/equity/thirdparty/qw/QwBenefitClientImplTest.java`
- Modify: `src/test/java/com/nexusfin/equity/config/QwPayProtocolOverrideGuardTest.java`
- Modify: `src/test/java/com/nexusfin/equity/service/PaymentProtocolServiceTest.java`
- Test: `mvn -Dtest=QwBenefitClientImplTest,QwPropertiesTest,QwPayProtocolOverrideGuardTest,PaymentProtocolServiceTest test`

- [ ] **Step 1: Write the property-split test changes first**

Update `QwPropertiesTest` to use the new shape:

```java
QwProperties properties = new QwProperties();
properties.getHttp().setBaseUrl("https://t-api.test.qweimobile.com");
properties.getSecurity().setAesKey("FbRW7iaiwcEKk2kY");
properties.getSecurity().setAesKeyEncoding(QwProperties.AesKeyEncoding.RAW);
properties.getPayment().setAllowMemberSyncPayProtocolNoOverride(true);
properties.getDirect().setBaseUrl("https://tlt-test.allinpay.com");

assertThat(properties.getHttp().getBaseUrl()).isEqualTo("https://t-api.test.qweimobile.com");
assertThat(properties.getSecurity().getAesKey()).isEqualTo("FbRW7iaiwcEKk2kY");
assertThat(properties.getPayment().isAllowMemberSyncPayProtocolNoOverride()).isTrue();
```

Update the other focused tests similarly so the red state is “new nested getters do not exist yet”.

- [ ] **Step 2: Implement the four new subdomain classes and shrink `QwProperties`**

Use this root shape:

```java
@ConfigurationProperties(prefix = "nexusfin.third-party.qw")
public class QwProperties {

    private boolean enabled = true;
    private Mode mode = Mode.MOCK;
    private final QwHttpProperties http = new QwHttpProperties();
    private final QwSecurityProperties security = new QwSecurityProperties();
    private final QwPaymentProperties payment = new QwPaymentProperties();
    private final QwDirectProperties direct = new QwDirectProperties();

    public QwHttpProperties getHttp() { return http; }
    public QwSecurityProperties getSecurity() { return security; }
    public QwPaymentProperties getPayment() { return payment; }
    public QwDirectProperties getDirect() { return direct; }
}
```

Move fields:

- `baseUrl / methodPath / connectTimeoutMs / readTimeoutMs / mockExerciseBaseUrl` -> `QwHttpProperties`
- `signKey / aesKey / aesKeyBase64 / aesKeyEncoding / aesAlgorithm / ciphertextEncoding / gcmTagBits / ivLengthBytes` -> `QwSecurityProperties`
- `defaultPayProtocolPrefix / memberSyncPayProtocolNoOverride / allowMemberSyncPayProtocolNoOverride / memberSyncPayProtocolNoOverrideAllowedProfiles` -> `QwPaymentProperties`
- current inner `Direct` -> `QwDirectProperties`

- [ ] **Step 3: Update production call sites to the nested accessors**

Examples:

```java
URI.create(qwProperties.getHttp().getBaseUrl() + qwProperties.getHttp().getMethodPath())
```

```java
String configuredOverride = qwProperties.getPayment().getMemberSyncPayProtocolNoOverride();
if (qwProperties.getPayment().isAllowMemberSyncPayProtocolNoOverride()) { ... }
```

```java
Cipher cipher = Cipher.getInstance(qwProperties.getSecurity().getAesAlgorithm());
```

- [ ] **Step 4: Run the focused property/config tests until green**

```bash
mvn -Dtest=QwBenefitClientImplTest,QwPropertiesTest,QwPayProtocolOverrideGuardTest,PaymentProtocolServiceTest test
```

Expected:

- `BUILD SUCCESS`

- [ ] **Step 5: Commit the property split**

```bash
git add src/main/java/com/nexusfin/equity/config/QwHttpProperties.java \
        src/main/java/com/nexusfin/equity/config/QwSecurityProperties.java \
        src/main/java/com/nexusfin/equity/config/QwPaymentProperties.java \
        src/main/java/com/nexusfin/equity/config/QwDirectProperties.java \
        src/main/java/com/nexusfin/equity/config/QwProperties.java \
        src/main/java/com/nexusfin/equity/thirdparty/qw/QwBenefitClientImpl.java \
        src/main/java/com/nexusfin/equity/service/impl/PaymentProtocolServiceImpl.java \
        src/main/java/com/nexusfin/equity/config/QwPayProtocolOverrideGuard.java \
        src/test/java/com/nexusfin/equity/thirdparty/qw/QwPropertiesTest.java \
        src/test/java/com/nexusfin/equity/thirdparty/qw/QwBenefitClientImplTest.java \
        src/test/java/com/nexusfin/equity/config/QwPayProtocolOverrideGuardTest.java \
        src/test/java/com/nexusfin/equity/service/PaymentProtocolServiceTest.java
git commit -m "refactor: split qw properties by subdomain"
```

---

### Task 5: Run Focused QW Regression and Structural Checks

**Files:**
- Modify: none unless verification exposes a concrete QW-only bug
- Test: all commands below

- [ ] **Step 1: Run the focused QW suite**

```bash
mvn -Dtest=AllinpayDirectRequestBuilderTest,AllinpayDirectUnsupportedProtocolHandlerTest,AllinpayDirectConfigurationTest,QwBenefitClientImplTest,AllinpayDirectQwBenefitClientTest,QwPropertiesTest,QwPayProtocolOverrideGuardTest,PaymentProtocolServiceTest,QwBenefitClientTest,RoutingQwBenefitClientTest test
```

Expected:

- `BUILD SUCCESS`

- [ ] **Step 2: Run compile verification**

```bash
mvn -q -DskipTests compile
```

Expected:

- exit code `0`

- [ ] **Step 3: Prove the structure actually shrank**

Run:

```bash
find src/main/java/com/nexusfin/equity/thirdparty/qw -maxdepth 1 -name 'Allinpay*' | wc -l
wc -l src/main/java/com/nexusfin/equity/config/QwProperties.java
rg -n "Skeleton" src/main/java/com/nexusfin/equity/thirdparty/qw src/test/java/com/nexusfin/equity/thirdparty/qw
```

Expected:

- `Allinpay*` count lower than `37`
- `QwProperties.java` materially smaller than `359` lines
- no remaining runtime-relevant `Skeleton*` class references under QW main code

- [ ] **Step 4: Commit only if verification forced a QW-only fix**

```bash
git add <only-qw-fix-files>
git commit -m "fix: stabilize qw convergence verification"
```

Default:

- no new commit
