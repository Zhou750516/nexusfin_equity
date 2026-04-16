#!/usr/bin/env bash

# 2026-04-01 M3.1 联调环境变量样板
# 用法：
# 1. 复制本文件为本地未纳管版本，例如 `docs/plan/.env.20260401.local.sh`
# 2. 将所有“待确认”占位值替换成真实参数
# 3. 执行 `source docs/plan/.env.20260401.local.sh`
# 4. 再启动应用进行联调
#
# 注意：
# - 本文件仅作为样板，默认值不可直接用于真实联调
# - 不要把真实密钥直接提交到仓库

export QW_ENABLED=true
export QW_MODE=HTTP
export QW_BASE_URL='待齐为确认'
export QW_METHOD_PATH='/api/abs/method'
export QW_PARTNER_NO='待齐为确认'
export QW_VERSION='v1.0'
export QW_SIGN_KEY='待齐为确认'
export QW_AES_KEY_BASE64='待齐为确认'
export QW_CONNECT_TIMEOUT_MS=3000
export QW_READ_TIMEOUT_MS=5000

export TECH_PLATFORM_BASE_URL='待科技平台确认'
export TECH_PLATFORM_USER_ME_PATH='/api/users/me'

export TECH_PLATFORM_API_ENABLED=true
export TECH_PLATFORM_API_MODE=HTTP
export TECH_PLATFORM_API_BASE_URL='待科技平台确认'
export TECH_PLATFORM_API_CHANNEL_ID='待科技平台确认'
export TECH_PLATFORM_API_VERSION='待科技平台确认'
export TECH_PLATFORM_API_SIGN_SECRET='待科技平台确认'
export TECH_PLATFORM_API_AES_KEY_BASE64='待科技平台确认'
export TECH_PLATFORM_API_CONNECT_TIMEOUT_MS=3000
export TECH_PLATFORM_API_READ_TIMEOUT_MS=5000

# 如需切到通联直连模式，再补齐以下配置后改为：
# export QW_MODE=ALLINPAY_DIRECT
# export QW_DIRECT_BASE_URL='待确认'
# export QW_DIRECT_PROCESS_PATH='/aipg/ProcessServlet'
# export QW_DIRECT_PKCS12_PATH='待提供'
# export QW_DIRECT_PKCS12_PASSWORD='待提供'
# export QW_DIRECT_VERIFY_CERT_PATH='待提供'
# export QW_DIRECT_MEMBER_SYNC_SERVICE_CODE='待提供'
# export QW_DIRECT_EXERCISE_URL_SERVICE_CODE='待提供'
# export QW_DIRECT_LENDING_NOTIFY_SERVICE_CODE='待提供'
