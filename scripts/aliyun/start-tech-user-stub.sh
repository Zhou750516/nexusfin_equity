#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
RUNTIME_DIR="${RUNTIME_DIR:-$ROOT_DIR/runtime/aliyun}"
LOG_FILE="${TECH_USER_LOG_FILE:-$RUNTIME_DIR/tech-user-stub.log}"
PID_FILE="${TECH_USER_PID_FILE:-$RUNTIME_DIR/tech-user-stub.pid}"
NODE_BIN="${NODE_BIN:-node}"

export HOST="${HOST:-127.0.0.1}"
export PORT="${PORT:-18080}"
export MOCK_TECH_TOKEN="${MOCK_TECH_TOKEN:-mock-tech-token}"

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

require_command "$NODE_BIN"

mkdir -p "$RUNTIME_DIR"

if [[ -f "$PID_FILE" ]]; then
  existing_pid="$(cat "$PID_FILE")"
  if [[ -n "$existing_pid" ]] && is_pid_running "$existing_pid"; then
    echo "tech-user-stub 已在运行，pid=$existing_pid" >&2
    exit 1
  fi
  rm -f "$PID_FILE"
fi

nohup "$NODE_BIN" "$ROOT_DIR/scripts/local-stubs/tech-user-stub.js" >>"$LOG_FILE" 2>&1 &
stub_pid=$!
echo "$stub_pid" >"$PID_FILE"

sleep 2

if ! is_pid_running "$stub_pid"; then
  echo "tech-user-stub 启动失败，请检查日志: $LOG_FILE" >&2
  exit 1
fi

cat <<EOF
[tech-user-stub] started
pid=$stub_pid
log=$LOG_FILE
url=http://$HOST:$PORT/api/users/me
default_bearer_token=$MOCK_TECH_TOKEN
EOF
