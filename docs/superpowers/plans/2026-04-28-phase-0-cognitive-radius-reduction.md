# Phase 0 Cognitive Radius Reduction Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Complete Phase 0 of the cognitive-radius reduction initiative by shrinking active entrypoints, archiving stale plan/spec content, splitting `AGENTS.md`, and moving non-runtime demo sources out of default scan paths.

**Architecture:** This is a repository-structure refactor, not a business-code refactor. Execute it in six layers: baseline capture, `docs/plan` root slimming, `AGENTS` split, archive/noise relocation, AI ignore setup, and lightweight validation. The active refactor master plan `docs/plan/20260427_重构降摩擦.md` stays in the `docs/plan` root throughout Phase 0.

**Tech Stack:** Git, Markdown, Java 17 + Maven, React/TypeScript H5 workspace, ripgrep

---

## File Structure

### Files to Create

- `docs/plan/topics/重构降摩擦/20260428_phase0_baseline.md`
- `docs/plan/archive/202604/`
- `docs/_archive/`
- `docs/AGENTS_BACKEND.md`
- `docs/AGENTS_H5.md`
- `.codexignore`
- `.aiignore`

### Files to Modify

- `AGENTS.md`
- `docs/plan/README.md`
- `docs/plan/20260427_重构降摩擦.md`
- `docs/plan/CURRENT_STATE.md` if root navigation text is stale after moves
- `pom.xml`
- active docs returned by the residual `rg` scans in Tasks 4-6

### Files/Directories to Move

- historical `docs/plan` day plans and checklists into `docs/plan/archive/202604/`
- historical root-level topic docs from `docs/plan/` into `docs/plan/archive/202604/` or the existing `docs/plan/topics/*/` directories
- `specs/004-benefit-loan-flow/` -> `docs/_archive/specs-004/`
- `src/main/java/com/nexusfin/equity/thirdparty/qw/demo/` -> `docs/third-part/qw-demo/`

### Validation Commands

- `find docs/plan -maxdepth 1 -type f | sort`
- `wc -l AGENTS.md`
- `rg -n "specs/004-benefit-loan-flow|src/main/java/com/nexusfin/equity/thirdparty/qw/demo|thirdparty/qw/demo" docs specs src pom.xml AGENTS.md README.md`
- `mvn -q -DskipTests compile`
- `cd H5 && ./node_modules/.bin/tsc --noEmit`

---

### Task 1: Capture the Phase 0 Baseline

**Files:**
- Create: `docs/plan/topics/重构降摩擦/20260428_phase0_baseline.md`
- Modify: `docs/plan/20260427_重构降摩擦.md`
- Test: none

- [ ] **Step 1: Create the refactor topic directory**

Run:

```bash
mkdir -p docs/plan/topics/重构降摩擦
```

Expected: `docs/plan/topics/重构降摩擦/` exists.

- [ ] **Step 2: Create the baseline document with the measured pre-change values**

Create `docs/plan/topics/重构降摩擦/20260428_phase0_baseline.md` with this exact content:

```md
# 2026-04-28 Phase 0 Baseline

## 1. Entry Surface Baseline

- `docs/plan` root file count: `50`
- root `AGENTS.md` line count: `235`
- `specs/004-benefit-loan-flow/` present: `yes`
- `src/main/java/com/nexusfin/equity/thirdparty/qw/demo/` present: `yes`
- `.codexignore` present: `no`
- `.aiignore` present: `no`

## 2. Java/H5 Size Markers

- `src/main/java` file count: `309`
- `H5/client/src` file count: `124`
- `LoanServiceImpl` lines: `730`
- `RepaymentServiceImpl` lines: `499`
- `H5/client/src/pages/CalculatorPage.tsx` lines: `645`
- `H5/client/src/pages/ApprovalPendingPage.tsx` lines: `233`

## 3. Notes

- This file captures the repository surface before Phase 0 moves.
- Later phases should append delta snapshots instead of overwriting this baseline.
```

- [ ] **Step 3: Link the baseline from the master refactor plan**

Add this bullet under the Phase 0 section in `docs/plan/20260427_重构降摩擦.md`:

```md
- 基线记录：`docs/plan/topics/重构降摩擦/20260428_phase0_baseline.md`
```

- [ ] **Step 4: Verify the baseline file content**

Run:

```bash
sed -n '1,220p' docs/plan/topics/重构降摩擦/20260428_phase0_baseline.md
```

Expected: the file contains the exact values listed in Step 2.

- [ ] **Step 5: Commit the baseline task**

