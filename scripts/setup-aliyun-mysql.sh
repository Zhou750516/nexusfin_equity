#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
SCHEMA_FILE_DEFAULT="$ROOT_DIR/src/main/resources/db/schema.sql"

usage() {
  cat <<'EOF'
用途：
  创建 work 账号、设置密码、创建 nexusfin_equity 数据库，并导入 schema.sql。

默认值：
  MYSQL_ADMIN_HOST=127.0.0.1
  MYSQL_ADMIN_PORT=3306
  MYSQL_ADMIN_USER=root
  WORK_DB_NAME=nexusfin_equity
  WORK_DB_USER=work
  WORK_DB_ACCOUNT_HOSTS=localhost,127.0.0.1
  SCHEMA_FILE=src/main/resources/db/schema.sql

可用环境变量：
  MYSQL_ADMIN_HOST
  MYSQL_ADMIN_PORT
  MYSQL_ADMIN_USER
  MYSQL_ADMIN_PASSWORD
  WORK_DB_NAME
  WORK_DB_USER
  WORK_DB_PASSWORD
  WORK_DB_ACCOUNT_HOSTS
  SCHEMA_FILE

示例：
  export MYSQL_ADMIN_PASSWORD='root-password'
  export WORK_DB_PASSWORD='work-password'
  ./scripts/setup-aliyun-mysql.sh
EOF
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  usage
  exit 0
fi

MYSQL_ADMIN_HOST="${MYSQL_ADMIN_HOST:-127.0.0.1}"
MYSQL_ADMIN_PORT="${MYSQL_ADMIN_PORT:-3306}"
MYSQL_ADMIN_USER="${MYSQL_ADMIN_USER:-root}"
MYSQL_ADMIN_PASSWORD="${MYSQL_ADMIN_PASSWORD:-}"

WORK_DB_NAME="${WORK_DB_NAME:-nexusfin_equity}"
WORK_DB_USER="${WORK_DB_USER:-work}"
WORK_DB_PASSWORD="${WORK_DB_PASSWORD:-}"
WORK_DB_ACCOUNT_HOSTS="${WORK_DB_ACCOUNT_HOSTS:-localhost,127.0.0.1}"
SCHEMA_FILE="${SCHEMA_FILE:-$SCHEMA_FILE_DEFAULT}"

require_command() {
  local command_name="$1"
  if ! command -v "$command_name" >/dev/null 2>&1; then
    echo "缺少命令: $command_name" >&2
    exit 1
  fi
}

prompt_secret_if_empty() {
  local variable_name="$1"
  local prompt_text="$2"
  local current_value="${!variable_name}"

  if [[ -n "$current_value" ]]; then
    return
  fi

  read -r -s -p "$prompt_text: " current_value
  echo

  if [[ -z "$current_value" ]]; then
    echo "$prompt_text 不能为空" >&2
    exit 1
  fi

  printf -v "$variable_name" '%s' "$current_value"
}

validate_identifier() {
  local value="$1"
  local field_name="$2"

  if [[ ! "$value" =~ ^[A-Za-z0-9_]+$ ]]; then
    echo "$field_name 只允许字母、数字、下划线: $value" >&2
    exit 1
  fi
}

validate_port() {
  local value="$1"

  if [[ ! "$value" =~ ^[0-9]+$ ]]; then
    echo "MYSQL_ADMIN_PORT 必须是数字: $value" >&2
    exit 1
  fi
}

sql_escape_literal() {
  local value="$1"
  value="${value//\\/\\\\}"
  value="${value//\'/\'\'}"
  printf "%s" "$value"
}

validate_host_token() {
  local value="$1"

  if [[ ! "$value" =~ ^[%A-Za-z0-9._-]+$ ]]; then
    echo "WORK_DB_ACCOUNT_HOSTS 中存在非法 host: $value" >&2
    exit 1
  fi
}

require_command mysql

validate_identifier "$WORK_DB_NAME" "WORK_DB_NAME"
validate_identifier "$WORK_DB_USER" "WORK_DB_USER"
validate_port "$MYSQL_ADMIN_PORT"

if [[ ! -f "$SCHEMA_FILE" ]]; then
  echo "schema 文件不存在: $SCHEMA_FILE" >&2
  exit 1
fi

prompt_secret_if_empty MYSQL_ADMIN_PASSWORD "请输入 MySQL 管理员密码"
prompt_secret_if_empty WORK_DB_PASSWORD "请输入 work 账号密码"

IFS=',' read -r -a account_hosts <<<"$WORK_DB_ACCOUNT_HOSTS"

if [[ "${#account_hosts[@]}" -eq 0 ]]; then
  echo "WORK_DB_ACCOUNT_HOSTS 不能为空" >&2
  exit 1
fi

for host in "${account_hosts[@]}"; do
  if [[ -z "$host" ]]; then
    echo "WORK_DB_ACCOUNT_HOSTS 中存在空 host" >&2
    exit 1
  fi
  validate_host_token "$host"
done

run_mysql_admin_sql() {
  local sql_text="$1"
  MYSQL_PWD="$MYSQL_ADMIN_PASSWORD" mysql \
    --protocol=TCP \
    -h "$MYSQL_ADMIN_HOST" \
    -P "$MYSQL_ADMIN_PORT" \
    -u "$MYSQL_ADMIN_USER" \
    --default-character-set=utf8mb4 \
    -e "$sql_text"
}

run_mysql_import() {
  MYSQL_PWD="$MYSQL_ADMIN_PASSWORD" mysql \
    --protocol=TCP \
    -h "$MYSQL_ADMIN_HOST" \
    -P "$MYSQL_ADMIN_PORT" \
    -u "$MYSQL_ADMIN_USER" \
    --default-character-set=utf8mb4 \
    "$WORK_DB_NAME" <"$SCHEMA_FILE"
}

escaped_db_name="$(sql_escape_literal "$WORK_DB_NAME")"
escaped_user="$(sql_escape_literal "$WORK_DB_USER")"
escaped_work_password="$(sql_escape_literal "$WORK_DB_PASSWORD")"

echo "[1/4] 创建数据库 $WORK_DB_NAME"
run_mysql_admin_sql "CREATE DATABASE IF NOT EXISTS \`$escaped_db_name\` DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_unicode_ci;"

echo "[2/4] 创建或重置账号 $WORK_DB_USER"
for host in "${account_hosts[@]}"; do
  escaped_host="$(sql_escape_literal "$host")"
  run_mysql_admin_sql "CREATE USER IF NOT EXISTS '$escaped_user'@'$escaped_host' IDENTIFIED BY '$escaped_work_password';"
  run_mysql_admin_sql "ALTER USER '$escaped_user'@'$escaped_host' IDENTIFIED BY '$escaped_work_password';"
  run_mysql_admin_sql "GRANT ALL PRIVILEGES ON \`$escaped_db_name\`.* TO '$escaped_user'@'$escaped_host';"
done
run_mysql_admin_sql "FLUSH PRIVILEGES;"

echo "[3/4] 导入 schema: $SCHEMA_FILE"
run_mysql_import

echo "[4/4] 完成，当前账号授权如下"
for host in "${account_hosts[@]}"; do
  escaped_host="$(sql_escape_literal "$host")"
  run_mysql_admin_sql "SHOW GRANTS FOR '$escaped_user'@'$escaped_host';"
done

cat <<EOF

初始化完成。

建议后端联调配置：
  MYSQL_HOST=$MYSQL_ADMIN_HOST
  MYSQL_PORT=$MYSQL_ADMIN_PORT
  MYSQL_DATABASE=$WORK_DB_NAME
  MYSQL_USERNAME=$WORK_DB_USER
  MYSQL_PASSWORD=<你刚才设置的密码>

EOF
