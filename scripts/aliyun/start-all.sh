#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"

"$ROOT_DIR/scripts/aliyun/start-tech-user-stub.sh"
"$ROOT_DIR/scripts/aliyun/start-backend.sh"
"$ROOT_DIR/scripts/aliyun/start-h5.sh"