```bash
git add docs/plan/topics/重构降摩擦/20260428_phase0_baseline.md docs/plan/20260427_重构降摩擦.md
git commit -m "docs: add phase 0 baseline metrics"
```

---

### Task 2: Slim the `docs/plan` Root to Five Entry Files

**Files:**
- Create: `docs/plan/archive/202604/`
- Modify: `docs/plan/README.md`
- Modify: `docs/plan/CURRENT_STATE.md` if needed
- Move: root files listed below
- Test: root file listing

- [ ] **Step 1: Create the archive directory**

Run:

```bash
mkdir -p docs/plan/archive/202604
```

Expected: `docs/plan/archive/202604/` exists.

- [ ] **Step 2: Move historical daily plans and checklists out of the root**

Run:

```bash
git mv docs/plan/20260419.md docs/plan/archive/202604/
git mv docs/plan/20260419_checklist.md docs/plan/archive/202604/
git mv docs/plan/20260420.md docs/plan/archive/202604/
git mv docs/plan/20260420_checklist.md docs/plan/archive/202604/
git mv docs/plan/20260421.md docs/plan/archive/202604/
git mv docs/plan/20260421_checklist.md docs/plan/archive/202604/
git mv docs/plan/20260423.md docs/plan/archive/202604/
git mv docs/plan/20260423_checklist.md docs/plan/archive/202604/
git mv docs/plan/20260424.md docs/plan/archive/202604/
git mv docs/plan/20260424_checklist.md docs/plan/archive/202604/
git mv docs/plan/20260426.md docs/plan/archive/202604/
git mv docs/plan/20260426_checklist.md docs/plan/archive/202604/
git mv docs/plan/20260427.md docs/plan/archive/202604/
git mv docs/plan/20260427_checklist.md docs/plan/archive/202604/
```

Expected: only `20260428.md`, `20260428_checklist.md`, `README.md`, `CURRENT_STATE.md`, and `20260427_重构降摩擦.md` remain as day-plan style root files.

- [ ] **Step 3: Move root topical docs into their existing topic directories**

Run:

```bash
git mv docs/plan/20260419_异步补偿人工处理SQL与操作模板.md docs/plan/topics/异步补偿/
git mv docs/plan/20260419_异步补偿开发任务拆解_调度接入版.md docs/plan/topics/异步补偿/
git mv docs/plan/20260419_异步补偿自动调度接入建议方案.md docs/plan/topics/异步补偿/
git mv docs/plan/20260419_异步补偿运维与人工处理说明.md docs/plan/topics/异步补偿/
git mv docs/plan/20260420_项目阶段性TODO_checklist.md docs/plan/topics/项目阶段性/
git mv docs/plan/20260420_项目阶段性TODO清单.md docs/plan/topics/项目阶段性/
git mv docs/plan/20260421_H5到后端报文加密方案.md docs/plan/topics/H5/
git mv docs/plan/20260421_H5绑卡页接入后端接口说明.md docs/plan/topics/H5/
git mv docs/plan/20260421_小花科技会议纪要与行动计划.md docs/plan/topics/小花科技/
git mv docs/plan/20260421_齐为会员同步字段升级收口方案.md docs/plan/topics/齐为/
git mv docs/plan/20260421_齐为权益接口文档更新纪要.md docs/plan/topics/齐为/
git mv docs/plan/20260421_齐为签约后端实现结论.md docs/plan/topics/齐为/
git mv docs/plan/20260421_齐为签约后端开发任务拆解.md docs/plan/topics/齐为/
git mv docs/plan/20260421_齐为签约后端接入设计方案.md docs/plan/topics/齐为/
git mv docs/plan/20260423_小花云卡齐为联动会议纪要与行动计划.md docs/plan/topics/小花科技/
git mv docs/plan/20260424_小花4.22开发任务拆解.md docs/plan/topics/小花科技/
git mv docs/plan/20260424_小花4.22现有实现差异分析与开发计划.md docs/plan/topics/小花科技/
git mv docs/plan/20260426_H5自动埋点方案.md docs/plan/topics/H5/
git mv docs/plan/20260426_云卡状态查询与H5状态映射方案.md docs/plan/topics/云卡/
git mv docs/plan/20260426_用信新链路改造方案.md docs/plan/topics/云卡/
git mv docs/plan/20260427_H5前后端接口对接清单.md docs/plan/topics/H5/
git mv docs/plan/20260427_H5前后端接口开发任务拆解.md docs/plan/topics/H5/
```

Expected: the moved files no longer exist in the `docs/plan` root, and the active refactor plan `docs/plan/20260427_重构降摩擦.md` still stays in root.

