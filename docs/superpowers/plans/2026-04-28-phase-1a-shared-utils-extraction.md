# Phase 1A Shared Utils Extraction Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extract the duplicated money-conversion and ID-generation helpers from `LoanServiceImpl` and `RepaymentServiceImpl` into shared utility classes without changing business behavior.

**Architecture:** This plan intentionally slices `Phase 1` into the lowest-risk backend cross-cutting refactor first: shared utility extraction only. It introduces `MoneyUnits` and `BizIds` with focused unit tests, then rewires `LoanServiceImpl` and `RepaymentServiceImpl` to consume those utilities and deletes their duplicated private helpers. The broader `YunkaCallTemplate` and gateway-log unification stays out of this slice and should be planned separately after these two utilities land.

**Tech Stack:** Java 17, Spring Boot 3.2, JUnit 5, Mockito, Maven

---

## File Structure

### Files to Create

- `src/main/java/com/nexusfin/equity/util/MoneyUnits.java`
- `src/main/java/com/nexusfin/equity/util/BizIds.java`
- `src/test/java/com/nexusfin/equity/util/MoneyUnitsTest.java`
- `src/test/java/com/nexusfin/equity/util/BizIdsTest.java`

### Files to Modify

- `src/main/java/com/nexusfin/equity/service/impl/LoanServiceImpl.java`
- `src/main/java/com/nexusfin/equity/service/impl/RepaymentServiceImpl.java`

### Files to Read During Implementation

- `src/test/java/com/nexusfin/equity/service/LoanServiceTest.java`
- `src/test/java/com/nexusfin/equity/service/RepaymentServiceTest.java`
- `docs/plan/20260427_重构降摩擦.md`

### Explicitly Out of Scope for This Plan

- `src/main/java/com/nexusfin/equity/service/impl/XiaohuaGatewayServiceImpl.java`
- `src/main/java/com/nexusfin/equity/service/impl/YunkaLoanApplyCompensationExecutor.java`
- any `YunkaCallTemplate` extraction
- any logging-AOP or interceptor work
- any service splitting beyond helper replacement

### Validation Commands

- `mvn -Dtest=MoneyUnitsTest test`
- `mvn -Dtest=BizIdsTest test`
- `mvn -Dtest=LoanServiceTest,RepaymentServiceTest,MoneyUnitsTest,BizIdsTest test`
- `mvn -q -DskipTests compile`

---

### Task 1: Add `MoneyUnits` and Replace Money Conversion Duplicates

**Files:**
- Create: `src/main/java/com/nexusfin/equity/util/MoneyUnits.java`
- Create: `src/test/java/com/nexusfin/equity/util/MoneyUnitsTest.java`
- Modify: `src/main/java/com/nexusfin/equity/service/impl/LoanServiceImpl.java`
- Modify: `src/main/java/com/nexusfin/equity/service/impl/RepaymentServiceImpl.java`
- Test: `mvn -Dtest=MoneyUnitsTest,LoanServiceTest,RepaymentServiceTest test`

- [ ] **Step 1: Write the failing unit test for `MoneyUnits`**

Create `src/test/java/com/nexusfin/equity/util/MoneyUnitsTest.java` with this exact content:

```java
package com.nexusfin.equity.util;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MoneyUnitsTest {

    @Test
    void shouldConvertLongYuanToCentExactly() {
        assertThat(MoneyUnits.yuanToCent(3000L)).isEqualTo(300000L);
    }

    @Test
    void shouldConvertDecimalYuanToCentWithHalfUpRounding() {
        assertThat(MoneyUnits.yuanToCent(new BigDecimal("1018.50"))).isEqualTo(101850L);
    }

    @Test
    void shouldConvertCentToYuanWithTwoDecimalPlaces() {
        assertThat(MoneyUnits.centsToYuan(104500L)).isEqualByComparingTo("1045.00");
    }
}
```

- [ ] **Step 2: Run the new test and confirm it fails because `MoneyUnits` does not exist yet**

Run:

```bash
mvn -Dtest=MoneyUnitsTest test
```

Expected: compilation fails with `cannot find symbol` for `MoneyUnits`.

- [ ] **Step 3: Implement `MoneyUnits`**

Create `src/main/java/com/nexusfin/equity/util/MoneyUnits.java` with this exact content:

```java
package com.nexusfin.equity.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class MoneyUnits {

    private static final BigDecimal CENTS_PER_YUAN = BigDecimal.valueOf(100L);

    private MoneyUnits() {
    }

    public static long yuanToCent(long yuanAmount) {
        return Math.multiplyExact(yuanAmount, 100L);
    }

    public static long yuanToCent(BigDecimal yuanAmount) {
        return yuanAmount.multiply(CENTS_PER_YUAN).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    public static BigDecimal centsToYuan(long cents) {
        return BigDecimal.valueOf(cents).divide(CENTS_PER_YUAN, 2, RoundingMode.UNNECESSARY);
    }
}
```

- [ ] **Step 4: Rewire `LoanServiceImpl` to use `MoneyUnits` and delete local conversion helpers**

