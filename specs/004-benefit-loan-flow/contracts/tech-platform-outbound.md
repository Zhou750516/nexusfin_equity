# Contract Notes: Tech Platform Outbound APIs

## Scope Implemented In This Slice

当前已实现的 outbound client 覆盖：

| API | Path | Direction | Purpose |
| --- | --- | --- | --- |
| `creditStatusNotice` | `/guide/api/creditStatusNotice` | ABS -> 科技平台 | 通知授信/进件结果 |
| `loanInfoNotice` | `/guide/api/loanInfoNotice` | ABS -> 科技平台 | 通知放款结果 |
| `repayInfoNotice` | `/guide/api/repayInfoNotice` | ABS -> 科技平台 | 通知还款结果 |

## Request Shape

### Headers

- `channelId`
- `timestamp`
- `sign`
- `version`

### Body

```json
{
  "param": "<encrypted-business-json>"
}
```

## Response Shape

当前 client 兼容两种响应形式：

### Plain Response

```json
{
  "code": "0",
  "msg": "ok"
}
```

### Encrypted Response

```json
{
  "param": "<encrypted-response-json>"
}
```

解密后仍应得到：

```json
{
  "code": "0",
  "msg": "ok"
}
```

## Assumptions

- 签名算法默认 `HMAC_SHA256`
- AES 模式默认 `AES/ECB/PKCS5Padding`
- 两项都可以通过配置切换
- 以上默认值用于本地开发与测试，不代表已经与科技平台联调确认
