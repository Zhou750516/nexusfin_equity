# 惠聚单页周报 PPT 模板规范

## Template Source

- Source file: `docs/周报/PPT/20260328周报_wuhao_惠聚.pptx`
- Current observed form: single-slide weekly report
- Slide ratio: `16:9`

## Fixed Structure

Keep the page in this order:

1. Main title
2. Project status
3. `一、产品`
4. `二、研发`
5. `三、商务（供应商 · 法务 · 其它）`
6. `风险 & 诉求`
7. `下周核心计划提炼`

## Title Pattern

Use a title close to:

`技术周报：惠聚项目 — <一句话总结本周关键信号>`

Good title qualities:

1. Starts with the project label.
2. Summarizes the week's leading signal in one sentence.
3. Reads like a management headline, not a task list.

## Project Status Line

Use one compact status line after the title, for example:

- `项目状态：符合预期，正常跟进`
- `项目状态：整体可控，部分事项需协调`
- `项目状态：存在风险，需加快联调`

## Table Logic

Each major section uses a compact 3-column table:

- First column: `事项` or `里程碑`
- Second column: `状态`
- Third column: `关键节点 / 进展`

Recommended row count:

- Product: `1-3` rows
- R&D: `1-3` rows
- Business/Legal/Others: `1-3` rows

## Status Vocabulary

Prefer short, executive-friendly statuses:

- `完成`
- `进行中`
- `推进中`
- `待确认`
- `有风险`

Do not use overly technical statuses such as:

- `单测通过`
- `代码已提交`
- `字段已改名`

Unless the user explicitly wants a technical version.

## Content Selection Rules

### Product

Suitable topics:

- Page/process convergence
- Agreement alignment
- Compliance vs conversion tradeoff
- Product scheme closure

### R&D

Suitable topics:

- Core transaction chain
- Authentication and trust boundary work
- Integration milestones
- Security and compliance implementation
- Regression, packaging, acceptance readiness

### Business / Legal / Others

Suitable topics:

- Supplier contracts
- Legal confirmation
- E-signature vendor progress
- Payment channel filing
- External environment coordination

## Tone Rules

1. Prefer summary over chronology.
2. Prefer milestone phrasing over engineering detail.
3. Keep every cell short enough to fit on a single slide.
4. Preserve business signal: what changed, what it means, what is blocked.
5. When multiple technical actions point to one outcome, summarize the outcome instead of listing all actions.

## Risks & Requests

This section should capture:

1. Third-party environment dependencies
2. Cross-team coordination needs
3. Legal or contract blockers
4. Timelines at risk

Keep it short and action-oriented.

## Next Week Plan

Summarize by theme, not by task explosion. Good grouping examples:

1. `产品与法务：`
2. `研发与交付：`
3. `商务推进：`

Each line should state the expected result, not just the activity.
