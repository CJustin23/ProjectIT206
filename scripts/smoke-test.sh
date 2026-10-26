#!/usr/bin/env bash
# Smoke test Sprint 1 — chạy sau khi dev-up.sh
set -euo pipefail
cd "$(dirname "$0")/.."
[ -f .env ] && set -a && . ./.env && set +a

APP_PORT="${APP_PORT:-8090}"
WEB_PORT="${WEB_PORT:-5173}"

echo "== 1. Container ở trạng thái running/healthy? =="
docker compose --env-file .env ps

echo
echo "== 2. API trả về JSON health? =="
curl -fsS "http://localhost:${APP_PORT}/api/health" | tee /tmp/ffa-health.json
echo

echo "== 3. Flyway đã tạo bao nhiêu bảng? (kỳ vọng >= 14) =="
python3 -c "import json;print(json.load(open('/tmp/ffa-health.json'))['db']['tables'])" 2>/dev/null \
  || grep -o '"tables":[0-9]*' /tmp/ffa-health.json

echo
echo "== 4. Frontend dựng được? =="
curl -fsS -o /dev/null -w 'HTTP %{http_code}\n' "http://localhost:${WEB_PORT}/"

echo
echo "== 5. Bảng đã được seed? =="
docker compose --env-file .env exec -T mysql sh -c \
  "mysql -u\"\$MYSQL_USER\" -p\"\$MYSQL_PASSWORD\" \"\$MYSQL_DATABASE\" -e 'select count(*) as transactions from transactions; select name,kind from categories limit 5;'"

echo
echo "OK — Sprint 1 môi trường đạt."