Apply these edits to `src/main/java/com/nexusfin/equity/service/impl/LoanServiceImpl.java`:

1. Remove the file-level `CENTS_PER_YUAN` constant.
2. Add this import:

```java
import static com.nexusfin.equity.util.MoneyUnits.centsToYuan;
import static com.nexusfin.equity.util.MoneyUnits.yuanToCent;
```

3. Delete these private methods completely:

```java
private static Long yuanToCent(Long yuanAmount) {
    return Math.multiplyExact(yuanAmount, 100L);
}

private static BigDecimal centsToYuan(long cents) {
    return BigDecimal.valueOf(cents).divide(CENTS_PER_YUAN, 2, RoundingMode.UNNECESSARY);
}
```

4. Keep all existing call sites unchanged other than resolving through the static imports. The following lines should still compile with the same behavior:

```java
new LoanTrailForwardData(uid, requestId, yuanToCent(request.amount()), request.term())
```

```java
approved ? centsToYuan(data.path("loanAmount").asLong(0L)) : BigDecimal.ZERO
```

- [ ] **Step 5: Rewire `RepaymentServiceImpl` to use `MoneyUnits` and delete local conversion helpers**

Apply these edits to `src/main/java/com/nexusfin/equity/service/impl/RepaymentServiceImpl.java`:

1. Remove the file-level `CENTS_PER_YUAN` constant.
2. Add this import:

```java
import static com.nexusfin.equity.util.MoneyUnits.centsToYuan;
import static com.nexusfin.equity.util.MoneyUnits.yuanToCent;
```

3. Delete these private methods completely:

```java
private static Long yuanToCent(BigDecimal yuanAmount) {
    return yuanAmount.multiply(CENTS_PER_YUAN).setScale(0, RoundingMode.HALF_UP).longValueExact();
}

private static BigDecimal centsToYuan(long cents) {
    return BigDecimal.valueOf(cents).divide(CENTS_PER_YUAN, 2, RoundingMode.UNNECESSARY);
}
```

4. Keep all existing call sites behaviorally identical. These usages must still compile and behave the same:

```java
yuanToCent(request.amount())
```

```java
centsToYuan(readLong(data, "repayAmount", "amount"))
```

- [ ] **Step 6: Run focused tests for utility behavior and the two refactored services**

Run:

```bash
mvn -Dtest=MoneyUnitsTest,LoanServiceTest,RepaymentServiceTest test
```

Expected: all selected tests pass.

- [ ] **Step 7: Commit the `MoneyUnits` task**

```bash
git add src/main/java/com/nexusfin/equity/util/MoneyUnits.java \
  src/test/java/com/nexusfin/equity/util/MoneyUnitsTest.java \
  src/main/java/com/nexusfin/equity/service/impl/LoanServiceImpl.java \
  src/main/java/com/nexusfin/equity/service/impl/RepaymentServiceImpl.java
git commit -m "refactor: extract shared money conversion utilities"
```

---

### Task 2: Add `BizIds` and Replace Local ID Generation Duplicates

**Files:**
- Create: `src/main/java/com/nexusfin/equity/util/BizIds.java`
- Create: `src/test/java/com/nexusfin/equity/util/BizIdsTest.java`
- Modify: `src/main/java/com/nexusfin/equity/service/impl/LoanServiceImpl.java`
- Modify: `src/main/java/com/nexusfin/equity/service/impl/RepaymentServiceImpl.java`
- Test: `mvn -Dtest=BizIdsTest,LoanServiceTest,RepaymentServiceTest test`

- [ ] **Step 1: Write the failing unit test for `BizIds`**

Create `src/test/java/com/nexusfin/equity/util/BizIdsTest.java` with this exact content:

```java
package com.nexusfin.equity.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BizIdsTest {

    @Test
    void shouldGenerateCompactUuidWithoutHyphens() {
        String value = BizIds.newCompactUuid();

        assertThat(value).hasSize(32);
        assertThat(value).doesNotContain("-");
    }

    @Test
    void shouldGeneratePrefixedIds() {
        String requestId = BizIds.next("LA");

        assertThat(requestId).startsWith("LA-");
        assertThat(requestId.substring(3)).hasSize(32);
        assertThat(requestId.substring(3)).doesNotContain("-");
    }
}
```

- [ ] **Step 2: Run the new test and confirm it fails because `BizIds` does not exist yet**

Run:

```bash
mvn -Dtest=BizIdsTest test
```

Expected: compilation fails with `cannot find symbol` for `BizIds`.

- [ ] **Step 3: Implement `BizIds`**

Create `src/main/java/com/nexusfin/equity/util/BizIds.java` with this exact content:

```java
package com.nexusfin.equity.util;

import java.util.UUID;

public final class BizIds {

    private BizIds() {
    }

    public static String newCompactUuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static String next(String prefix) {
        return prefix + "-" + newCompactUuid();
    }
}
```

- [ ] **Step 4: Rewire `LoanServiceImpl` to use `BizIds` and delete the local UUID helper**

Apply these edits to `src/main/java/com/nexusfin/equity/service/impl/LoanServiceImpl.java`:

1. Add this import:

