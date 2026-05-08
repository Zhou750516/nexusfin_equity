#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"

"$ROOT_DIR/scripts/aliyun/stop-h5.sh" || true
"$ROOT_DIR/scripts/aliyun/stop-backend.sh" || true
"$ROOT_DIR/scripts/aliyun/stop-tech-user-stub.sh" || true
