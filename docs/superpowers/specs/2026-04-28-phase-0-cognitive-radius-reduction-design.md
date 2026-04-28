# Phase 0 Cognitive Radius Reduction Design

> Date: 2026-04-28
> Scope: `docs/`, `AGENTS.md`, AI ignore files, archived specs, non-runtime demo sources
> Source plan: `docs/plan/20260427_重构降摩擦.md`

---

## 1. Goal

Implement Phase 0 of the "降低 Codex 迭代摩擦" plan as a full, bounded refactor of project entrypoints and repository scan surface.

The goal of this phase is not to optimize runtime behavior or business code. The goal is to reduce the amount of irrelevant context that a fresh agent or developer has to load before doing useful work.

Success means:

1. `docs/plan` becomes a small, intentional entry surface instead of a mixed working directory.
2. `AGENTS.md` becomes a concise root entrypoint instead of a long all-in-one instruction file.
3. archived specs and non-runtime demo sources stop competing with active implementation files in default search paths.
4. AI-facing ignore files explicitly exclude archive/noise paths.

---

## 2. Scope Boundaries

### In Scope

1. Add a measurable Phase 0 baseline document.
2. Reorganize `docs/plan` root into a stable "current entrypoint" structure.
3. Create archive destinations for old day plans and checklists.
4. Split `AGENTS.md` into:
   - root `AGENTS.md` as the short entrypoint
   - `docs/AGENTS_BACKEND.md`
   - `docs/AGENTS_H5.md`
5. Move `specs/004-benefit-loan-flow/` into an archive location under `docs/_archive/`.
6. Move `src/main/java/com/nexusfin/equity/thirdparty/qw/demo/` out of the active source tree, if it is not referenced by runtime code.
7. Add `.codexignore` and `.aiignore` with archive/noise exclusions.
8. Repair references affected by file moves.
9. Run lightweight verification proportional to the change surface.

### Explicitly Out of Scope

1. No `LoanServiceImpl` or `RepaymentServiceImpl` splitting.
2. No helper extraction such as `MoneyUnits`, `BizIds`, or `YunkaCallTemplate`.
3. No H5 page decomposition or shadcn cleanup.
4. No test architecture refactor.
5. No runtime business logic changes unless required to keep references/build valid after file moves.

---

## 3. Design Principles

### 3.1 Entry Surface Must Be Small

The repository root and `docs/plan` root should answer only one question: "Where do I start today?"

Any file that is historical, thematic, or no longer the current execution entrypoint should not remain in the root of `docs/plan`.

### 3.2 Archive, Do Not Delete, Unless Pure Noise

Historical specs and plan artifacts should be archived instead of deleted so that context remains available without polluting default discovery.

Pure demo files that are not part of the runtime code path may be moved out of `src/main/java`, because their current location is misleading.

### 3.3 Root Instructions Must Be Short and Directional

`AGENTS.md` should guide readers to the right detailed file rather than attempt to be the whole operating manual.

### 3.4 Verification Must Match Change Surface

This phase changes structure, entrypoints, and search paths, not product behavior. Verification should therefore emphasize:

1. broken references
2. build safety
3. H5 type safety

It should not default to unrelated full regressions unless the moves affect runtime code unexpectedly.

---

## 4. Target Repository Structure

### 4.1 `docs/plan/` Root

The intended root set after Phase 0 is:

1. `docs/plan/README.md`
2. `docs/plan/CURRENT_STATE.md`
3. current-day plan file
4. current-day checklist file
5. the active refactor master plan: `docs/plan/20260427_重构降摩擦.md`

This keeps the root limited to stable entrypoints plus the single currently active cross-cutting initiative.

### 4.2 `docs/plan/archive/202604/`

Historical daily plans/checklists and no-longer-current root-level plan artifacts move here.

Examples:

1. old daily plans
2. old daily checklists
3. day-specific planning snapshots no longer serving as root entrypoints

### 4.3 `docs/plan/topics/重构降摩擦/`

This becomes the topic home for:

1. Phase 0 baseline metrics
2. execution notes for future phases
3. derivative sub-plans created later from the master refactor plan

### 4.4 `docs/_archive/specs-004/`

`specs/004-benefit-loan-flow/` is archived here as a historical spec tree. It is preserved for traceability, but it is no longer part of the active execution entry surface.