```java
import static com.nexusfin.equity.util.BizIds.newCompactUuid;
import static com.nexusfin.equity.util.BizIds.next;
```

2. Replace prefixed concatenations with `next(...)`:

```java
String requestId = next("LC");
```

```java
String applicationId = next("APP");
String loanId = next("LN");
String requestId = next("LA");
```

```java
String requestId = next("LQ");
```

```java
String requestId = next("LRP");
```

3. Delete the local helper completely:

```java
private static String newCompactUuid() {
    return UUID.randomUUID().toString().replace("-", "");
}
```

4. Leave any truly non-prefixed usage on `newCompactUuid()` only if there is no business prefix requirement. In the current file, every ID shown by the existing code should use `next(...)`.

- [ ] **Step 5: Rewire `RepaymentServiceImpl` to use `BizIds` and delete the local UUID helper**

Apply these edits to `src/main/java/com/nexusfin/equity/service/impl/RepaymentServiceImpl.java`:

1. Add this import:

```java
import static com.nexusfin.equity.util.BizIds.next;
```

2. Replace prefixed concatenations with `next(...)`:

```java
String requestId = next("RT");
```

```java
String requestId = next("RSS");
```

```java
String requestId = next("RSC");
```

```java
String requestId = next("RS");
```

```java
String requestId = next("RQ");
```

3. Replace the inline `"RUC-" + newCompactUuid()` with:

```java
next("RUC")
```

4. Delete the local helper completely:

```java
private static String newCompactUuid() {
    return UUID.randomUUID().toString().replace("-", "");
}
```

- [ ] **Step 6: Run focused tests for utility behavior and the two refactored services**

Run:

```bash
mvn -Dtest=BizIdsTest,LoanServiceTest,RepaymentServiceTest test
```

Expected: all selected tests pass.

- [ ] **Step 7: Commit the `BizIds` task**

```bash
git add src/main/java/com/nexusfin/equity/util/BizIds.java \
  src/test/java/com/nexusfin/equity/util/BizIdsTest.java \
  src/main/java/com/nexusfin/equity/service/impl/LoanServiceImpl.java \
  src/main/java/com/nexusfin/equity/service/impl/RepaymentServiceImpl.java
git commit -m "refactor: extract shared business id utilities"
```

---

### Task 3: Remove Dead Duplicates and Run Slice-Level Verification

**Files:**
- Modify: `src/main/java/com/nexusfin/equity/service/impl/LoanServiceImpl.java`
- Modify: `src/main/java/com/nexusfin/equity/service/impl/RepaymentServiceImpl.java`
- Test: `mvn -Dtest=MoneyUnitsTest,BizIdsTest,LoanServiceTest,RepaymentServiceTest test`

- [ ] **Step 1: Verify both service files no longer define the extracted helpers**

Run:

```bash
rg -n "private static String newCompactUuid|private static Long yuanToCent|private static BigDecimal centsToYuan|CENTS_PER_YUAN" \
  src/main/java/com/nexusfin/equity/service/impl/LoanServiceImpl.java \
  src/main/java/com/nexusfin/equity/service/impl/RepaymentServiceImpl.java
```

Expected: no matches.

- [ ] **Step 2: Verify the shared utility call sites are now the only money/id helper source in these two services**

Run:

```bash
rg -n "MoneyUnits|BizIds|next\\(|yuanToCent\\(|centsToYuan\\(" \
  src/main/java/com/nexusfin/equity/service/impl/LoanServiceImpl.java \
  src/main/java/com/nexusfin/equity/service/impl/RepaymentServiceImpl.java
```

Expected: helper usage resolves through `MoneyUnits` and `BizIds`, not through deleted private methods.

- [ ] **Step 3: Run slice-level regression tests**

Run:

```bash
mvn -Dtest=MoneyUnitsTest,BizIdsTest,LoanServiceTest,RepaymentServiceTest test
```

Expected: all selected tests pass.

- [ ] **Step 4: Run compile verification**

Run:

```bash
mvn -q -DskipTests compile
```

Expected: exit code `0`.

- [ ] **Step 5: Commit the verification cleanup**

```bash
git add src/main/java/com/nexusfin/equity/service/impl/LoanServiceImpl.java \
  src/main/java/com/nexusfin/equity/service/impl/RepaymentServiceImpl.java
git commit -m "refactor: remove duplicated helper methods from loan and repayment services"
```

---

## Spec Coverage Check

This plan covers the first executable slice of `Phase 1 · 后端横切抽取` from `docs/plan/20260427_重构降摩擦.md`:

1. `MoneyUnits` extraction -> Task 1
2. `BizIds` extraction -> Task 2
3. duplicate helper removal in `LoanServiceImpl` and `RepaymentServiceImpl` -> Tasks 1-3

This plan intentionally does **not** cover:

1. `YunkaCallTemplate` introduction
2. gateway log unification
3. `XiaohuaGatewayServiceImpl` or `YunkaLoanApplyCompensationExecutor` refactors
4. any service splitting or payload relocation

Those should be planned in a separate `Phase 1B` document after this shared-utils slice lands cleanly.
