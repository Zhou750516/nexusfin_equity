#!/bin/zsh

set -euo pipefail

RUNTIME_DIR="${TMPDIR:-/tmp}/nexusfin-h5-fullflow"

stop_tmux_service() {
  local name="$1"
  local session_name="nexusfin-${name}"

  if ! tmux has-session -t "$session_name" 2>/dev/null; then
    echo "[$name] not running in tmux"
    return
  fi

  tmux kill-session -t "$session_name"
  echo "[$name] stopped tmux session=$session_name"
}

stop_tmux_service "h5"
stop_tmux_service "backend"
stop_tmux_service "yunka-stub"
stop_tmux_service "tech-user-stub"

rm -f \
  "$RUNTIME_DIR/tech-user-stub.pid" \
  "$RUNTIME_DIR/yunka-stub.pid" \
  "$RUNTIME_DIR/backend.pid" \
  "$RUNTIME_DIR/h5.pid"
