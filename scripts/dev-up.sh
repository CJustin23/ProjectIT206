#!/usr/bin/env bash
# Khởi động môi trường dev: MySQL + Spring Boot API + Vite + Adminer
set -euo pipefail
cd "$(dirname "$0")/.."

[ -f .env ] || cp .env.example .env

docker compose --env-file .env up -d --build
echo
docker compose --env-file .env ps
echo
echo "API health : http://localhost:${APP_PORT:-8090}/api/health"
echo "Frontend   : http://localhost:${WEB_PORT:-5173}"
echo "Adminer    : http://localhost:${ADMINER_PORT:-8081}  (server=mysql, user=${MYSQL_USER}, pass xem .env)"
