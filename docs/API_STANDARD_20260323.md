# NexusFin Equity API Standard

## 1. Scope

This document reflects the current public API behavior after the cross-domain
auth update on 2026-03-24.

Current entry flow:

1. Tech-platform browser redirect to `GET /api/auth/sso-callback`
2. ABS issues local JWT cookie
3. Frontend checks `GET /api/users/me`
4. Logged-in user calls `/api/equity/**`
5. Partner callbacks continue to use `/api/callbacks/**` with signature headers

## 2. Security Rules

### 2.1 Browser-facing auth

- `GET /api/auth/sso-callback` accepts the upstream `token` and returns `302`
  with local JWT cookie issuance.
- `GET /api/users/me` and business endpoints under `/api/equity/products/**`,
  `/api/equity/orders/**`, and `/api/equity/exercise-url/**` require the local
  JWT cookie `NEXUSFIN_AUTH`.
- Cookie attributes: `HttpOnly`, `SameSite=Lax`, `Secure` in production config.

### 2.2 Partner callbacks

- `/api/callbacks/**` continue to require:
  - `X-App-Id`
  - `X-Timestamp`
  - `X-Nonce`
  - `X-Signature`

### 2.3 Unified response

- JSON APIs continue to use `Result<T>` with `code`, `message`, and `data`.
- Browser redirect endpoint `GET /api/auth/sso-callback` is the exception and
  returns `302` instead of `Result<T>`.

## 3. Public APIs

### 3.1 `GET /api/auth/sso-callback`

- Purpose: verify the upstream tech-platform token, JIT-create or reuse the
  ABS-side member, set local JWT cookie, and redirect the browser.
- Query params:
  - `token` required
  - `redirect_url` optional, must match redirect whitelist
- Success:
  - HTTP `302`
  - `Set-Cookie: NEXUSFIN_AUTH=...`
  - `Location: <redirect_url>`

### 3.2 `GET /api/users/me`

- Purpose: validate current local JWT cookie and return current ABS-side user.
- Auth: local JWT cookie required
- Success body:

```json
{
  "code": 0,
  "message": "OK",
  "data": {
    "memberId": "mem-auth-seed-001",
    "techPlatformUserId": "tech-user-seed-001",
    "externalUserId": "tech-user-seed-001",
    "memberStatus": "ACTIVE"
  }
}
```

### 3.3 `GET /api/equity/products/{productCode}`

- Purpose: load product purchase page data for the authenticated member.
- Auth: local JWT cookie required
- Notes:
  - `memberId` query param is no longer accepted
  - member context is derived from the local JWT

### 3.4 `POST /api/equity/orders`

- Purpose: create a benefit order for the authenticated member.
- Auth: local JWT cookie required
- Request body:

```json
{
  "requestId": "req-order-create-001",
  "productCode": "QS-PROD-001",
  "loanAmount": 880000,
  "agreementSigned": true
}
```

- Notes:
  - `memberId` is no longer a client-supplied field
  - controller derives the acting member from auth context

### 3.5 `GET /api/equity/orders/{benefitOrderNo}`

- Purpose: query current order status
- Auth: local JWT cookie required

### 3.6 `GET /api/equity/exercise-url/{benefitOrderNo}`

- Purpose: query exercise URL for an existing order
- Auth: local JWT cookie required

## 4. Callback APIs

The callback contract paths remain unchanged:

- `POST /api/callbacks/first-deduction`
- `POST /api/callbacks/fallback-deduction`
- `POST /api/callbacks/grant/forward`
- `POST /api/callbacks/repayment/forward`
- `POST /api/callbacks/exercise-equity`
- `POST /api/callbacks/refund`

The auth change does not alter callback payload semantics. Those interfaces
still rely on partner signature verification rather than local JWT cookie auth.

## 5. Removed Public Entry

The following interface is obsolete and has been removed from runtime code:

- `POST /api/users/register`

Rejected-user onboarding is now handled through SSO callback + JIT provisioning
inside the ABS service.
