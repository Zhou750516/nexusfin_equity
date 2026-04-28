# Phase 5 Test Context Convergence Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reduce Spring test context flavors and verification cost by converting low-risk controller integration tests to thinner controller slices, explicitly stabilizing JUnit platform behavior, and preserving current API behavior.

**Architecture:** Keep the current `AbstractIntegrationTest`/full-context tests for flows that genuinely need application wiring, MySQL profile switching, or cross-layer behavior. For the thinnest controller-only cases, prefer standalone `MockMvcBuilders.standaloneSetup(...)` slices over `@WebMvcTest` when the project-wide `@MapperScan`/MVC config makes Boot slices drag in unrelated infrastructure. Add explicit JUnit platform configuration so test execution mode is stable instead of implicit.

**Tech Stack:** Java 17, Spring Boot 3.2, JUnit 5, MockMvc, Mockito, Maven

---

## Baseline

- Baseline capture date: `2026-04-28`
- Current `@SpringBootTest` count: `18`
- Current slice test count:
  - `@WebMvcTest`: `0`
  - `@DataJpaTest` / similar: `0`
- Current `mvn test` wall-clock: `14.544s`
- Current test count under `mvn test`: `214`
- Current Spring test family groups:
  1. default profile, full context, no `@MockBean`
  2. default profile, full context, single-service mock controller tests
  3. default profile, full context, multi-mock feature tests
  4. `mysql-it` profile, full context, no `@MockBean`
  5. `mysql-it` profile, full context, `AsyncCompensationRouterExecutor` mocked

## File Structure

### Files to Create

- `src/test/resources/junit-platform.properties`

### Files to Modify

- `src/test/java/com/nexusfin/equity/controller/RefundControllerIntegrationTest.java`
- `src/test/java/com/nexusfin/equity/controller/BenefitDispatchControllerIntegrationTest.java`
- `src/test/java/com/nexusfin/equity/controller/JointLoginControllerIntegrationTest.java`

### Files to Read During Implementation

- `docs/test-performance-analysis.md`
- `src/test/java/com/nexusfin/equity/support/AbstractIntegrationTest.java`
- `src/main/java/com/nexusfin/equity/controller/RefundController.java`
- `src/main/java/com/nexusfin/equity/controller/BenefitDispatchController.java`
- `src/main/java/com/nexusfin/equity/controller/JointLoginController.java`
- `src/main/java/com/nexusfin/equity/config/JwtAuthenticationFilter.java`
- `src/test/java/com/nexusfin/equity/config/JwtAuthenticationFilterTest.java`

### Explicitly In Scope

- stabilize JUnit platform execution mode with explicit config
- convert low-risk controller tests from full context to standalone MockMvc controller slices
- reduce `@SpringBootTest` count by targeting controller-only tests first
- preserve existing controller HTTP contract, validation, and auth-cookie behavior in tests

### Explicitly Out of Scope

- modifying production controller/service code
- changing MySQL IT semantics or repository tests
- changing already focused service unit tests
- introducing parallel execution that could destabilize the suite
- reorganizing unrelated test packages

### Validation Commands

- `rg -n "@SpringBootTest" src/test/java | wc -l`
- `rg -n "@WebMvcTest" src/test/java | wc -l`
- `rg -n "standaloneSetup" src/test/java | wc -l`
- `mvn -Dtest=RefundControllerIntegrationTest,BenefitDispatchControllerIntegrationTest,JointLoginControllerIntegrationTest test`
- `mvn test`

---

### Task 1: Lock Baseline and Explicit JUnit Platform Defaults

**Files:**
- Create: `src/test/resources/junit-platform.properties`
- Test: none

- [ ] **Step 1: Create explicit JUnit platform configuration**

Create `src/test/resources/junit-platform.properties` with:

```properties
junit.jupiter.execution.parallel.enabled=false
junit.jupiter.testinstance.lifecycle.default=per_method
```

- [ ] **Step 2: Verify the file exists and is minimal**

Run:

```bash
sed -n '1,40p' src/test/resources/junit-platform.properties
```

Expected:

- only the two lines above

- [ ] **Step 3: Commit the baseline/stability config**

```bash
git add src/test/resources/junit-platform.properties
git commit -m "test: pin junit platform execution defaults"
```

---

### Task 2: Convert RefundControllerIntegrationTest to a Standalone Controller Slice

**Files:**
- Modify: `src/test/java/com/nexusfin/equity/controller/RefundControllerIntegrationTest.java`
- Test: `mvn -Dtest=RefundControllerIntegrationTest test`

- [ ] **Step 1: Rewrite the test to standalone `MockMvc`**

Use this structure:

```java
@ExtendWith(MockitoExtension.class)
class RefundControllerIntegrationTest {

    @Mock
    private RefundService refundService;

    private MockMvc mockMvc;
    private JwtUtil jwtUtil;
}
```

In `@BeforeEach`, build:

```java
AuthProperties authProperties = authProperties("/api/refund");
jwtUtil = new JwtUtil(authProperties);
JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter(
        authProperties,
        jwtUtil,
        new ObjectMapper()
);
LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
validator.afterPropertiesSet();
mockMvc = MockMvcBuilders
        .standaloneSetup(new RefundController(refundService))
        .addFilters(jwtAuthenticationFilter)
        .setControllerAdvice(new GlobalExceptionHandler())
        .setMessageConverters(new MappingJackson2HttpMessageConverter(new ObjectMapper()))
        .setValidator(validator)
        .build();
```

