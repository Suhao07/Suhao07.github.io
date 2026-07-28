#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

if [[ ! -f .env.production ]]; then
  echo "缺少 .env.production。请先复制 .env.production.example 并填写真实域名与随机密码。" >&2
  exit 1
fi

if ! grep -Eq '^ADMIN_PASSWORD_HASH=\$2[aby]\$' .env.production; then
  echo "ADMIN_PASSWORD_HASH 未设置为 bcrypt 哈希，拒绝以默认后台密码上线。" >&2
  exit 1
fi

docker compose --env-file .env.production -f compose.prod.yaml config --quiet
docker compose --env-file .env.production -f compose.prod.yaml build
docker compose --env-file .env.production -f compose.prod.yaml up -d
docker compose --env-file .env.production -f compose.prod.yaml ps