### 4.5 Agent Guidance Files

The target agent docs are:

1. `AGENTS.md`
2. `docs/AGENTS_BACKEND.md`
3. `docs/AGENTS_H5.md`

Root `AGENTS.md` should retain:

1. project overview
2. critical prohibitions
3. recommended read order
4. links to backend/H5 specifics

The detailed rules move to the two domain-specific files.

---

## 5. Handling Noise Sources

### 5.1 Archived Spec Tree

`specs/004-benefit-loan-flow/` should move to `docs/_archive/specs-004/`.

Requirements:

1. preserve content
2. update references in docs and entrypoints
3. clearly label it as non-current

### 5.2 QW Demo Sources

`src/main/java/com/nexusfin/equity/thirdparty/qw/demo/` should no longer live in active source discovery if it is not referenced by runtime code.

Preferred handling:

1. confirm no runtime or test dependency
2. move to a documentation/archive path such as `docs/third-part/qw-demo/`
3. leave a small note in the destination if needed

This keeps historical samples available while removing them from default source searches.

### 5.3 AI Ignore Files

Add:

1. `.codexignore`
2. `.aiignore`

They should exclude at minimum:

1. `docs/plan/archive/`
2. `docs/_archive/`
3. `docs/third-part/`
4. `src/main/java/com/nexusfin/equity/thirdparty/qw/demo/` if it still exists during migration

The active topic directories should remain visible unless they are explicitly historical.

---

## 6. Execution Sequence

Phase 0 should be executed in this order:

1. Create a baseline metrics document.
2. Create target archive/topic directories.
3. Slim `docs/plan` root and rewrite `docs/plan/README.md`.
4. Split `AGENTS.md` into root/backend/H5 files.
5. Move `specs/004-benefit-loan-flow/` into `docs/_archive/specs-004/`.
6. Move `thirdparty/qw/demo/` out of active source tree.
7. Add `.codexignore` and `.aiignore`.
8. Repair references.
9. Run validation.

This order is intentional:

1. baseline first preserves pre-change measurements
2. root cleanup before ignore updates keeps path debugging simple
3. ignore files last prevent self-inflicted discovery blind spots during migration

---

## 7. Verification Strategy

### 7.1 Documentation and Reference Checks

Use targeted search to detect stale paths:

1. references to `specs/004-benefit-loan-flow`
2. references to `thirdparty/qw/demo`
3. references to old `docs/plan` root files that were moved

### 7.2 Build Safety

Run:

1. `mvn -q -DskipTests compile`

This is enough to catch accidental runtime dependencies on moved Java demo files.

### 7.3 H5 Safety

Run:

1. `cd H5 && ./node_modules/.bin/tsc --noEmit`

This confirms the doc and repo-structure changes did not accidentally damage the H5 project surface.

### 7.4 Optional Escalation Rule

If moving demo files exposes unexpected runtime/test references, verification scope expands before merge. In that case, additional Java tests can be added selectively rather than defaulting to all regressions.

---

## 8. Deliverables

Phase 0 is complete when the repository contains:

1. a baseline metrics document under `docs/plan/topics/重构降摩擦/`
2. a reduced `docs/plan` root
3. archive directories populated with moved historical files
4. a split `AGENTS` structure
5. archived `specs/004` content
6. relocated `qw/demo` content or an explicit removal with evidence of no references
7. `.codexignore`
8. `.aiignore`

---

## 9. Risks and Controls

### Risk 1: Broken Doc Links

Control:

1. search-and-repair after every move class
2. spot-check root entry files manually

### Risk 2: Hidden Source Dependency on Demo Files

Control:

1. search for references before moving
2. run `mvn -q -DskipTests compile` after the move

### Risk 3: Over-thinning `AGENTS.md`

Control:

1. keep root prohibitions and read order
2. move detail, do not drop it

### Risk 4: Archive Paths Become Another Mess

Control:

1. archive by month for daily operational files
2. archive by topic for non-current specs
3. keep root README authoritative

---

## 10. Non-Goals for Future Phases

Future phases may:

1. extract backend cross-cutting helpers
2. split oversized services
3. shrink `thirdparty/qw/`
4. decompose H5 pages
5. reduce test context sprawl

This design intentionally does not include implementation decisions for those phases.
