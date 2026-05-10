#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
SCRIPT_PATH="$ROOT_DIR/scripts/watch-logs.sh"
TEST_TMP_ROOT="$(mktemp -d "${TMPDIR:-/tmp}/watch-logs-test.XXXXXX")"

cleanup() {
  rm -rf "$TEST_TMP_ROOT"
}

assert_contains() {
  local haystack="$1"
  local needle="$2"
  if [[ "$haystack" != *"$needle"* ]]; then
    echo "断言失败：输出未包含 [$needle]" >&2
    echo "实际输出：" >&2
    echo "$haystack" >&2
    exit 1
  fi
}

run_and_capture() {
  local output_file="$1"
  shift

  set +e
  "$@" >"$output_file" 2>&1
  local status=$?
  set -e

  printf '%s' "$status"
}

test_core_without_logs_exits_cleanly() {
  local runtime_dir="$TEST_TMP_ROOT/empty-runtime"
  mkdir -p "$runtime_dir"
  local output_file="$TEST_TMP_ROOT/core-empty.out"

  local status
  status="$(run_and_capture "$output_file" env \
    RUNTIME_DIR="$runtime_dir" \
    NGINX_ERROR_LOG="$runtime_dir/nginx-error.log" \
    NGINX_ACCESS_LOG="$runtime_dir/nginx-access.log" \
    bash "$SCRIPT_PATH" core)"

  local output
  output="$(cat "$output_file")"

  if [[ "$status" -eq 0 ]]; then
    echo "断言失败：空日志场景不应返回 0" >&2
    exit 1
  fi

  assert_contains "$output" "core 模式至少需要 backend 或 h5 日志之一"
  assert_contains "$output" "没有可查看的日志文件"
  if [[ "$output" == *"unbound variable"* ]]; then
    echo "断言失败：空日志场景仍然出现 unbound variable" >&2
    echo "$output" >&2
    exit 1
  fi
}

test_auto_detects_local_tmp_runtime() {
  local tmp_runtime="$TEST_TMP_ROOT/tmp-root/nexusfin-h5-fullflow"
  mkdir -p "$tmp_runtime"
  printf 'backend sample\n' >"$tmp_runtime/backend.log"
  printf 'h5 sample\n' >"$tmp_runtime/h5.log"
  local output_file="$TEST_TMP_ROOT/core-detect.out"
  local status

  (
    env \
      TMPDIR="$TEST_TMP_ROOT/tmp-root/" \
      NGINX_ERROR_LOG="$TEST_TMP_ROOT/tmp-root/nginx-error.log" \
      NGINX_ACCESS_LOG="$TEST_TMP_ROOT/tmp-root/nginx-access.log" \
      TAIL_LINES=1 \
      bash "$SCRIPT_PATH" core >"$output_file" 2>&1
  ) &
  local pid=$!
  sleep 1
  kill "$pid" >/dev/null 2>&1 || true

  set +e
  wait "$pid"
  status=$?
  set -e

  local output
  output="$(cat "$output_file")"

  if [[ "$status" -ne 0 && "$status" -ne 143 ]]; then
    echo "断言失败：本地自动探测执行异常，status=$status" >&2
    echo "$output" >&2
    exit 1
  fi

  assert_contains "$output" "runtime=$tmp_runtime"
  assert_contains "$output" "[backend] backend sample"
  assert_contains "$output" "[h5] h5 sample"
}

trap cleanup EXIT

test_core_without_logs_exits_cleanly
test_auto_detects_local_tmp_runtime

echo "watch-logs tests passed"
