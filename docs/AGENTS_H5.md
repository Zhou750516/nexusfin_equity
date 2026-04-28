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
