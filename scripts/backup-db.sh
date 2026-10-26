#!/usr/bin/env bash
# Backup CSDL — chạy trong cron trên VPS (Sprint 4), hoặc chạy tay khi cần
# crontab:  0 2 * * * /opt/ffa/scripts/backup-db.sh >> /var/log/ffa-backup.log 2>&1
set -euo pipefail
cd "$(dirname "$0")/.."
[ -f .env ] && set -a && . ./.env && set +a

DEST="${BACKUP_DIR:-./backups}"
STAMP="$(date +%Y%m%d-%H%M)"
KEEP="${BACKUP_KEEP:-7}"

mkdir -p "$DEST"
FILE="$DEST/${MYSQL_DATABASE:-ffa_db}-$STAMP.sql.gz"

docker compose --env-file .env exec -T mysql sh -c \
  "exec mysqldump -u root -p\"\$MYSQL_ROOT_PASSWORD\" --single-transaction --routines --triggers \"\$MYSQL_DATABASE\"" \
  | gzip > "$FILE"

echo "$(date -Iseconds) OK $FILE ($(du -h "$FILE" | cut -f1))"

# Giữ N bản gần nhất
ls -1t "$DEST"/*.sql.gz 2>/dev/null | tail -n +$((KEEP + 1)) | while read -r old; do
  echo "xóa bản cũ: $old"; rm -f "$old"
done
