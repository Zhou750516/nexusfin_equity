#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
RUNTIME_DIR="${RUNTIME_DIR:-$ROOT_DIR/runtime/aliyun}"
BACKEND_LOG_FILE="${BACKEND_LOG_FILE:-$RUNTIME_DIR/backend.log}"
H5_LOG_FILE="${H5_LOG_FILE:-$RUNTIME_DIR/h5.log}"
NGINX_ERROR_LOG="${NGINX_ERROR_LOG:-/var/log/nginx/error.log}"
NGINX_ACCESS_LOG="${NGINX_ACCESS_LOG:-/var/log/nginx/access.log}"
TAIL_BIN="${TAIL_BIN:-tail}"
AWK_BIN="${AWK_BIN:-awk}"
TAIL_LINES="${TAIL_LINES:-200}"
MODE="${1:-core}"

usage() {
  cat <<'EOF'
用法:
  ./scripts/watch-logs.sh
  ./scripts/watch-logs.sh core
  ./scripts/watch-logs.sh all
  ./scripts/watch-logs.sh backend
  ./scripts/watch-logs.sh h5
  ./scripts/watch-logs.sh nginx
  ./scripts/watch-logs.sh help

模式:
  core     backend + h5；如果 nginx error 存在，也一起看
  all      backend + h5 + nginx error + nginx access
  backend  只看 backend
  h5       只看 h5
  nginx    只看 nginx error
  help     打印帮助

可覆盖环境变量:
  RUNTIME_DIR
  BACKEND_LOG_FILE
  H5_LOG_FILE
  NGINX_ERROR_LOG
  NGINX_ACCESS_LOG
  TAIL_LINES
EOF
}

require_command() {
  local command_name="$1"
  if ! command -v "$command_name" >/dev/null 2>&1; then
    echo "缺少命令: $command_name" >&2
    exit 1
  fi
}

append_log() {
  local label="$1"
  local path="$2"

  if [[ ! -e "$path" ]]; then
    missing_labels+=("$label")
    missing_paths+=("$path")
  elif [[ ! -f "$path" ]]; then
    invalid_labels+=("$label")
    invalid_paths+=("$path")
  elif [[ ! -r "$path" ]]; then
    unreadable_labels+=("$label")
    unreadable_paths+=("$path")
  else
    available_labels+=("$label")
    available_paths+=("$path")
  fi
}

start_tail() {
  local label="$1"
  local path="$2"

  echo "[watch-logs] start [$label] $path"
  (
    "$TAIL_BIN" -n "$TAIL_LINES" -F "$path" 2>&1 |
      "$AWK_BIN" -v label="$label" '{ print "[" label "] " $0; fflush() }'
  ) &
  tail_pids+=("$!")
}

print_skipped_logs() {
  if [[ "${#missing_paths[@]}" -gt 0 ]]; then
    echo "[watch-logs] 以下日志文件不存在，已跳过:" >&2
    for index in "${!missing_paths[@]}"; do
      echo "  - [${missing_labels[$index]}] ${missing_paths[$index]}" >&2
    done
    echo >&2
  fi

  if [[ "${#invalid_paths[@]}" -gt 0 ]]; then
    echo "[watch-logs] 以下路径不是普通日志文件，已跳过:" >&2
    for index in "${!invalid_paths[@]}"; do
      echo "  - [${invalid_labels[$index]}] ${invalid_paths[$index]}" >&2
    done
    echo >&2
  fi

  if [[ "${#unreadable_paths[@]}" -gt 0 ]]; then
    echo "[watch-logs] 以下日志文件当前不可读，已跳过:" >&2
    for index in "${!unreadable_paths[@]}"; do
      echo "  - [${unreadable_labels[$index]}] ${unreadable_paths[$index]}" >&2
    done
    echo >&2
  fi
}

cleanup() {
  if [[ "${#tail_pids[@]}" -eq 0 ]]; then
    return
  fi

  for pid in "${tail_pids[@]}"; do
    kill "$pid" >/dev/null 2>&1 || true
  done
}

require_command "$TAIL_BIN"
require_command "$AWK_BIN"

declare -a selected_labels=()
declare -a selected_paths=()
declare -a available_labels=()
declare -a available_paths=()
declare -a missing_labels=()
declare -a missing_paths=()
declare -a invalid_labels=()
declare -a invalid_paths=()
declare -a unreadable_labels=()
declare -a unreadable_paths=()
declare -a tail_pids=()

case "$MODE" in
  core)
    selected_labels=("backend" "h5" "nginx-error")
    selected_paths=("$BACKEND_LOG_FILE" "$H5_LOG_FILE" "$NGINX_ERROR_LOG")
    ;;
  all)
    selected_labels=("backend" "h5" "nginx-error" "nginx-access")
    selected_paths=("$BACKEND_LOG_FILE" "$H5_LOG_FILE" "$NGINX_ERROR_LOG" "$NGINX_ACCESS_LOG")
    ;;
  backend)
    selected_labels=("backend")
    selected_paths=("$BACKEND_LOG_FILE")
    ;;
  h5)
    selected_labels=("h5")
    selected_paths=("$H5_LOG_FILE")
    ;;
  nginx)
    selected_labels=("nginx-error")
    selected_paths=("$NGINX_ERROR_LOG")
    ;;
  -h|--help|help)
    usage
    exit 0
    ;;
  *)
    echo "不支持的模式: $MODE" >&2
    usage >&2
    exit 1
    ;;
esac

for index in "${!selected_labels[@]}"; do
  append_log "${selected_labels[$index]}" "${selected_paths[$index]}"
done

echo "[watch-logs] mode=$MODE"
echo "[watch-logs] root=$ROOT_DIR"
echo "[watch-logs] runtime=$RUNTIME_DIR"
echo

print_skipped_logs

if [[ "$MODE" == "core" ]]; then
  backend_available=false
  h5_available=false
  for label in "${available_labels[@]}"; do
    if [[ "$label" == "backend" ]]; then
      backend_available=true
    fi
    if [[ "$label" == "h5" ]]; then
      h5_available=true
    fi
  done

  if [[ "$backend_available" == "false" && "$h5_available" == "false" ]]; then
    echo "core 模式至少需要 backend 或 h5 日志之一，当前两者都不可用。" >&2
    exit 1
  fi
fi

if [[ "${#available_paths[@]}" -eq 0 ]]; then
  echo "没有可查看的日志文件，请先确认服务是否已启动，或检查日志路径配置。" >&2
  exit 1
fi

echo "[watch-logs] 将跟随以下日志:"
for index in "${!available_paths[@]}"; do
  echo "  - [${available_labels[$index]}] ${available_paths[$index]}"
done
echo

trap cleanup EXIT INT TERM

for index in "${!available_paths[@]}"; do
  start_tail "${available_labels[$index]}" "${available_paths[$index]}"
done

wait
