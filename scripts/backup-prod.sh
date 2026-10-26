#!/usr/bin/env bash
# Backup CSDL production: mysqldump trong container ffa-db, nen gzip, giu 14 ban.
# Cron tren VPS:  0 2 * * * /opt/ffa/scripts/backup-prod.sh >> /var/log/ffa-backup.log 2>&1
set -euo pipefail

APP_DIR="${APP_DIR:-/opt/ffa}"
BACKUP_DIR="${BACKUP_DIR:-$APP_DIR/backups}"
KEEP_DAYS=14

cd "$APP_DIR"
if [ ! -f .env.prod ]; then
  echo "[backup] thieu .env.prod trong $APP_DIR" >&2
  exit 1
fi
# Nạp MYSQL_* từ file env để đọc cấu hình
set -a
. .env.prod
set +a

mkdir -p "$BACKUP_DIR"
STAMP="$(date +%Y%m%d-%H%M%S)"
OUT="$BACKUP_DIR/ffa-$STAMP.sql.gz"

docker compose -f docker-compose.prod.yml --env-file .env.prod exec -T db \
  sh -c "exec mysqldump -u\"\$MYSQL_USER\" -p\"\$MYSQL_PASSWORD\" --single-transaction \
         --routines --set-gtid-purged=OFF \"\$MYSQL_DATABASE\"" \
  | gzip > "$OUT"

SIZE=$(du -h "$OUT" | cut -f1)
echo "[backup] $OUT ($SIZE)"

find "$BACKUP_DIR" -name 'ffa-*.sql.gz' -mtime +"$KEEP_DAYS" -delete

# Kiem tra backup khong rong: file gzip nho hon 1KB la dau hieu dump that bai
if [ "$(stat -c%s "$OUT")" -lt 1024 ]; then
  echo "[backup] CANH BAO: file qua nho, kiem lai mysqldump" >&2
  exit 1
fi
