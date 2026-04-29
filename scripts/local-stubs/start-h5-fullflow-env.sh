#!/bin/zsh

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
RUNTIME_DIR="${TMPDIR:-/tmp}/nexusfin-h5-fullflow"
mkdir -p "$RUNTIME_DIR"

start_tmux_service() {
  local name="$1"
  local workdir="$2"
  local command="$3"
  local session_name="nexusfin-${name}"
  local log_file="$RUNTIME_DIR/${name}.log"

  if tmux has-session -t "$session_name" 2>/dev/null; then
    echo "[$name] already running in tmux session=$session_name"
    return
  fi

  : >"$log_file"
  tmux new-session -d -s "$session_name" \
    "/bin/zsh -lc 'cd \"$workdir\" && exec $command >>\"$log_file\" 2>&1'"

  sleep 2

  if ! tmux has-session -t "$session_name" 2>/dev/null; then
    echo "[$name] failed to stay running in tmux, inspect $log_file" >&2
    exit 1
  fi
  echo "[$name] started in tmux session=$session_name log=$log_file"
}

start_tmux_service \
  "tech-user-stub" \
  "$ROOT_DIR" \
  "node scripts/local-stubs/tech-user-stub.js"

start_tmux_service \
  "yunka-stub" \
  "$ROOT_DIR" \
  "node scripts/local-stubs/yunka-stub.js"

start_tmux_service \
  "backend" \
  "$ROOT_DIR" \
  "mvn -Dspring-boot.run.arguments=--spring.datasource.username=root\\ --spring.datasource.password=\\ --nexusfin.auth.jwt.cookie-secure=false\\ --nexusfin.third-party.yunka.mode=REST\\ --nexusfin.third-party.yunka.base-url=http://127.0.0.1:18081\\ --nexusfin.auth.tech-platform-base-url=http://127.0.0.1:18080 spring-boot:run"

start_tmux_service \
  "h5" \
  "$ROOT_DIR/H5" \
  "pnpm dev"

cat <<EOF
runtime_dir=$RUNTIME_DIR
tech_user_stub_log=$RUNTIME_DIR/tech-user-stub.log
yunka_stub_log=$RUNTIME_DIR/yunka-stub.log
backend_log=$RUNTIME_DIR/backend.log
h5_log=$RUNTIME_DIR/h5.log
tmux_sessions=
  nexusfin-tech-user-stub
  nexusfin-yunka-stub
  nexusfin-backend
  nexusfin-h5
EOF