- [ ] **Step 4: Move the remaining root history-only docs into the monthly archive**

Run:

```bash
git mv docs/plan/20260419_控制器级返回口径核对结论.md docs/plan/archive/202604/
git mv docs/plan/20260420_自动调度启用后观察与人工介入SOP.md docs/plan/archive/202604/
git mv docs/plan/20260420_自动调度运行口径与启用说明.md docs/plan/archive/202604/
git mv docs/plan/20260421_后端日志联调与排障检查清单.md docs/plan/archive/202604/
git mv docs/plan/20260421_外部确认项复核结果.md docs/plan/archive/202604/
git mv docs/plan/20260421_艾博生后端日志完备性Review计划.md docs/plan/archive/202604/
git mv docs/plan/20260421_艾博生后端日志方案开发任务拆解.md docs/plan/archive/202604/
git mv docs/plan/20260426_Shared Foundation第一批落地项.md docs/plan/archive/202604/
git mv docs/plan/20260426_外部待确认项与本侧可执行项拆分.md docs/plan/archive/202604/
```

Expected: no non-entrypoint history docs remain in the `docs/plan` root.

- [ ] **Step 5: Rewrite `docs/plan/README.md` as the live entry index**

Replace `docs/plan/README.md` with this exact content:

```md
# docs/plan 使用入口

## 1. 今天先看什么

1. `CURRENT_STATE.md`
2. `20260428.md`
3. `20260428_checklist.md`
4. `20260427_重构降摩擦.md`

## 2. 根目录保留规则

- `README.md`
- `CURRENT_STATE.md`
- `20260428.md`
- `20260428_checklist.md`
- `20260427_重构降摩擦.md`

## 3. 主题目录

- `topics/H5/`
- `topics/云卡/`
- `topics/小花科技/`
- `topics/异步补偿/`
- `topics/科技平台/`
- `topics/退款分发/`
- `topics/里程碑/`
- `topics/项目阶段性/`
- `topics/齐为/`
- `topics/重构降摩擦/`

## 4. 历史归档

- `archive/202604/`

## 5. 当前活跃专题

- `20260427_重构降摩擦.md`
- `topics/重构降摩擦/20260428_phase0_baseline.md`
```

- [ ] **Step 6: Refresh `docs/plan/CURRENT_STATE.md` only if it still points at old root locations**

Run:

```bash
rg -n "docs/plan/20260419|docs/plan/20260420|docs/plan/20260421|docs/plan/20260423|docs/plan/20260424|docs/plan/20260426|docs/plan/20260427_H5|docs/plan/20260427_checklist" docs/plan/CURRENT_STATE.md
```

Expected: either no matches, or the file is updated so its references match the new archive/topic locations.

- [ ] **Step 7: Verify the root was slimmed correctly**

Run:

```bash
find docs/plan -maxdepth 1 -type f | sort
```

Expected output:

```text
docs/plan/20260427_重构降摩擦.md
docs/plan/20260428.md
docs/plan/20260428_checklist.md
docs/plan/CURRENT_STATE.md
docs/plan/README.md
```

- [ ] **Step 8: Commit the root-slimming task**

```bash
git add docs/plan
git commit -m "docs: slim docs plan entrypoints"
```

---

### Task 3: Split `AGENTS.md` into Root, Backend, and H5 Guides

**Files:**
- Modify: `AGENTS.md`
- Create: `docs/AGENTS_BACKEND.md`
- Create: `docs/AGENTS_H5.md`
- Test: `wc -l AGENTS.md`

- [ ] **Step 1: Create the backend guide**

Create `docs/AGENTS_BACKEND.md` with this exact content:

```md
# AGENTS Backend Guide

## Scope

Java 17, Spring Boot 3.2, MyBatis-Plus, and MySQL 8.0 backend code under `src/main/java/com/nexusfin/equity/`.

## Build and Verification

- Build: `mvn clean package -DskipTests`
- Test: `mvn test`
- MySQL regression: `MYSQL_IT_ENABLED=true MYSQL_IT_DATABASE=nexusfin_equity mvn -Dtest=MySqlRoundTripIntegrationTest,MySqlCallbackFlowIntegrationTest test`
- Checkstyle: `mvn checkstyle:check`

## Non-Negotiable Rules

- All REST APIs return `Result<T>`
- No business logic in controllers
- No hardcoded config values
- No `System.out.println`
- No database queries inside loops
- No swallowed exceptions

## Domain Rules

- Amount fields use `Long` in fen
- Sensitive fields must be encrypted at rest and queried with hash indexes
- Key business logs include `traceId + bizOrderNo`
```

