#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
RUNTIME_DIR="${RUNTIME_DIR:-$ROOT_DIR/runtime/aliyun}"
LOG_FILE="${BACKEND_LOG_FILE:-$RUNTIME_DIR/backend.log}"
PID_FILE="${BACKEND_PID_FILE:-$RUNTIME_DIR/backend.pid}"
MVN_BIN="${MVN_BIN:-mvn}"
JAVA_BIN="${JAVA_BIN:-java}"
FORCE_REBUILD="${FORCE_REBUILD:-0}"

export SERVER_PORT="${SERVER_PORT:-8080}"
export YUNKA_MODE="${YUNKA_MODE:-REST}"
export YUNKA_ENABLED="${YUNKA_ENABLED:-true}"
export YUNKA_GATEWAY_PATH="${YUNKA_GATEWAY_PATH:-/api/gateway/proxy}"
export QW_MODE="${QW_MODE:-MOCK}"
export QW_ENABLED="${QW_ENABLED:-true}"
export AUTH_COOKIE_SECURE="${AUTH_COOKIE_SECURE:-true}"
# 仅用于 SSO / /api/users/me 这类 ABS 自身直连 auth 边界，不属于今天 Yunka 主链默认下游。
export TECH_PLATFORM_BASE_URL="${TECH_PLATFORM_BASE_URL:-http://127.0.0.1:18080}"
export TECH_PLATFORM_API_ENABLED="${TECH_PLATFORM_API_ENABLED:-false}"
export TECH_PLATFORM_API_MODE="${TECH_PLATFORM_API_MODE:-MOCK}"

require_command() {
  local command_name="$1"
  if ! command -v "$command_name" >/dev/null 2>&1; then
    echo "缺少命令: $command_name" >&2
    exit 1
  fi
}

require_env() {
  local variable_name="$1"
  if [[ -z "${!variable_name:-}" ]]; then
    echo "缺少环境变量: $variable_name" >&2
    exit 1
  fi
}

is_pid_running() {
  local pid="$1"
  kill -0 "$pid" >/dev/null 2>&1
}

resolve_backend_jar() {
  find "$ROOT_DIR/target" -maxdepth 1 -type f -name '*.jar' ! -name 'original-*' | sort | tail -n 1
}

require_command "$MVN_BIN"
require_command "$JAVA_BIN"

require_env MYSQL_HOST
require_env MYSQL_PORT
require_env MYSQL_DATABASE
require_env MYSQL_USERNAME
require_env MYSQL_PASSWORD
require_env AUTH_JWT_SECRET
require_env NEXUSFIN_APP_SECRET
require_env YUNKA_BASE_URL
require_env YUNKA_CHANNEL_CODE
require_env YUNKA_SIGNATURE

mkdir -p "$RUNTIME_DIR"

if [[ -f "$PID_FILE" ]]; then
  existing_pid="$(cat "$PID_FILE")"
  if [[ -n "$existing_pid" ]] && is_pid_running "$existing_pid"; then
    echo "backend 已在运行，pid=$existing_pid" >&2
    exit 1
  fi
  rm -f "$PID_FILE"
fi

if [[ "$FORCE_REBUILD" == "1" || "$FORCE_REBUILD" == "true" || -z "$(resolve_backend_jar 2>/dev/null || true)" ]]; then
  echo "[backend] 开始打包"
  (
    cd "$ROOT_DIR"
    "$MVN_BIN" -q -DskipTests package
  )
fi

BACKEND_JAR="${BACKEND_JAR:-$(resolve_backend_jar)}"

if [[ -z "$BACKEND_JAR" || ! -f "$BACKEND_JAR" ]]; then
  echo "未找到后端 jar，请先执行打包或检查 target 目录" >&2
  exit 1
fi

JAVA_OPTS="${JAVA_OPTS:--Xms512m -Xmx1024m}"

echo "[backend] 启动 jar=$BACKEND_JAR"
nohup "$JAVA_BIN" $JAVA_OPTS -jar "$BACKEND_JAR" >>"$LOG_FILE" 2>&1 &
backend_pid=$!
echo "$backend_pid" >"$PID_FILE"

sleep 3

if ! is_pid_running "$backend_pid"; then
  echo "backend 启动失败，请检查日志: $LOG_FILE" >&2
  exit 1
fi

cat <<EOF
[backend] started
pid=$backend_pid
log=$LOG_FILE
port=$SERVER_PORT
yunka_mode=$YUNKA_MODE
yunka_base_url=$YUNKA_BASE_URL
qw_mode=$QW_MODE
tech_platform_auth_base_url=$TECH_PLATFORM_BASE_URL
tech_platform_api_enabled=$TECH_PLATFORM_API_ENABLED
tech_platform_api_mode=$TECH_PLATFORM_API_MODE
EOF
