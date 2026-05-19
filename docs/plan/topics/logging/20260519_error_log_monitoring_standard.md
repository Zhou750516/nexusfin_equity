# Error Log Monitoring Standard

## Unified Fields

WARN and ERROR logs must keep using the existing project-wide monitoring field names:

- `errorNo`
- `errorMsg`

Do not introduce a replacement field set such as `errno`, `errmsg`, `error_no`, or `error_msg`. If a log collection platform later requires lowercase aliases, add them only as compatibility fields while preserving `errorNo` and `errorMsg`.

## Required WARN/ERROR Fields

Every WARN and ERROR log should include:

- `traceId`
- `bizOrderNo`, or `SYSTEM` / `UNKNOWN` when there is no business order
- `errorNo`
- `errorMsg`

Third-party outbound failed, rejected, and timeout logs should also include the available request context:

- `requestId`, if available
- `path`, `method`, or third-party method name
- `elapsedMs`, if measured
- request and response summaries, with sensitive fields redacted

INFO success logs do not need `errorNo=OK` or `errorMsg=OK`. Avoid adding OK fields to normal success logs because that pollutes monitoring queries. INFO logs that represent ignored, deferred, or duplicate business states should use explicit `status` / `reason` fields first.

## Error Field Resolution

Use `ErrorLogFields` for reusable error extraction:

- `BizException` maps to its `errorNo` and `errorMsg`.
- Other exceptions map to the exception class name and root-cause message.
- Empty messages fall back to the provided default or exception class name.
- Messages are truncated before logging to avoid dumping long HTML responses or stack traces into `errorMsg`.

Do not put stack traces or sensitive payloads into `errorMsg`. Passing the exception as the final logger argument is acceptable when the log level and class already require stack traces.

## Third-Party Error Naming

New third-party clients should use provider-scoped error numbers:

- `<PROVIDER>_UPSTREAM_REJECTED` for provider business rejection.
- `<PROVIDER>_UPSTREAM_TIMEOUT` for timeout.
- `<PROVIDER>_UPSTREAM_FAILED` for HTTP, network, parse, or unknown upstream failure.

If the provider has its own code, log it separately as `upstreamCode` when useful. Keep `errorNo` as the normalized monitoring code and `errorMsg` as the sanitized message.

Examples for future 法大大 integration:

- `FADADA_UPSTREAM_REJECTED`
- `FADADA_UPSTREAM_TIMEOUT`
- `FADADA_UPSTREAM_FAILED`
- `upstreamCode=<法大大原始code>`

## Sensitive Data Rules

Logs must not expose:

- Full ID card numbers
- Full bank card numbers
- Full phone numbers
- Tokens
- Signing keys, AES keys, or complete signatures
- Full encrypted payloads when only a length/hash summary is needed

When payload summaries are needed, log redacted markers, length, and hash. If plaintext payload logging is temporarily enabled for联调, keep it behind an explicit configuration switch and redact sensitive fields where possible.

## Coverage Guard

`LogErrorFieldsCoverageTest` scans source files for `log.warn(` and `log.error(` statements and fails when the statement text lacks both `errorNo` and `errorMsg`. This is intentionally lightweight and exists to keep future Yunka, QW, 法大大, and async compensation logs aligned with the monitoring contract.
