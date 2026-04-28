# Phase 4 H5 Page Decomposition Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reduce H5 page and i18n file size by extracting shared UI sections, splitting `CalculatorPage` into focused subcomponents, and pruning unused shadcn-style UI files without changing current page behavior.

**Architecture:** Preserve the current route surface and page behavior, especially `landing` and `joint-*` routes where product deletion semantics remain ambiguous. Use the existing Tailwind 4 CSS-first setup under `H5/client/src/index.css` as the semantic token source, add shared layout/card/hero/action primitives under `H5/client/src/components/`, keep page-specific logic in the existing `*.logic.ts` files, and move bulky page JSX into colocated page section components so container pages become thin composition shells.

**Tech Stack:** React 19, TypeScript 5.6, Vite 7, Tailwind CSS 4, Vitest, Wouter

---

## Baseline

- Baseline capture date: `2026-04-28`
- Source root: `H5/client/src`
- Current oversized files:
  - `H5/client/src/pages/CalculatorPage.tsx`: `645` lines
  - `H5/client/src/pages/ConfirmRepaymentPage.tsx`: `486` lines
  - `H5/client/src/pages/BenefitsCardPage.tsx`: `473` lines
  - `H5/client/src/i18n/messages.ts`: `587` lines
  - `H5/client/src/components/ui/sidebar.tsx`: `734` lines
  - `H5/client/src/components/ui/chart.tsx`: `355` lines
- Existing focused H5 regression commands:
  - `cd H5 && ./node_modules/.bin/vitest run client/src/pages/calculator.logic.test.ts client/src/pages/approval-pending-benefits.logic.test.ts client/src/pages/approval-result.logic.test.ts client/src/pages/benefits-card.logic.test.ts`
  - `cd H5 && ./node_modules/.bin/tsc --noEmit`
- Current route boundary to preserve:
  - keep `/landing`
  - keep all `/joint-*`
  - keep `/404`
  - do not delete `NotFound` or route entries unless future product direction becomes explicit

## File Structure

### Files to Create

- `H5/client/src/components/shared/SectionCard.tsx`
- `H5/client/src/components/shared/StickyActionBar.tsx`
- `H5/client/src/components/calculator/CalculatorHero.tsx`
- `H5/client/src/components/calculator/CalculatorAmountDrawer.tsx`
- `H5/client/src/components/calculator/CalculatorPurposeDrawer.tsx`
- `H5/client/src/components/calculator/CalculatorProtocolDrawer.tsx`
- `H5/client/src/components/calculator/CalculatorTermSection.tsx`
- `H5/client/src/components/calculator/CalculatorRepaymentSection.tsx`
- `H5/client/src/components/calculator/CalculatorReceivingAccountSection.tsx`
- `H5/client/src/components/calculator/CalculatorLenderSection.tsx`
- `H5/client/src/components/calculator/CalculatorAgreementSection.tsx`
- `H5/client/src/components/calculator/CalculatorTipsSection.tsx`
- `H5/client/src/i18n/messages/common.ts`
- `H5/client/src/i18n/messages/calculator.ts`
- `H5/client/src/i18n/messages/approval.ts`
- `H5/client/src/i18n/messages/benefits.ts`
- `H5/client/src/i18n/messages/repayment.ts`
- `H5/client/src/i18n/messages/joint.ts`
- `H5/client/src/i18n/messages/index.ts`

### Files to Modify

- `H5/client/src/index.css`
- `H5/client/src/App.tsx`
- `H5/client/src/components/LanguageSwitcher.tsx`
- `H5/client/src/components/PageFeedback.tsx`
- `H5/client/src/pages/CalculatorPage.tsx`
- `H5/client/src/pages/BenefitsCardPage.tsx`
- `H5/client/src/pages/ApprovalPendingPage.tsx`
- `H5/client/src/i18n/I18nProvider.tsx`
- `H5/client/src/i18n/locale.test.ts`

### Files to Delete

Delete only after reference verification shows zero imports:

- `H5/client/src/components/ui/sidebar.tsx`
- `H5/client/src/components/ui/chart.tsx`
- `H5/client/src/components/ui/carousel.tsx`
- `H5/client/src/components/ui/menubar.tsx`
- `H5/client/src/components/ui/context-menu.tsx`
- `H5/client/src/components/ui/navigation-menu.tsx`
- `H5/client/src/components/ui/pagination.tsx`
- `H5/client/src/components/ui/breadcrumb.tsx`
- `H5/client/src/components/ui/command.tsx`
- `H5/client/src/components/ui/hover-card.tsx`
- `H5/client/src/components/ui/avatar.tsx`
- `H5/client/src/components/ui/calendar.tsx`
- `H5/client/src/components/ui/resizable.tsx`
- `H5/client/src/components/ui/table.tsx`
- `H5/client/src/components/ui/input-otp.tsx`
- `H5/client/src/components/ui/tabs.tsx`
- `H5/client/src/components/ui/toggle.tsx`
- `H5/client/src/components/ui/toggle-group.tsx`
- `H5/client/src/components/ui/progress.tsx`
- `H5/client/src/components/ui/aspect-ratio.tsx`

### Files to Read During Implementation

- `docs/AGENTS_H5.md`
- `docs/plan/20260427_重构降摩擦.md`
- `H5/client/src/pages/CalculatorPage.tsx`
- `H5/client/src/pages/BenefitsCardPage.tsx`
- `H5/client/src/pages/ApprovalPendingPage.tsx`
- `H5/client/src/pages/calculator.logic.ts`
- `H5/client/src/pages/benefits-card.logic.ts`
- `H5/client/src/pages/approval-pending-benefits.logic.ts`
- `H5/client/src/i18n/messages.ts`
- `H5/client/src/index.css`
- `H5/client/src/App.tsx`

### Explicitly In Scope

- landing semantic design tokens into the active Tailwind 4 theme entrypoint
- extracting repeated card / sticky action / hero patterns into shared components
- shrinking `CalculatorPage.tsx` into a thin orchestration page with colocated section components
- splitting the monolithic `i18n/messages.ts` file into smaller domain modules
- deleting clearly unused `components/ui/*` files after import verification

### Explicitly Out of Scope

- deleting or changing ambiguous routes such as `/landing` and `/joint-*`
- changing H5 business flows, API payloads, or translation keys
- redesigning page visuals beyond shared tokenization and component extraction
- modifying backend Java production code
- modifying `.gitignore` or unrelated docs

### Validation Commands

- `cd H5 && ./node_modules/.bin/vitest run client/src/pages/calculator.logic.test.ts client/src/pages/approval-pending-benefits.logic.test.ts client/src/pages/approval-result.logic.test.ts client/src/pages/benefits-card.logic.test.ts client/src/i18n/locale.test.ts`
- `cd H5 && ./node_modules/.bin/tsc --noEmit`
- `cd H5 && rg -n "from \\\"@/components/ui/" client/src`
- `cd H5 && wc -l client/src/pages/CalculatorPage.tsx client/src/i18n/messages/index.ts client/src/i18n/messages/*.ts`
- `cd H5 && find client/src/components/ui -maxdepth 1 -type f | wc -l`

---

### Task 1: Establish Shared Tokens and Reusable H5 Surface Primitives

**Files:**
- Create: `H5/client/src/components/shared/SectionCard.tsx`
- Create: `H5/client/src/components/shared/StickyActionBar.tsx`
- Modify: `H5/client/src/index.css`
- Modify: `H5/client/src/components/LanguageSwitcher.tsx`
- Modify: `H5/client/src/components/PageFeedback.tsx`
- Test: `cd H5 && ./node_modules/.bin/tsc --noEmit`

- [ ] **Step 1: Introduce semantic H5 tokens in the active Tailwind 4 theme source**

Add H5-specific semantic variables under `@theme inline` / `:root` in `H5/client/src/index.css`, including:

```css
--color-h5-page: var(--background);
--color-h5-surface: oklch(0.985 0.003 265);
--color-h5-surface-strong: oklch(1 0 0);
--color-h5-border-soft: oklch(0.93 0.006 265);
--color-h5-text-primary: oklch(0.23 0.01 265);
--color-h5-text-secondary: oklch(0.52 0.015 265);
--color-h5-brand: oklch(0.57 0.2 259);
--color-h5-brand-strong: oklch(0.51 0.2 259);
--color-h5-brand-warm: oklch(0.77 0.16 72);
```

Also add a small set of utility component classes for:

```css
.h5-page-shell
.h5-card
.h5-card-title
.h5-card-subtitle
.h5-sticky-bar
.h5-primary-button
```

- [ ] **Step 2: Create the shared surface primitives**

Add `SectionCard.tsx`:

```tsx
import type { PropsWithChildren, ReactNode } from "react";

type SectionCardProps = PropsWithChildren<{
  title?: ReactNode;
  subtitle?: ReactNode;
  action?: ReactNode;
  className?: string;
  contentClassName?: string;
}>;

export default function SectionCard({
  title,
  subtitle,
  action,
  className,
  contentClassName,
  children,
}: SectionCardProps) {
  return (
    <section className={cn("h5-card", className)}>
      {(title || subtitle || action) && (
        <header className="flex items-start justify-between gap-3">
          <div className="min-w-0">
            {title ? <h3 className="h5-card-title">{title}</h3> : null}
            {subtitle ? <p className="h5-card-subtitle mt-1">{subtitle}</p> : null}
          </div>
          {action}
        </header>
      )}
      <div className={cn(title || subtitle || action ? "mt-4" : "", contentClassName)}>{children}</div>
    </section>
  );
}
```

Add `StickyActionBar.tsx`:

```tsx
import type { PropsWithChildren, ReactNode } from "react";

type StickyActionBarProps = PropsWithChildren<{
  primary?: ReactNode;
  secondary?: ReactNode;
  className?: string;
}>;

export default function StickyActionBar({
  primary,
  secondary,
  className,
  children,
}: StickyActionBarProps) {
  return (
    <div className={cn("h5-sticky-bar", className)}>
      {children}
      {secondary}
      {primary}
    </div>
  );
}
```

- [ ] **Step 3: Switch the light shared widgets to the new tokens first**

Update `LanguageSwitcher.tsx` and `PageFeedback.tsx` so their color classes stop hardcoding `#165dff`, `#86909c`, `#f2f3f5`, and instead use the semantic token classes introduced above.

- [ ] **Step 4: Verify the shared-token step**

Run:

```bash
cd H5 && ./node_modules/.bin/tsc --noEmit
```

Expected:

- type check passes with no new errors

- [ ] **Step 5: Commit the shared token groundwork**

```bash
git add H5/client/src/index.css \
        H5/client/src/components/LanguageSwitcher.tsx \
        H5/client/src/components/PageFeedback.tsx \
        H5/client/src/components/shared/SectionCard.tsx \
        H5/client/src/components/shared/StickyActionBar.tsx
git commit -m "refactor: add shared h5 section primitives"
```

---

### Task 2: Split CalculatorPage Into Focused Composition Sections

**Files:**
- Create: `H5/client/src/components/calculator/CalculatorHero.tsx`
- Create: `H5/client/src/components/calculator/CalculatorAmountDrawer.tsx`
- Create: `H5/client/src/components/calculator/CalculatorPurposeDrawer.tsx`
- Create: `H5/client/src/components/calculator/CalculatorProtocolDrawer.tsx`
- Create: `H5/client/src/components/calculator/CalculatorTermSection.tsx`
- Create: `H5/client/src/components/calculator/CalculatorRepaymentSection.tsx`
- Create: `H5/client/src/components/calculator/CalculatorReceivingAccountSection.tsx`
- Create: `H5/client/src/components/calculator/CalculatorLenderSection.tsx`
- Create: `H5/client/src/components/calculator/CalculatorAgreementSection.tsx`
- Create: `H5/client/src/components/calculator/CalculatorTipsSection.tsx`
- Modify: `H5/client/src/pages/CalculatorPage.tsx`
- Test: `cd H5 && ./node_modules/.bin/vitest run client/src/pages/calculator.logic.test.ts`
- Test: `cd H5 && ./node_modules/.bin/tsc --noEmit`

- [ ] **Step 1: Freeze calculator behavior with the existing focused test**

Run:

```bash
cd H5 && ./node_modules/.bin/vitest run client/src/pages/calculator.logic.test.ts
```

Expected:

- `1` file passed before any page split

- [ ] **Step 2: Move static rendering sections out of `CalculatorPage.tsx`**

Extract the following without changing data flow:

- `CalculatorHero.tsx`: top amount hero, edit action, amount range copy
- `CalculatorTermSection.tsx`: term pill selection
- `CalculatorRepaymentSection.tsx`: repayment summary and expanded plan list
- `CalculatorReceivingAccountSection.tsx`: receiving card row
- `CalculatorLenderSection.tsx`: annual rate, partner dialog trigger, lender list dialog
- `CalculatorAgreementSection.tsx`: protocol viewing state and agreement CTA copy
- `CalculatorTipsSection.tsx`: risk / partner tips body

Keep these inside `CalculatorPage.tsx`:

- `loadConfig`
- `loadCalculation`
- `handleSubmit`
- `normalizeAmount`
- `readErrorMessage`
- page-level state wiring and navigation