- [ ] **Step 2: Create the H5 guide**

Create `docs/AGENTS_H5.md` with this exact content:

```md
# AGENTS H5 Guide

## Scope

React 19 + TypeScript H5 app under `H5/`.

## Build and Verification

- Install: `cd H5 && pnpm install`
- Dev: `cd H5 && pnpm dev`
- Build: `cd H5 && pnpm build`
- Type check: `cd H5 && pnpm check`

## Non-Negotiable Rules

- Pages live in `H5/client/src/pages/`
- Routes register in `H5/client/src/App.tsx`
- Every page uses `MobileLayout`
- Navigation uses `wouter`
- No `any`
- API requests go through `H5/client/src/lib/api.ts`

## UI Rules

- UI copy stays in Simplified Chinese
- Preserve the existing H5 visual language unless the task explicitly redesigns it
- Follow the project design token set documented in the repository
```

- [ ] **Step 3: Replace the root `AGENTS.md` with a short entry file**

Replace `AGENTS.md` with this exact content:

```md
# NexusFin - nexusfin-equity

## Project Summary

惠聚项目艾博生权益分发服务，包含 Java 后端与 `H5/` 移动端前端。

## Read Order

1. `docs/plan/README.md`
2. `docs/plan/CURRENT_STATE.md`
3. `docs/plan/20260428.md`
4. `docs/plan/20260428_checklist.md`
5. `docs/plan/20260427_重构降摩擦.md`
6. `docs/AGENTS_BACKEND.md`
7. `docs/AGENTS_H5.md`

## Non-Negotiable Rules

- Controller 层不写业务逻辑
- 禁止硬编码配置值
- 禁止 `System.out.println`
- 禁止循环内查库
- 禁止吞异常

## Verification Baseline

- Java: `mvn test`
- H5: `cd H5 && ./node_modules/.bin/tsc --noEmit`
```

- [ ] **Step 4: Verify the root guide is now short**

Run:

```bash
wc -l AGENTS.md
```

Expected: the line count is below `60`.

- [ ] **Step 5: Commit the AGENTS split**

```bash
git add AGENTS.md docs/AGENTS_BACKEND.md docs/AGENTS_H5.md
git commit -m "docs: split root agents guidance by domain"
```

---

### Task 4: Archive `specs/004` and Relocate the QW Demo Sources

**Files:**
- Create: `docs/_archive/`
- Move: `specs/004-benefit-loan-flow/`
- Move: `src/main/java/com/nexusfin/equity/thirdparty/qw/demo/`
- Modify: `pom.xml`
- Modify: active docs returned by the scans in Steps 4-5
- Test: `rg` scan, `mvn -q -DskipTests compile`

- [ ] **Step 1: Create the archive destination**

Run:

```bash
mkdir -p docs/_archive
mkdir -p docs/third-part
```

Expected: `docs/_archive/` and `docs/third-part/` both exist.

- [ ] **Step 2: Confirm the demo source tree is not referenced by active Java code**

Run:

```bash
rg -n "thirdparty/qw/demo" src/main/java src/test/java pom.xml
```

Expected: only the `pom.xml` exclude matches.

- [ ] **Step 3: Move the archived spec tree and the demo tree**

Run:

```bash
git mv specs/004-benefit-loan-flow docs/_archive/specs-004
git mv src/main/java/com/nexusfin/equity/thirdparty/qw/demo docs/third-part/qw-demo
```

Expected:

```text
specs/004-benefit-loan-flow/      -> missing from specs/
docs/_archive/specs-004/          -> present
src/main/java/com/nexusfin/equity/thirdparty/qw/demo/ -> missing from src/main/java/
docs/third-part/qw-demo/          -> present
```

- [ ] **Step 4: Remove the stale `pom.xml` exclude for the moved demo path**

Delete this line from `pom.xml`:

```xml
<exclude>com/nexusfin/equity/thirdparty/qw/demo/**</exclude>
```

Keep the `EncryptDemo.java` exclude only if that file still exists and still must stay out of compilation.

- [ ] **Step 5: Repair active documentation references returned by the path scan**

Run:

```bash
rg -n "specs/004-benefit-loan-flow|src/main/java/com/nexusfin/equity/thirdparty/qw/demo|thirdparty/qw/demo" docs specs AGENTS.md README.md pom.xml
```

Update every non-archive, non-Phase-0-planning match so it points to the new locations:

```text
docs/_archive/specs-004/
docs/third-part/qw-demo/
```

Leave intentional historical mentions in the Phase 0 design/plan docs unchanged.

