#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
RUNTIME_DIR="${RUNTIME_DIR:-$ROOT_DIR/runtime/aliyun}"
NGINX_ERROR_LOG="${NGINX_ERROR_LOG:-/var/log/nginx/error.log}"
NGINX_ACCESS_LOG="${NGINX_ACCESS_LOG:-/var/log/nginx/access.log}"
H5_LOG_FILE="${H5_LOG_FILE:-$RUNTIME_DIR/h5.log}"
BACKEND_LOG_FILE="${BACKEND_LOG_FILE:-$RUNTIME_DIR/backend.log}"
TAIL_BIN="${TAIL_BIN:-tail}"
TAIL_LINES="${TAIL_LINES:-200}"
MODE="${1:-core}"

usage() {
  cat <<'EOF'
用法:
  ./scripts/aliyun/watch-logs.sh
  ./scripts/aliyun/watch-logs.sh core
  ./scripts/aliyun/watch-logs.sh all
  ./scripts/aliyun/watch-logs.sh backend
  ./scripts/aliyun/watch-logs.sh h5
  ./scripts/aliyun/watch-logs.sh nginx

模式:
  core     nginx error + h5 + backend
  all      nginx error + nginx access + h5 + backend
  backend  只看 backend
  h5       只看 h5
  nginx    只看 nginx error
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

  if [[ -f "$path" ]]; then
    available_labels+=("$label")
    available_paths+=("$path")
  else
    missing_labels+=("$label")
    missing_paths+=("$path")
  fi
}

require_command "$TAIL_BIN"

declare -a selected_labels=()
declare -a selected_paths=()
declare -a available_labels=()
declare -a available_paths=()
declare -a missing_labels=()
declare -a missing_paths=()

case "$MODE" in
  core)
    selected_labels=("nginx-error" "h5" "backend")
    selected_paths=("$NGINX_ERROR_LOG" "$H5_LOG_FILE" "$BACKEND_LOG_FILE")
    ;;
  all)
    selected_labels=("nginx-error" "nginx-access" "h5" "backend")
    selected_paths=("$NGINX_ERROR_LOG" "$NGINX_ACCESS_LOG" "$H5_LOG_FILE" "$BACKEND_LOG_FILE")
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

if [[ "${#available_paths[@]}" -gt 0 ]]; then
  echo "[watch-logs] 将跟随以下日志:"
  for index in "${!available_paths[@]}"; do
    echo "  - ${available_labels[$index]}: ${available_paths[$index]}"
  done
  echo
fi

if [[ "${#missing_paths[@]}" -gt 0 ]]; then
  echo "[watch-logs] 以下日志文件不存在，已跳过:" >&2
  for index in "${!missing_paths[@]}"; do
    echo "  - ${missing_labels[$index]}: ${missing_paths[$index]}" >&2
  done
  echo >&2
fi

if [[ "${#available_paths[@]}" -eq 0 ]]; then
  echo "没有可查看的日志文件，请先确认服务是否已启动，或检查日志路径配置。" >&2
  exit 1
fi

exec "$TAIL_BIN" -n "$TAIL_LINES" -F -v "${available_paths[@]}"