- [ ] **Step 3: Move modal/drawer markup out of the page shell**

Extract:

- `CalculatorAmountDrawer.tsx`
- `CalculatorPurposeDrawer.tsx`
- `CalculatorProtocolDrawer.tsx`

Each new component receives plain props from `CalculatorPage.tsx`; none should call APIs or navigation directly.

- [ ] **Step 4: Reduce `CalculatorPage.tsx` to orchestration only**

After extraction, `CalculatorPage.tsx` should primarily contain:

- imports
- state/effects
- API calls
- prop derivation
- composition order of the extracted sections

Target:

- page file below `250` lines

- [ ] **Step 5: Verify calculator-focused behavior**

Run:

```bash
cd H5 && ./node_modules/.bin/vitest run client/src/pages/calculator.logic.test.ts
cd H5 && ./node_modules/.bin/tsc --noEmit
```

Expected:

- calculator logic test remains green
- type check passes

- [ ] **Step 6: Commit the calculator decomposition**

```bash
git add H5/client/src/pages/CalculatorPage.tsx \
        H5/client/src/components/calculator \
        H5/client/src/components/shared
git commit -m "refactor: split calculator page into focused sections"
```

---

### Task 3: Split the Monolithic H5 Message Catalog Into Domain Modules

**Files:**
- Create: `H5/client/src/i18n/messages/common.ts`
- Create: `H5/client/src/i18n/messages/calculator.ts`
- Create: `H5/client/src/i18n/messages/approval.ts`
- Create: `H5/client/src/i18n/messages/benefits.ts`
- Create: `H5/client/src/i18n/messages/repayment.ts`
- Create: `H5/client/src/i18n/messages/joint.ts`
- Create: `H5/client/src/i18n/messages/index.ts`
- Modify: `H5/client/src/i18n/I18nProvider.tsx`
- Modify: `H5/client/src/i18n/locale.test.ts`
- Delete: `H5/client/src/i18n/messages.ts`
- Test: `cd H5 && ./node_modules/.bin/vitest run client/src/i18n/locale.test.ts client/src/pages/approval-pending-benefits.logic.test.ts client/src/pages/approval-result.logic.test.ts client/src/pages/benefits-card.logic.test.ts`
- Test: `cd H5 && ./node_modules/.bin/tsc --noEmit`

- [ ] **Step 1: Create module files by page/domain, keeping message keys unchanged**

Split `messages.ts` into:

- `common.ts`
- `calculator.ts`
- `approval.ts`
- `benefits.ts`
- `repayment.ts`
- `joint.ts`

Each file should export `Record<Locale, MessageDictionary>` or a same-shape partial. Do not rename any translation key.

- [ ] **Step 2: Add a single `index.ts` aggregator**

Build the final `messages` export in `H5/client/src/i18n/messages/index.ts` by merging the domain dictionaries:

```ts
export const messages: Record<Locale, MessageDictionary> = {
  "zh-CN": {
    ...common["zh-CN"],
    ...calculator["zh-CN"],
    ...approval["zh-CN"],
    ...benefits["zh-CN"],
    ...repayment["zh-CN"],
    ...joint["zh-CN"],
  },
  ...
};
```

Also re-export `LOCALE_LABELS` from this new index so `I18nProvider.tsx` keeps a stable import surface.

- [ ] **Step 3: Update the i18n provider import and extend the locale regression test**

Change `I18nProvider.tsx` to import from `./messages`.

In `locale.test.ts`, add a concrete contract assertion like:

```ts
import { messages } from "./messages";

it("should keep shared calculator and approval keys across all locales", () => {
  for (const locale of SUPPORTED_LOCALES) {
    expect(messages[locale]["calculator.submit"]).toBeTruthy();
    expect(messages[locale]["approvalPending.title"]).toBeTruthy();
    expect(messages[locale]["benefits.openNow"]).toBeTruthy();
  }
});
```

- [ ] **Step 4: Verify the i18n split**

Run:

```bash
cd H5 && ./node_modules/.bin/vitest run \
  client/src/i18n/locale.test.ts \
  client/src/pages/approval-pending-benefits.logic.test.ts \
  client/src/pages/approval-result.logic.test.ts \
  client/src/pages/benefits-card.logic.test.ts
cd H5 && ./node_modules/.bin/tsc --noEmit
```

Expected:

- all focused tests remain green
- type check passes

- [ ] **Step 5: Commit the message split**

