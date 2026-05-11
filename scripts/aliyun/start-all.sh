#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
START_TECH_USER_STUB="${START_TECH_USER_STUB:-0}"

if [[ "$START_TECH_USER_STUB" == "1" || "$START_TECH_USER_STUB" == "true" ]]; then
  "$ROOT_DIR/scripts/aliyun/start-tech-user-stub.sh"
else
  echo "[start-all] skip tech-user-stub (set START_TECH_USER_STUB=1 to enable auth boundary stub)"
fi

"$ROOT_DIR/scripts/aliyun/start-backend.sh"
"$ROOT_DIR/scripts/aliyun/start-h5.sh"
