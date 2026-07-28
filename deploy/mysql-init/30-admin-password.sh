#!/usr/bin/env bash
set -euo pipefail

if [[ ! "${ADMIN_PASSWORD_HASH:-}" =~ ^\$2[aby]\$ ]]; then
  echo "ADMIN_PASSWORD_HASH 必须是有效的 bcrypt 哈希。" >&2
  exit 1
fi

escaped_hash="${ADMIN_PASSWORD_HASH//\\/\\\\}"
escaped_hash="${escaped_hash//\'/\\\'}"

mysql --protocol=socket \
  --user=root \
  --password="${MYSQL_ROOT_PASSWORD}" \
  "${MYSQL_DATABASE}" <<SQL
UPDATE tb_user_auth
SET password = '${escaped_hash}'
WHERE username = 'admin@localhost';
SQL

