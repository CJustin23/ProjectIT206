#!/usr/bin/env bash
# Dừng môi trường dev. Mặc định GIỮ lại dữ liệu MySQL trong volume.
# Cẩn thận: --purge sẽ XÓA toàn bộ dữ liệu (docker compose down -v).
set -euo pipefail
cd "$(dirname "$0")/.."

if [ "${1:-}" = "--purge" ]; then
  echo "Cảnh báo: xóa cả volume mysql_data. Ctrl+C để hủy, đang chạy sau 3s..."
  sleep 3
  docker compose --env-file .env down -v
else
  docker compose --env-file .env down
fi
