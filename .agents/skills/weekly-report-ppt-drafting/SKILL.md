---
name: weekly-report-ppt-drafting
description: Use when a user provides weekly progress notes and wants a one-page management weekly report draft that follows the existing 惠聚周报 PPT structure, section layout, and concise business tone
---

# Weekly Report PPT Drafting

## Overview

Use this skill when the user wants a weekly report or PPT draft that matches the existing single-page 惠聚周报 template in this repository.

Default goal: turn scattered weekly notes into a PPT-ready one-page draft for management review, not a detailed engineering log.

## Source Material

Use these sources in this order:

1. The user's latest raw notes or weekly summary.
2. Existing weekly or daily reports under `docs/日报/` and `docs/周报/` if the user asks to summarize from repository material.
3. The reference template notes in `references/template-spec.md`.

If needed, inspect the source template at:

- `docs/周报/PPT/20260328周报_wuhao_惠聚.pptx`

Do not invent facts that are not supported by the user input or repository documents.

## Required Output Shape

Always structure the draft in this order:

1. `标题`
2. `项目状态`
3. `一、产品`
4. `二、研发`
5. `三、商务（供应商 · 法务 · 其它）`
6. `风险 & 诉求`
7. `下周核心计划提炼`

Prefer a PPT-ready Markdown block unless the user explicitly asks for another format.

## Workflow

1. Extract the week's factual changes, decisions, risks, blockers, and next steps.
2. Group items into `产品` / `研发` / `商务（供应商 · 法务 · 其它）`.
3. Compress each grouped item into `事项或里程碑 + 状态 + 关键节点 / 进展`.
4. Write one overall title that summarizes the week's main signal, not a list of tasks.
5. Write one `项目状态` line using management language such as `符合预期，正常跟进` or `有风险，需协调`.
6. Summarize `风险 & 诉求` as coordination needs, dependencies, or unresolved blockers.
7. Summarize `下周核心计划提炼` into short action-oriented lines.

## Writing Rules

1. Keep it to one-page density. Prefer 2-3 rows per major section.
2. Write for management readability first; avoid class names, method names, or test names unless the user explicitly wants technical detail.
3. Prefer milestone language over task language.
4. Prefer concise status words such as `完成`、`进行中`、`推进中`、`待确认`.
5. Each progress cell should read like one compact sentence with a clear business signal.
6. If material is insufficient, say `待补充` instead of guessing.
7. Preserve important external dependencies such as legal review, vendor contracts, payment access, or third-party environment coordination.

## Section Guidance

### 一、产品

Use for:

- Product solution convergence
- Page/process changes
- Compliance and user experience tradeoffs
- PRD or agreement alignment

### 二、研发

Use for:

- Core chain delivery
- Authentication, orders, callbacks, integration, security, regression, release readiness
- External system integration milestones

### 三、商务（供应商 · 法务 · 其它）

Use for:

- Vendor contracts
- Legal review
- E-signature supplier progress
- Payment channel filing
- External coordination items that are not pure product or engineering delivery

## Output Template

```md
标题：技术周报：惠聚项目 — <一句话总述>
项目状态：<符合预期，正常跟进 / 有风险，需协调 / 其他>

一、产品
| 事项 | 状态 | 关键节点 / 进展 |
| --- | --- | --- |
| <事项1> | <状态> | <进展> |
| <事项2> | <状态> | <进展> |

二、研发
| 里程碑 | 状态 | 关键节点 / 进展 |
| --- | --- | --- |
| <里程碑1> | <状态> | <进展> |
| <里程碑2> | <状态> | <进展> |

三、商务（供应商 · 法务 · 其它）
| 事项 | 状态 | 关键节点 / 进展 |
| --- | --- | --- |
| <事项1> | <状态> | <进展> |
| <事项2> | <状态> | <进展> |

风险 & 诉求
<1-3 条简明风险或协调诉求>

下周核心计划提炼
<按主题提炼 2-4 条>
```

## When Not To Use

Do not use this skill for:

1. Multi-page technical architecture decks.
2. Daily reports that need full chronological detail.
3. PPT visual redesign work where the user wants a new style rather than this existing template.
