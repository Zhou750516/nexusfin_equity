#!/usr/bin/env bash

set -euo pipefail

NGINX_CONF="${NGINX_CONF:-/etc/nginx/nginx.conf}"
BACKUP_PATH="${NGINX_CONF}.bak.$(date +%Y%m%d%H%M%S)"

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  cat <<'EOF'
用途：
  备份并写入 www.aibosoftware.com 的 nginx 配置，然后执行 nginx -t 和 reload。

路由：
  http://www.aibosoftware.com/*          -> 301 到 HTTPS
  https://www.aibosoftware.com/          -> 官网静态首页 /usr/share/nginx/html
  https://www.aibosoftware.com/equity/   -> H5 127.0.0.1:3000
  https://www.aibosoftware.com/api/      -> 后端 127.0.0.1:8080

用法：
  ./scripts/aliyun/apply-nginx-aibosoftware.sh

可选环境变量：
  NGINX_CONF=/etc/nginx/nginx.conf
EOF
  exit 0
fi

require_command() {
  local command_name="$1"
  if ! command -v "$command_name" >/dev/null 2>&1; then
    echo "缺少命令: $command_name" >&2
    exit 1
  fi
}

require_command sudo
require_command tee
require_command nginx
require_command systemctl

echo "[1/4] 备份当前 nginx 配置: $NGINX_CONF -> $BACKUP_PATH"
sudo cp "$NGINX_CONF" "$BACKUP_PATH"

echo "[2/4] 写入 nginx 配置: $NGINX_CONF"
sudo tee "$NGINX_CONF" >/dev/null <<'EOF'
# For more information on configuration, see:
#   * Official English Documentation: http://nginx.org/en/docs/
#   * Official Russian Documentation: http://nginx.org/ru/docs/

user nginx;
worker_processes auto;
error_log /var/log/nginx/error.log;
pid /run/nginx.pid;

include /usr/share/nginx/modules/*.conf;

events {
    worker_connections 1024;
}

http {
    log_format  main  '$remote_addr - $remote_user [$time_local] "$request" '
                      '$status $body_bytes_sent "$http_referer" '
                      '"$http_user_agent" "$http_x_forwarded_for"';

    access_log  /var/log/nginx/access.log  main;

    sendfile            on;
    tcp_nopush          on;
    tcp_nodelay         on;
    keepalive_timeout   65;
    types_hash_max_size 4096;

    include             /etc/nginx/mime.types;
    default_type        application/octet-stream;

    include /etc/nginx/conf.d/*.conf;

    server {
        listen 80;
        listen [::]:80;
        server_name www.aibosoftware.com;

        return 301 https://$host$request_uri;
    }

    server {
        listen 443 ssl http2;
        listen [::]:443 ssl http2;
        server_name www.aibosoftware.com;
        root /usr/share/nginx/html;

        ssl_certificate     /etc/nginx/cert/www.aibosoftware.com.pem;
        ssl_certificate_key /etc/nginx/cert/www.aibosoftware.com.key;

        ssl_session_timeout 5m;
        ssl_ciphers ECDHE-RSA-AES128-GCM-SHA256:ECDHE:ECDH:AES:HIGH:!NULL:!aNULL:!MD5:!ADH:!RC4;
        ssl_protocols TLSv1.1 TLSv1.2 TLSv1.3;
        ssl_prefer_server_ciphers on;

        client_max_body_size 20m;

        # 后端 API
        location /api/ {
            proxy_pass http://127.0.0.1:8080;
            proxy_http_version 1.1;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto https;
            proxy_set_header X-Forwarded-Host $host;
            proxy_set_header X-Forwarded-Port 443;
            proxy_read_timeout 60s;
            proxy_connect_timeout 10s;
            proxy_send_timeout 60s;
            proxy_redirect off;
        }

        # /equity 自动补斜杠
        location = /equity {
            return 301 /equity/;
        }

        # 艾博生 H5
        location /equity/ {
            proxy_pass http://127.0.0.1:3000/;
            proxy_http_version 1.1;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto https;
            proxy_set_header X-Forwarded-Host $host;
            proxy_set_header X-Forwarded-Port 443;
            proxy_read_timeout 60s;
            proxy_connect_timeout 10s;
            proxy_send_timeout 60s;
            proxy_redirect off;
        }

        # 官网首页继续保留
        location / {
            root /usr/share/nginx/html;
            index index.html index.htm;
        }
    }
}
EOF

echo "[3/4] 检查 nginx 配置"
if ! sudo nginx -t; then
  echo "nginx 配置检查失败，回滚到备份: $BACKUP_PATH" >&2
  sudo cp "$BACKUP_PATH" "$NGINX_CONF"
  sudo nginx -t || true
  exit 1
fi

echo "[4/4] reload nginx"
sudo systemctl reload nginx

cat <<EOF

nginx 配置已更新并 reload。

备份文件：
  $BACKUP_PATH

建议验证：
  curl -I http://www.aibosoftware.com/
  curl -I https://www.aibosoftware.com/
  curl -I https://www.aibosoftware.com/equity/
  curl -I https://www.aibosoftware.com/api/equity/health

EOF