- [ ] **Step 6: Verify Java compilation still succeeds**

Run:

```bash
mvn -q -DskipTests compile
```

Expected: exit code `0`.

- [ ] **Step 7: Commit the archive/noise migration**

```bash
git add docs/_archive docs/third-part pom.xml specs src/main/java AGENTS.md README.md
git commit -m "chore: move archived specs and demo sources out of active paths"
```

---

### Task 5: Add AI Ignore Files and Finish Reference Cleanup

**Files:**
- Create: `.codexignore`
- Create: `.aiignore`
- Modify: any remaining active docs found by scans
- Test: residual `rg` scan

- [ ] **Step 1: Create `.codexignore`**

Create `.codexignore` with this exact content:

```text
docs/plan/archive/
docs/_archive/
docs/third-part/
target/
```

- [ ] **Step 2: Create `.aiignore`**

Create `.aiignore` with this exact content:

```text
docs/plan/archive/
docs/_archive/
docs/third-part/
target/
```

- [ ] **Step 3: Run the residual path scan**

Run:

```bash
rg -n "specs/004-benefit-loan-flow|src/main/java/com/nexusfin/equity/thirdparty/qw/demo|thirdparty/qw/demo|docs/plan/20260419|docs/plan/20260420|docs/plan/20260421|docs/plan/20260423|docs/plan/20260424|docs/plan/20260426|docs/plan/20260427_H5前后端接口" docs AGENTS.md README.md pom.xml
```

Expected: remaining matches are only intentional archive references or the Phase 0 planning/design documents.

- [ ] **Step 4: Commit the ignore files and final doc cleanups**

```bash
git add .codexignore .aiignore docs AGENTS.md README.md pom.xml
git commit -m "chore: reduce default AI scan surface"
```

---

### Task 6: Validate the New Entry Surface and Record the Snapshot

**Files:**
- Modify: `docs/plan/topics/重构降摩擦/20260428_phase0_baseline.md`
- Test: root listing, `wc -l`, `mvn -q -DskipTests compile`, `cd H5 && ./node_modules/.bin/tsc --noEmit`

- [ ] **Step 1: Measure the post-change entry surface**

Run:

```bash
find docs/plan -maxdepth 1 -type f | wc -l
wc -l AGENTS.md
```

Expected:

```text
5
26 AGENTS.md
```

- [ ] **Step 2: Append the post-change snapshot to the baseline file**

Append a new `## 4. Post-Phase-0 Snapshot` section to `docs/plan/topics/重构降摩擦/20260428_phase0_baseline.md` using the two values returned in Step 1, plus these four final booleans:

```md
## 4. Post-Phase-0 Snapshot

- `specs/004-benefit-loan-flow/` present under `specs/`: `no`
- `src/main/java/com/nexusfin/equity/thirdparty/qw/demo/` present: `no`
- `.codexignore` present: `yes`
- `.aiignore` present: `yes`
```

Expected: the baseline file now shows both the pre-change baseline and the post-change snapshot.

- [ ] **Step 3: Run the final stale-path scan**

Run:

```bash
rg -n "specs/004-benefit-loan-flow|src/main/java/com/nexusfin/equity/thirdparty/qw/demo|thirdparty/qw/demo" docs specs src pom.xml AGENTS.md README.md
```

Expected: only intentional historical references remain in archive or Phase 0 planning documents.

- [ ] **Step 4: Run Java compile verification**

Run:

```bash
mvn -q -DskipTests compile
```

Expected: exit code `0`.

- [ ] **Step 5: Run H5 type-check verification**

Run:

```bash
cd H5 && ./node_modules/.bin/tsc --noEmit
```

Expected: exit code `0`.

- [ ] **Step 6: Commit the validation snapshot**

```bash
git add docs/plan/topics/重构降摩擦/20260428_phase0_baseline.md
git commit -m "docs: record phase 0 validation snapshot"
```

---

## Spec Coverage Check

This plan covers every approved requirement from `docs/superpowers/specs/2026-04-28-phase-0-cognitive-radius-reduction-design.md`:

1. Baseline capture -> Task 1
2. `docs/plan` root slimming while keeping `docs/plan/20260427_重构降摩擦.md` in root -> Task 2
3. `AGENTS.md` split -> Task 3
4. `specs/004` archive and `thirdparty/qw/demo` relocation -> Task 4
5. `.codexignore` and `.aiignore` -> Task 5
6. Lightweight validation -> Task 6

This plan intentionally excludes all Phase 1+ backend service refactors, H5 page decomposition, and test-architecture changes.
