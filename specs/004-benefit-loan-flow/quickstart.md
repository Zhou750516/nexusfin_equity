# Quickstart: 惠选卡权益与借款流程 V1 页面交互理解

## Current Implemented Slice

本轮已完成：

- 科技平台 outbound client 基础实现
- 3 个通知类接口请求模型
- 加密、签名、请求组包、响应解包测试
- 实现说明文档

## Local Verification

### 1. Run target client test

```bash
mvn -q -Dtest=TechPlatformClientTest test
```

### 2. Run full test suite

```bash
mvn -q test
```

### 3. Run build verification

```bash
mvn -q clean package -DskipTests
```

### 4. Run style verification

```bash
mvn -q checkstyle:check
```

## Latest Verification Record

2026-03-30 已实际执行并通过：

- `mvn -q -Dtest=TechPlatformClientTest test`
- `mvn -q test`
- `mvn -q clean package -DskipTests`
- `mvn -q checkstyle:check`

## Mock Configuration

默认测试环境通过以下配置启用 mock 模式：

- `nexusfin.third-party.tech-platform.enabled=true`
- `nexusfin.third-party.tech-platform.mode=MOCK`

如果要切换到 HTTP：

- 设置 `TECH_PLATFORM_API_MODE=HTTP`
- 配置 `TECH_PLATFORM_API_BASE_URL`
- 配置 `TECH_PLATFORM_API_CHANNEL_ID`
- 配置 `TECH_PLATFORM_API_SIGN_SECRET`
- 配置 `TECH_PLATFORM_API_AES_KEY_BASE64`

## Review Entry

代码 review 建议先看：

1. `src/main/java/com/nexusfin/equity/thirdparty/techplatform/TechPlatformClientImpl.java`
2. `src/main/java/com/nexusfin/equity/thirdparty/techplatform/TechPlatformPayloadCodec.java`
3. `src/test/java/com/nexusfin/equity/thirdparty/techplatform/TechPlatformClientTest.java`
4. `docs/科技平台接口调用实现说明_20260330.md`