```bash
git add H5/client/src/i18n/I18nProvider.tsx \
        H5/client/src/i18n/locale.test.ts \
        H5/client/src/i18n/messages \
        H5/client/src/i18n/messages.ts
git commit -m "refactor: split h5 i18n catalogs by domain"
```

---

### Task 4: Prune Unused shadcn-Style UI Files After Import Verification

**Files:**
- Modify: `H5/client/src/App.tsx`
- Modify: `H5/client/src/pages/BenefitsCardPage.tsx`
- Modify: `H5/client/src/pages/ApprovalPendingPage.tsx`
- Delete: the unused `H5/client/src/components/ui/*` files listed above after verification
- Test: `cd H5 && ./node_modules/.bin/vitest run client/src/pages/calculator.logic.test.ts client/src/pages/approval-pending-benefits.logic.test.ts client/src/pages/approval-result.logic.test.ts client/src/pages/benefits-card.logic.test.ts client/src/i18n/locale.test.ts`
- Test: `cd H5 && ./node_modules/.bin/tsc --noEmit`

- [ ] **Step 1: Verify which `components/ui/*` modules are truly unreferenced**

Run:

```bash
cd H5 && rg -n "from \\\"@/components/ui/" client/src
```

Use that result to confirm the deletion list is still unimported. If any file is imported, keep it and skip deletion rather than rewriting more page code in this phase.

- [ ] **Step 2: Apply low-risk token cleanup to the remaining large pages**

While already touching the H5 layer, replace the most repeated hardcoded page-shell colors in:

- `BenefitsCardPage.tsx`
- `ApprovalPendingPage.tsx`
- `App.tsx`

with shared `h5-*` token classes and shared surface primitives where that does not change layout behavior.

- [ ] **Step 3: Delete the verified-unused UI files only**

Delete the `components/ui/*` files that remain unreferenced after Step 1.

- [ ] **Step 4: Verify the final H5 focused regression and structure targets**

Run:

```bash
cd H5 && ./node_modules/.bin/vitest run \
  client/src/pages/calculator.logic.test.ts \
  client/src/pages/approval-pending-benefits.logic.test.ts \
  client/src/pages/approval-result.logic.test.ts \
  client/src/pages/benefits-card.logic.test.ts \
  client/src/i18n/locale.test.ts
cd H5 && ./node_modules/.bin/tsc --noEmit
cd H5 && wc -l client/src/pages/CalculatorPage.tsx client/src/i18n/messages/index.ts client/src/i18n/messages/*.ts
cd H5 && find client/src/components/ui -maxdepth 1 -type f | wc -l
```

Expected:

- focused tests green
- type check green
- `CalculatorPage.tsx` materially smaller than baseline `645`
- the largest i18n file materially smaller than baseline `587`
- `components/ui` file count lower than baseline

- [ ] **Step 5: Commit the Phase 4 cleanup**

```bash
git add H5/client/src/App.tsx \
        H5/client/src/pages/BenefitsCardPage.tsx \
        H5/client/src/pages/ApprovalPendingPage.tsx \
        H5/client/src/components/ui \
        H5/client/src/components/shared
git commit -m "refactor: prune unused h5 ui primitives"
```

---

## Final Phase Verification

- [ ] Run the complete H5 type check:

```bash
cd H5 && ./node_modules/.bin/tsc --noEmit
```

- [ ] Run the backend full suite to ensure H5-only changes did not disturb the workspace:

```bash
mvn test
```

- [ ] Run the H5 focused regression bundle:

```bash
cd H5 && ./node_modules/.bin/vitest run \
  client/src/pages/calculator.logic.test.ts \
  client/src/pages/approval-pending-benefits.logic.test.ts \
  client/src/pages/approval-result.logic.test.ts \
  client/src/pages/benefits-card.logic.test.ts \
  client/src/i18n/locale.test.ts
```

- [ ] Run structural checks for the final report:

```bash
cd H5 && wc -l client/src/pages/CalculatorPage.tsx client/src/i18n/messages/index.ts client/src/i18n/messages/*.ts
cd H5 && find client/src/components/ui -maxdepth 1 -type f | wc -l
cd H5 && rg -n 'Route path=\\{"/(landing|joint-entry|joint-dispatch|joint-refund-entry|joint-unsupported|404)' client/src/App.tsx
```

## Suggested Commit Boundary Summary

1. `refactor: add shared h5 section primitives`
2. `refactor: split calculator page into focused sections`
3. `refactor: split h5 i18n catalogs by domain`
4. `refactor: prune unused h5 ui primitives`
