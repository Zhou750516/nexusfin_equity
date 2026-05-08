#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
RUNTIME_DIR="${RUNTIME_DIR:-$ROOT_DIR/runtime/aliyun}"
PID_FILE="${H5_PID_FILE:-$RUNTIME_DIR/h5.pid}"

if [[ ! -f "$PID_FILE" ]]; then
  echo "h5 未运行"
  exit 0
fi

pid="$(cat "$PID_FILE")"

if [[ -n "$pid" ]] && kill -0 "$pid" >/dev/null 2>&1; then
  kill "$pid"
  echo "h5 已停止，pid=$pid"
else
  echo "h5 pid 文件存在，但进程已不在运行"
fi

rm -f "$PID_FILE"