- [ ] **Step 2: Run the single test and keep behavior green**

Run:

```bash
mvn -Dtest=RefundControllerIntegrationTest test
```

Expected:

- `BUILD SUCCESS`

- [ ] **Step 3: Commit the slice conversion**

```bash
git add src/test/java/com/nexusfin/equity/controller/RefundControllerIntegrationTest.java
git commit -m "test: convert refund controller test to standalone slice"
```

---

### Task 3: Convert BenefitDispatchControllerIntegrationTest to a Standalone Controller Slice

**Files:**
- Modify: `src/test/java/com/nexusfin/equity/controller/BenefitDispatchControllerIntegrationTest.java`
- Test: `mvn -Dtest=BenefitDispatchControllerIntegrationTest test`

- [ ] **Step 1: Rewrite the test to standalone `MockMvc`**

Use the same support shape as Refund, but instantiate `BenefitDispatchController`:

```java
@ExtendWith(MockitoExtension.class)
class BenefitDispatchControllerIntegrationTest {
```

Required mocks:

```java
@Mock private BenefitDispatchService benefitDispatchService;
```

`@BeforeEach` auth setup:

```java
AuthProperties authProperties = authProperties("/api/benefit-dispatch");
jwtUtil = new JwtUtil(authProperties);
JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter(
        authProperties,
        jwtUtil,
        new ObjectMapper()
);
mockMvc = MockMvcBuilders
        .standaloneSetup(new BenefitDispatchController(benefitDispatchService))
        .addFilters(jwtAuthenticationFilter)
        .setControllerAdvice(new GlobalExceptionHandler())
        .setMessageConverters(new MappingJackson2HttpMessageConverter(new ObjectMapper()))
        .setValidator(validator)
        .build();
```

- [ ] **Step 2: Run the single test**

Run:

```bash
mvn -Dtest=BenefitDispatchControllerIntegrationTest test
```

Expected:

- `BUILD SUCCESS`

- [ ] **Step 3: Commit the slice conversion**

```bash
git add src/test/java/com/nexusfin/equity/controller/BenefitDispatchControllerIntegrationTest.java
git commit -m "test: convert benefit dispatch controller test to standalone slice"
```

---

### Task 4: Convert JointLoginControllerIntegrationTest to a Standalone Controller Slice

**Files:**
- Modify: `src/test/java/com/nexusfin/equity/controller/JointLoginControllerIntegrationTest.java`
- Test: `mvn -Dtest=JointLoginControllerIntegrationTest test`

- [ ] **Step 1: Rewrite the test to standalone `MockMvc`**

Use this structure:

```java
@ExtendWith(MockitoExtension.class)
class JointLoginControllerIntegrationTest {

    @Mock
    private JointLoginService jointLoginService;

    private MockMvc mockMvc;
}
```

`@BeforeEach` config setup:

```java
AuthProperties authProperties = authProperties();
mockMvc = MockMvcBuilders
        .standaloneSetup(new JointLoginController(
                jointLoginService,
                new CookieUtil(authProperties)
        ))
        .setControllerAdvice(new GlobalExceptionHandler())
        .setMessageConverters(new MappingJackson2HttpMessageConverter(new ObjectMapper()))
        .setValidator(validator)
        .build();
```

No JWT filter is needed here because `/api/auth/joint-login` stays unauthenticated and the test only checks response/body/cookie writing plus request validation.

- [ ] **Step 2: Run the single test**

Run:

```bash
mvn -Dtest=JointLoginControllerIntegrationTest test
```

Expected:

- `BUILD SUCCESS`

- [ ] **Step 3: Commit the slice conversion**

```bash
git add src/test/java/com/nexusfin/equity/controller/JointLoginControllerIntegrationTest.java
git commit -m "test: convert joint login controller test to web slice"
```

---

### Task 5: Run Focused Regression and Full Suite

**Files:**
- Verify only
- Test:
  - `mvn -Dtest=RefundControllerIntegrationTest,BenefitDispatchControllerIntegrationTest,JointLoginControllerIntegrationTest test`
  - `mvn test`

- [ ] **Step 1: Run focused controller regression**

```bash
mvn -Dtest=RefundControllerIntegrationTest,BenefitDispatchControllerIntegrationTest,JointLoginControllerIntegrationTest test
```

Expected:

- `BUILD SUCCESS`

- [ ] **Step 2: Run full Java test suite**

```bash
mvn test
```

Expected:

- `BUILD SUCCESS`

- [ ] **Step 3: Measure after-state**

Run:

```bash
rg -n "@SpringBootTest" src/test/java | wc -l
rg -n "@WebMvcTest" src/test/java | wc -l
```

Expected:

- `@SpringBootTest` count drops from `18` to `15`
- `@WebMvcTest` count increases from `0` to `3`

- [ ] **Step 4: No extra commit for verification-only task**

Only create a new commit if a plan-local fix is required during verification.
