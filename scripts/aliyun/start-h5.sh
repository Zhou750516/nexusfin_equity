#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
H5_DIR="$ROOT_DIR/H5"
RUNTIME_DIR="${RUNTIME_DIR:-$ROOT_DIR/runtime/aliyun}"
LOG_FILE="${H5_LOG_FILE:-$RUNTIME_DIR/h5.log}"
PID_FILE="${H5_PID_FILE:-$RUNTIME_DIR/h5.pid}"
PNPM_BIN="${PNPM_BIN:-pnpm}"
NODE_BIN="${NODE_BIN:-node}"
H5_PORT="${H5_PORT:-3000}"
FORCE_INSTALL="${FORCE_INSTALL:-0}"
FORCE_REBUILD="${FORCE_REBUILD:-0}"

require_command() {
  local command_name="$1"
  if ! command -v "$command_name" >/dev/null 2>&1; then
    echo "缺少命令: $command_name" >&2
    exit 1
  fi
}

is_pid_running() {
  local pid="$1"
  kill -0 "$pid" >/dev/null 2>&1
}

require_command "$PNPM_BIN"
require_command "$NODE_BIN"

mkdir -p "$RUNTIME_DIR"

if [[ -f "$PID_FILE" ]]; then
  existing_pid="$(cat "$PID_FILE")"
  if [[ -n "$existing_pid" ]] && is_pid_running "$existing_pid"; then
    echo "h5 已在运行，pid=$existing_pid" >&2
    exit 1
  fi
  rm -f "$PID_FILE"
fi

if [[ "$FORCE_INSTALL" == "1" || "$FORCE_INSTALL" == "true" || ! -d "$H5_DIR/node_modules" ]]; then
  echo "[h5] 安装依赖"
  (
    cd "$H5_DIR"
    "$PNPM_BIN" install
  )
fi

if [[ "$FORCE_REBUILD" == "1" || "$FORCE_REBUILD" == "true" || ! -f "$H5_DIR/dist/index.js" ]]; then
  echo "[h5] 构建生产包"
  (
    cd "$H5_DIR"
    "$PNPM_BIN" build
  )
fi

echo "[h5] 启动生产服务"
(
  cd "$H5_DIR"
  PORT="$H5_PORT" NODE_ENV=production nohup "$PNPM_BIN" start >>"$LOG_FILE" 2>&1 &
  h5_pid=$!
  echo "$h5_pid" >"$PID_FILE"
)

sleep 3

h5_pid="$(cat "$PID_FILE")"
if ! is_pid_running "$h5_pid"; then
  echo "h5 启动失败，请检查日志: $LOG_FILE" >&2
  exit 1
fi

cat <<EOF
[h5] started
pid=$h5_pid
log=$LOG_FILE
port=$H5_PORT
EOF
